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
        val targetKey = inputData.getString("TARGET_KEY") ?: "scene_classifier"

        val manifestEntries = try {
            val jsonStr = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
            ManifestParser.parseManifest(jsonStr)
        } catch (e: Exception) {
            Log.w(tag, "Failed to read assets/manifest.json", e)
            emptyList()
        }

        val manifestEntry = manifestEntries.firstOrNull { it.key == targetKey }

        val modelUrl = inputData.getString("MODEL_URL") ?: manifestEntry?.url
        val targetFilename = inputData.getString("TARGET_FILENAME") ?: manifestEntry?.fileName ?: "$targetKey.tflite"
        val expectedSha256 = inputData.getString("EXPECTED_SHA256") ?: manifestEntry?.sha256

        if (modelUrl.isNullOrBlank() || modelUrl.equals("null", ignoreCase = true)) {
            Log.w(tag, "Model URL is null or unconfigured for $targetFilename ($targetKey). Skipping network call.")
            return@withContext Result.failure()
        }

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

            val actualSha256Hex = Sha256Util.hex(tmpFile)
            Log.i(tag, "Extracted & downloaded $targetFilename successfully ($totalBytesRead bytes). SHA-256: $actualSha256Hex")

            if (!expectedSha256.isNullOrBlank()) {
                if (!Sha256Util.matches(tmpFile, expectedSha256)) {
                    Log.e(tag, "SHA-256 mismatch! Expected: $expectedSha256, Computed: $actualSha256Hex")
                    tmpFile.delete()
                    return@withContext Result.failure()
                }
                Log.i(tag, "SHA-256 checksum verification PASSED!")
            } else {
                Log.i(tag, "SHA-256 checksum unverified (no expected sha256 in manifest)")
            }

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
                    "SHA256" to actualSha256Hex,
                ),
            )
        } catch (e: Exception) {
            Log.e(tag, "Error downloading model from $modelUrl", e)
            Result.failure()
        }
    }
}
