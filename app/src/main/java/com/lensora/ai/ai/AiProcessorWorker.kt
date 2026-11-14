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
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

            // 2. Build ColorMatrix based on ProcessingPlan parameters
            val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)

            // 3. Apply ColorMatrix filter onto new Bitmap using Canvas
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
                Log.i(tag, "L3 ColorMatrix AI Processing finished in ${executionTime}ms. Saved $fileName to MediaStore at $newUri")

                Result.success()
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for processed image")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during AiProcessorWorker ColorMatrix execution", e)
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
