package com.lensora.ai.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lensora.ai.ui.studio.ImageEditor
import com.lensora.ai.ui.vibes.VibePreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

class AiProcessorWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val tag = "AiProcessorWorker"
    private val validator = Validator()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val photoUriStr = inputData.getString("PHOTO_URI")
            ?: inputData.getString("input_uri")
            ?: return@withContext Result.failure()
        val sceneTypeStr = inputData.getString("SCENE_TYPE")
            ?: inputData.getString("scene_type")
            ?: "AUTO"
        val evOffset = inputData.getFloat("EV_OFFSET", inputData.getFloat("ev_offset", 0.0f))
        val vibeStr = inputData.getString("VIBE_PRESET")
            ?: inputData.getString("vibe_preset")
            ?: "NONE"
        val vibePreset = try { VibePreset.valueOf(vibeStr) } catch (_: Exception) { VibePreset.NONE }
        val isHighRes = inputData.getBoolean("IS_HIGH_RES", inputData.getBoolean("is_high_res", false))

        Log.i(tag, "Starting L3 Tiled AI Processing pass for $photoUriStr [Scene: $sceneTypeStr, EV: $evOffset, Vibe: ${vibePreset.displayName}, HighRes: $isHighRes]")

        val startTime = System.currentTimeMillis()
        var processedBitmap: Bitmap? = null

        try {
            val inputUri = Uri.parse(photoUriStr)
            val resolver = context.contentResolver

            // 1. Memory Shield: Decode full bounds or stream input
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(inputUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val imgWidth = options.outWidth
            val imgHeight = options.outHeight

            if (imgWidth <= 0 || imgHeight <= 0) {
                Log.w(tag, "Invalid image dimensions for $photoUriStr")
                return@withContext Result.failure()
            }

            val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)
            if (vibePreset != VibePreset.NONE) {
                val vibeMatrix = ImageEditor.createVibeAndroidColorMatrix(vibePreset)
                colorMatrix.postConcat(vibeMatrix)
                Log.i(tag, "Applied Vibe Preset ColorMatrix pass: ${vibePreset.displayName}")
            }

            // 2. Check for Real-ESRGAN v2 TFLite Model availability & Contract verification for HIGH-RES mode
            var esrganInterpreterObj: EsrganInterpreter? = null
            var esrganValid = false

            if (isHighRes) {
                val modelManager = ModelManager(context)
                val esrganFile = modelManager.getEnhancementModelFile()

                if (esrganFile != null && ModelFileGuard.isUsable(esrganFile)) {
                    try {
                        val candidateObj = RealEsrganEngine.createInterpreter(esrganFile)
                        val provider = InterpreterTensorInfoProvider(candidateObj.interpreter)
                        val contractStatus = ModelContractChecker.checkContract(esrganFile, ModelContracts.REAL_ESRGAN_V2, provider)

                        if (contractStatus is ModelContractStatus.Valid) {
                            esrganInterpreterObj = candidateObj
                            esrganValid = true
                            Log.i(tag, "Real-ESRGAN v2 model validated and loaded: ${esrganFile.name} [Backend: ${if (candidateObj.usingGpu) "GPU" else "CPU"}]")
                        } else {
                            Log.w(tag, "Real-ESRGAN v2 model contract check failed: $contractStatus. Falling back to standard pipeline.")
                            candidateObj.close()
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to instantiate Real-ESRGAN Interpreter: ${e.localizedMessage}")
                    }
                }
            }

            // Decode source image for processing
            val fullSourceBitmap = resolver.openInputStream(inputUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

            if (fullSourceBitmap == null) {
                Log.w(tag, "Failed to decode bitmap from $photoUriStr")
                esrganInterpreterObj?.close()
                return@withContext Result.failure()
            }

            val activeEsrganObj = esrganInterpreterObj
            if (isHighRes && esrganValid && activeEsrganObj != null) {
                // 3. Attempt 50% pre-downscale pass (50% input -> 4x tile SR -> 2x net output scale)
                try {
                    Log.i(tag, "Executing Real-ESRGAN 2x net super-resolution pass (50% pre-downscale, 4x tile scale) [Backend: ${if (activeEsrganObj.usingGpu) "GPU" else "CPU"}]")
                    processedBitmap = RealEsrganEngine.runSuperResolutionPipeline(
                        sourceBitmap = fullSourceBitmap,
                        downscaleFactor = 0.5f,
                        interpreter = activeEsrganObj.interpreter,
                        validator = validator,
                        colorMatrix = colorMatrix,
                        onProgress = { percent ->
                            setProgressAsync(workDataOf("progress" to percent))
                        },
                    )
                } catch (oom: OutOfMemoryError) {
                    Log.w(tag, "OOM during 50% Real-ESRGAN pass. Retrying at 25% scale yielding 1x net resolution (no upscale fallback)", oom)
                    processedBitmap?.recycle()
                    processedBitmap = null

                    // Retry at 25% scale (25% input -> 4x tile SR -> 1x net resolution fallback)
                    try {
                        processedBitmap = RealEsrganEngine.runSuperResolutionPipeline(
                            sourceBitmap = fullSourceBitmap,
                            downscaleFactor = 0.25f,
                            interpreter = activeEsrganObj.interpreter,
                            validator = validator,
                            colorMatrix = colorMatrix,
                            onProgress = { percent ->
                                setProgressAsync(workDataOf("progress" to percent))
                            },
                        )
                    } catch (retryOom: OutOfMemoryError) {
                        Log.e(tag, "OOM during 25% fallback pass as well. Falling back to standard tiled pass.", retryOom)
                        processedBitmap?.recycle()
                        processedBitmap = null
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w(tag, "Exception during 25% Real-ESRGAN fallback pass. Falling back to standard tiled pass.", e)
                        processedBitmap?.recycle()
                        processedBitmap = null
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(tag, "Exception during 50% Real-ESRGAN pass. Falling back to standard tiled pass.", e)
                    processedBitmap?.recycle()
                    processedBitmap = null
                } finally {
                    activeEsrganObj.close()
                }
            }

            // Standard ColorMatrix / Vibe preset tiled fallback pass if Real-ESRGAN not used or failed
            if (processedBitmap == null) {
                processedBitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(processedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                    isFilterBitmap = true
                }

                val decoder = resolver.openInputStream(inputUri)?.use { inputStream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        BitmapRegionDecoder.newInstance(inputStream)
                    } else {
                        @Suppress("DEPRECATION")
                        BitmapRegionDecoder.newInstance(inputStream, false)
                    }
                }

                if (decoder != null) {
                    val tileSize = 512
                    val numCols = (imgWidth + tileSize - 1) / tileSize
                    val numRows = (imgHeight + tileSize - 1) / tileSize
                    val totalTiles = numCols * numRows

                    var tileIndex = 0
                    for (row in 0 until numRows) {
                        for (col in 0 until numCols) {
                            val left = col * tileSize
                            val top = row * tileSize
                            val right = (left + tileSize).coerceAtMost(imgWidth)
                            val bottom = (top + tileSize).coerceAtMost(imgHeight)
                            val rect = Rect(left, top, right, bottom)

                            val tileBitmap = decoder.decodeRegion(rect, null)
                            if (tileBitmap != null) {
                                canvas.drawBitmap(tileBitmap, left.toFloat(), top.toFloat(), paint)
                                tileBitmap.recycle()
                            }

                            tileIndex++
                            val progressPercent = (tileIndex * 100) / totalTiles
                            setProgressAsync(workDataOf("progress" to progressPercent))
                        }
                    }
                } else {
                    canvas.drawBitmap(fullSourceBitmap, 0f, 0f, paint)
                }
            }

            fullSourceBitmap.recycle()

            val finalProcessedBitmap = processedBitmap
            if (!validator.isValid(finalProcessedBitmap)) {
                Log.w(tag, "L4 Validator failed: Processed bitmap invalid or pitch black. Keeping raw photo $photoUriStr")
                return@withContext Result.failure()
            }

            // Save processed Bitmap to MediaStore
            val timestamp = System.currentTimeMillis()
            val fileName = "LENSORA_AI_PROCESSED_$timestamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Lensora_Ai")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val newUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )

            if (newUri != null) {
                resolver.openOutputStream(newUri)?.use { outStream ->
                    finalProcessedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)

                val executionTime = System.currentTimeMillis() - startTime
                Log.i(tag, "L3 Tiled AI Processing finished in ${executionTime}ms (Vibe: ${vibePreset.displayName}, Dimensions: ${finalProcessedBitmap.width}x${finalProcessedBitmap.height}). Saved $fileName to MediaStore at $newUri")

                // Enqueue CloudSyncWorker if Cloud Sync toggle is ON in Settings
                val prefs = context.getSharedPreferences("lensora_prefs", Context.MODE_PRIVATE)
                val isCloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", false)
                val isAutoPurgeEnabled = prefs.getBoolean("auto_purge_enabled", true)

                if (isCloudSyncEnabled) {
                    val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                        .setInputData(
                            workDataOf(
                                "PHOTO_URI" to newUri.toString(),
                                "FILE_NAME" to fileName,
                                "AUTO_PURGE_ENABLED" to isAutoPurgeEnabled,
                            ),
                        )
                        .build()

                    WorkManager.getInstance(context).enqueue(syncRequest)
                    Log.i(tag, "Cloud Sync toggle is ON. Enqueued CloudSyncWorker for $fileName (Auto-Purge: $isAutoPurgeEnabled)")
                }

                Result.success()
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for processed image")
                Result.failure()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(tag, "Error during AiProcessorWorker execution", e)
            Result.failure()
        } finally {
            processedBitmap?.recycle()
        }
    }

    private fun createColorMatrixForPlan(sceneType: String, evOffset: Float): ColorMatrix {
        val matrix = ColorMatrix()

        val evScale = 1.0f + (evOffset * 0.20f)
        val contrastScale = when (sceneType) {
            "PORTRAIT" -> 1.10f
            "LANDSCAPE" -> 1.15f
            "NIGHT" -> 1.25f
            "SKY" -> 1.10f
            else -> 1.05f
        }

        val combinedScale = contrastScale * evScale
        val translateOffset = 128f * (1f - contrastScale) + (evOffset * 15f)

        val floatArray = floatArrayOf(
            combinedScale, 0f, 0f, 0f, translateOffset,
            0f, combinedScale, 0f, 0f, translateOffset,
            0f, 0f, combinedScale, 0f, translateOffset,
            0f, 0f, 0f, 1f, 0f,
        )

        matrix.set(floatArray)
        return matrix
    }
}
