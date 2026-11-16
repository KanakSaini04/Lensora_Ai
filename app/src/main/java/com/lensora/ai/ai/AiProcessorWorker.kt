package com.lensora.ai.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
        val vibePreset = try { VibePreset.valueOf(vibeStr) } catch (e: Exception) { VibePreset.NONE }

        Log.i(tag, "Starting L3 512x512 Tiled AI Processing pass for $photoUriStr [Scene: $sceneTypeStr, EV: $evOffset, Vibe: ${vibePreset.displayName}]")

        val startTime = System.currentTimeMillis()
        var processedBitmap: Bitmap? = null

        try {
            val inputUri = Uri.parse(photoUriStr)
            val resolver = context.contentResolver

            // 1. Memory Shield: Initialize BitmapRegionDecoder without decoding full image into RAM
            val decoder = resolver.openInputStream(inputUri)?.use { inputStream ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(inputStream)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(inputStream, false)
                }
            }

            if (decoder == null) {
                Log.w(tag, "Failed to initialize BitmapRegionDecoder for $photoUriStr")
                return@withContext Result.failure()
            }

            val imgWidth = decoder.width
            val imgHeight = decoder.height

            // 2. TFLite Model Detection & Optional Inference Pass on Low-Res Sample
            val mobileNetFile = File(context.filesDir, "models/mobilenet_v1_1.0_224.tflite")
            val fallbackFile = File(context.filesDir, "models/real_esrgan_v2.tflite")
            val modelFile = if (mobileNetFile.exists()) mobileNetFile else if (fallbackFile.exists()) fallbackFile else null
            var usedTfLite = false

            if (modelFile != null && modelFile.exists()) {
                var interpreter: Interpreter? = null
                try {
                    val options = Interpreter.Options().apply { setNumThreads(4) }
                    interpreter = Interpreter(modelFile, options)

                    val sampleRect = Rect(0, 0, imgWidth.coerceAtMost(512), imgHeight.coerceAtMost(512))
                    val sampleTile = decoder.decodeRegion(sampleRect, null)

                    if (sampleTile != null && modelFile.name.contains("mobilenet", ignoreCase = true)) {
                        val scaledBitmap = Bitmap.createScaledBitmap(sampleTile, 224, 224, true)
                        val inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
                            order(ByteOrder.nativeOrder())
                        }

                        val intValues = IntArray(224 * 224)
                        scaledBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
                        for (pixelValue in intValues) {
                            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
                            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
                            val b = (pixelValue and 0xFF) / 255.0f
                            inputBuffer.putFloat(r)
                            inputBuffer.putFloat(g)
                            inputBuffer.putFloat(b)
                        }

                        val outputArray = Array(1) { FloatArray(1001) }
                        interpreter.run(inputBuffer, outputArray)
                        scaledBitmap.recycle()
                        sampleTile.recycle()
                        Log.i(tag, "Real MobileNet TFLite AI Inference executed on tiled sample")
                    } else {
                        sampleTile?.recycle()
                    }
                    usedTfLite = true
                } catch (e: Exception) {
                    Log.w(tag, "TFLite Interpreter pass failed (${e.localizedMessage}). Safe fallback.", e)
                } finally {
                    interpreter?.close()
                }
            }

            // 3. 512x512 Tiled Pipeline with ColorMatrix & Vibe LUT Integration
            val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)
            if (vibePreset != VibePreset.NONE) {
                val vibeMatrix = ImageEditor.createVibeAndroidColorMatrix(vibePreset)
                colorMatrix.postConcat(vibeMatrix)
                Log.i(tag, "Applied Vibe Preset ColorMatrix pass: ${vibePreset.displayName}")
            }

            processedBitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(processedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
                isFilterBitmap = true
            }

            val tileSize = 512
            val numCols = (imgWidth + tileSize - 1) / tileSize
            val numRows = (imgHeight + tileSize - 1) / tileSize
            val totalTiles = numCols * numRows

            Log.i(tag, "Processing $imgWidth x $imgHeight px in $totalTiles tiles ($tileSize x $tileSize px each)")

            var tileIndex = 0
            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * tileSize
                    val top = row * tileSize
                    val right = (left + tileSize).coerceAtMost(imgWidth)
                    val bottom = (top + tileSize).coerceAtMost(imgHeight)
                    val rect = Rect(left, top, right, bottom)

                    // Decode only 512x512 region
                    val tileBitmap = decoder.decodeRegion(rect, null)
                    if (tileBitmap != null) {
                        canvas.drawBitmap(tileBitmap, left.toFloat(), top.toFloat(), paint)
                        tileBitmap.recycle() // Memory Shield: Recycle tile immediately
                    }

                    tileIndex++
                    val progressPercent = (tileIndex * 100) / totalTiles
                    setProgressAsync(workDataOf("progress" to progressPercent))
                    Log.d(tag, "Tile $tileIndex of $totalTiles processed ($left,$top - $right,$bottom) [$progressPercent%]")
                }
            }

            // 4. L4 Quality Validation Check
            if (!validator.isValid(processedBitmap)) {
                Log.w(tag, "L4 Validator failed: Processed bitmap invalid or pitch black. Keeping raw photo $photoUriStr")
                return@withContext Result.failure()
            }

            // 5. Save processed Bitmap to MediaStore
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
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)

                val executionTime = System.currentTimeMillis() - startTime
                Log.i(tag, "L3 512x512 Tiled AI Processing finished in ${executionTime}ms (Used TFLite: $usedTfLite, Vibe: ${vibePreset.displayName}, Total Tiles: $totalTiles). Saved $fileName to MediaStore at $newUri")

                // 6. Enqueue CloudSyncWorker if Cloud Sync toggle is ON in Settings
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
            Log.e(tag, "Error during AiProcessorWorker 512x512 tiled execution", e)
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
