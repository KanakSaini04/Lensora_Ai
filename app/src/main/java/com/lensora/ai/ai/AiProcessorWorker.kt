package com.lensora.ai.ai

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

        try {
            val inputUri = Uri.parse(photoUriStr)

            // Simulate heavy AI processing pass (2-second computational delay on Dispatchers.IO)
            delay(2000)

            val resolver = context.contentResolver
            val bitmap = resolver.openInputStream(inputUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }

            // L4 Quality Validation Check
            if (!validator.isValid(bitmap)) {
                Log.w(tag, "L4 Validator failed: Bitmap invalid or pitch black. Keeping raw photo $photoUriStr")
                return@withContext Result.failure()
            }

            // Write final processed version to MediaStore
            val timestamp = System.currentTimeMillis()
            val fileName = "LENSORA_AI_PROCESSED_$timestamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Pictures/Lensora_Ai",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val newUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )

            if (newUri != null) {
                resolver.openInputStream(inputUri)?.use { inStream ->
                    resolver.openOutputStream(newUri)?.use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)

                Log.i(tag, "L3 AI Processing complete: Saved $fileName to MediaStore at $newUri")
                Result.success()
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for processed image")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during AiProcessorWorker execution", e)
            Result.failure()
        }
    }
}
