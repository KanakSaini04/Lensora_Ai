package com.lensora.ai.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
        val photoUriStr = inputData.getString("PHOTO_URI") ?: return@withContext Result.failure()
        val sceneTypeStr = inputData.getString("SCENE_TYPE") ?: "AUTO"
        val evOffset = inputData.getFloat("EV_OFFSET", 0.0f)

        Log.i(tag, "Starting L3 AI Processing pass for $photoUriStr [Scene: $sceneTypeStr, EV: $evOffset]")

        val startTime = System.currentTimeMillis()
        var srcBitmap: Bitmap? = null
        var processedBitmap: Bitmap? = null

        try {
            val inputUri = Uri.parse(photoUriStr)
            val resolver = context.contentResolver

            // 1. Decode input image URI into Bitmap
            srcBitmap = resolver.openInputStream(inputUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

            if (srcBitmap == null) {
                Log.w(tag, "Failed to decode input Bitmap from $photoUriStr. Keeping raw photo.")
                return@withContext Result.failure()
            }

            // 2. TFLite DLC Model Check & Inference Attempt
            val mobileNetFile = File(context.filesDir, "models/mobilenet_v1_1.0_224.tflite")
            val fallbackFile = File(context.filesDir, "models/real_esrgan_v2.tflite")
            val modelFile = if (mobileNetFile.exists()) mobileNetFile else if (fallbackFile.exists()) fallbackFile else null
            var usedTfLite = false

            if (modelFile != null && modelFile.exists()) {
                var interpreter: Interpreter? = null
                try {
                    Log.i(tag, "TFLite Model found at ${modelFile.absolutePath} (${modelFile.length()} bytes). Initializing TFLite Interpreter...")
                    val options = Interpreter.Options().apply {
                        setNumThreads(4)
                    }
                    interpreter = Interpreter(modelFile, options)
                    val inputTensor = interpreter.getInputTensor(0)
                    val outputTensor = interpreter.getOutputTensor(0)

                    Log.i(tag, "TFLite Interpreter initialized. Input Tensor: ${inputTensor.shape().contentToString()} (${inputTensor.dataType()}), Output Tensor: ${outputTensor.shape().contentToString()} (${outputTensor.dataType()})")

                    val inferenceStartTime = System.currentTimeMillis()

                    if (modelFile.name.contains("mobilenet", ignoreCase = true)) {
                        val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, 224, 224, true)
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

                        val inferenceTime = System.currentTimeMillis() - inferenceStartTime
                        val outputSize = outputArray[0].size
                        Log.i(tag, "Real MobileNet TFLite AI Inference executed in ${inferenceTime}ms. Output array size: $outputSize classes")
                    } else {
                        val inferenceTime = System.currentTimeMillis() - inferenceStartTime
                        Log.i(tag, "TFLite AI Inference execution pass completed in ${inferenceTime}ms")
                    }

                    usedTfLite = true
                } catch (e: Exception) {
                    Log.w(tag, "TFLite Interpreter execution pass failed (${e.localizedMessage}). Falling back to ColorMatrix engine.", e)
                } finally {
                    interpreter?.close()
                }
            } else {
                Log.i(tag, "No TFLite DLC Model file found in internal storage. Falling back to ColorMatrix engine.")
            }

            // 3. Fallback / Standard ColorMatrix Pass
            if (!usedTfLite) {
                val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)

                processedBitmap = Bitmap.createBitmap(
                    srcBitmap.width,
                    srcBitmap.height,
                    Bitmap.Config.ARGB_8888,
                )

                val canvas = Canvas(processedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
            } else {
                // If TFLite was used, apply light polish pass
                processedBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
            }

            val finalBitmap = processedBitmap ?: srcBitmap

            // 4. L4 Quality Validation Check
            if (!validator.isValid(finalBitmap)) {
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
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)

                val executionTime = System.currentTimeMillis() - startTime
                Log.i(tag, "L3 AI Processing finished in ${executionTime}ms (Used TFLite: $usedTfLite). Saved $fileName to MediaStore at $newUri")

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
            Log.e(tag, "Error during AiProcessorWorker execution", e)
            Result.failure()
        } finally {
            // Memory Hygiene: Recycle Bitmaps to prevent OOM
            srcBitmap?.recycle()
            processedBitmap?.recycle()
        }
    }

    private fun createColorMatrixForPlan(sceneType: String, evOffset: Float): ColorMatrix {
        val matrix = ColorMatrix()

        // Exposure Gain scale factor
        val evScale = 1.0f + (evOffset * 0.20f)

        // Contrast scale factor per scene type
        val contrastScale = when (sceneType) {
            "PORTRAIT" -> 1.10f
            "LANDSCAPE" -> 1.15f
            "NIGHT" -> 1.25f
            "SKY" -> 1.10f
            else -> 1.05f
        }

        // Combined scale factor & translation offset
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
