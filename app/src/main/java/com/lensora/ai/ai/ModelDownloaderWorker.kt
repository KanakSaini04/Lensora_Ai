package com.lensora.ai.ai

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ModelDownloaderWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val tag = "ModelDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = inputData.getString("MODEL_URL") ?: return@withContext Result.failure()
        val targetFilename = inputData.getString("TARGET_FILENAME") ?: "model.tflite"
        val expectedSha256 = inputData.getString("EXPECTED_SHA256")

        Log.i(tag, "Starting GitHub DLC Model Download from $modelUrl for $targetFilename")

        try {
            val modelsDir = File(context.filesDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val tmpFile = File(modelsDir, "$targetFilename.tmp")
            val finalFile = File(modelsDir, targetFilename)

            val request = Request.Builder()
                .url(modelUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(tag, "HTTP download failed with code ${response.code} for $modelUrl")
                return@withContext Result.failure()
            }

            val body = response.body ?: run {
                Log.e(tag, "HTTP response body is null for $modelUrl")
                return@withContext Result.failure()
            }

            val contentLength = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tmpFile)
            val digest = MessageDigest.getInstance("SHA-256")

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                if (contentLength > 0) {
                    val progressPercent = ((totalBytesRead * 100) / contentLength).toInt()
                    setProgress(
                        workDataOf(
                            "PROGRESS" to progressPercent,
                            "BYTES_READ" to totalBytesRead,
                            "TOTAL_BYTES" to contentLength,
                        ),
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Calculate SHA-256 Hash
            val sha256Bytes = digest.digest()
            val sha256Hex = sha256Bytes.joinToString("") { "%02x".format(it) }

            Log.i(tag, "Downloaded $targetFilename successfully ($totalBytesRead bytes). Calculated SHA-256: $sha256Hex")

            if (!expectedSha256.isNullOrBlank()) {
                if (!sha256Hex.equals(expectedSha256, ignoreCase = true)) {
                    Log.e(tag, "SHA-256 mismatch! Expected: $expectedSha256, Got: $sha256Hex")
                    tmpFile.delete()
                    return@withContext Result.failure()
                }
                Log.i(tag, "SHA-256 checksum verification PASSED!")
            }

            // Rename .tmp file to final target filename
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tmpFile.renameTo(finalFile)
            if (!renamed) {
                tmpFile.copyTo(finalFile, overwrite = true)
                tmpFile.delete()
            }

            Log.i(tag, "Finalized AI Model saved at ${finalFile.absolutePath}")

            Result.success(
                workDataOf(
                    "FILE_PATH" to finalFile.absolutePath,
                    "SHA256" to sha256Hex,
                ),
            )
        } catch (e: Exception) {
            Log.e(tag, "Error downloading model from $modelUrl", e)
            Result.failure()
        }
    }
}
