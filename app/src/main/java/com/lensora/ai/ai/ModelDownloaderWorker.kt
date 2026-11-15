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
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

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
        // Read asset manifest.json
        val (manifestModelName, manifestUrl) = try {
            val jsonStr = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)
            Pair(
                json.optString("model_name", "mobilenet_v1_1.0_224.tflite"),
                json.optString("url", "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224.zip"),
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse assets/manifest.json. Using defaults.", e)
            Pair("mobilenet_v1_1.0_224.tflite", "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224.zip")
        }

        val modelUrl = inputData.getString("MODEL_URL") ?: manifestUrl
        val targetFilename = inputData.getString("TARGET_FILENAME") ?: manifestModelName
        val expectedSha256 = inputData.getString("EXPECTED_SHA256")

        Log.i(tag, "Starting TFLite Model Download from $modelUrl for $targetFilename")

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

            var totalBytesRead: Long = 0

            // If the URL points to a ZIP file, extract the .tflite entry in memory
            if (modelUrl.endsWith(".zip", ignoreCase = true)) {
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                var foundTfLite = false

                while (entry != null) {
                    if (entry.name.endsWith(".tflite", ignoreCase = true)) {
                        foundTfLite = true
                        Log.i(tag, "Found TFLite entry in ZIP: ${entry.name} (${entry.size} bytes). Extracting...")

                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (zipInputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            digest.update(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progressPercent = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                                setProgress(workDataOf("PROGRESS" to progressPercent))
                            }
                        }
                        zipInputStream.closeEntry()
                        break
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }

                zipInputStream.close()

                if (!foundTfLite) {
                    Log.e(tag, "No .tflite file found inside ZIP archive at $modelUrl")
                    outputStream.close()
                    tmpFile.delete()
                    return@withContext Result.failure()
                }
            } else {
                // Direct file stream pass
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progressPercent = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        setProgress(workDataOf("PROGRESS" to progressPercent))
                    }
                }
                inputStream.close()
            }

            outputStream.flush()
            outputStream.close()

            // Calculate SHA-256 Hash
            val sha256Bytes = digest.digest()
            val sha256Hex = sha256Bytes.joinToString("") { "%02x".format(it) }

            Log.i(tag, "Extracted & downloaded $targetFilename successfully ($totalBytesRead bytes). SHA-256: $sha256Hex")

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

            Log.i(tag, "Finalized TFLite AI Model saved at ${finalFile.absolutePath} (${finalFile.length()} bytes)")

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
