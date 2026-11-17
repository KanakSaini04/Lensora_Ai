package com.lensora.ai.editor.import

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.ProjectRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import kotlin.coroutines.coroutineContext

sealed class ImportState {
    object Idle : ImportState()
    data class StorageBlocked(val availableBytes: Long, val requiredBytes: Long) : ImportState()
    data class Progress(val currentClip: Int, val totalClips: Int, val currentClipName: String) : ImportState()
    data class Success(val project: Project, val skippedCount: Int = 0) : ImportState()
    data class Error(val message: String) : ImportState()
    object Cancelled : ImportState()
}

object ExtensionHelper {
    val ALLOWED_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "heic", "heif",
        "mp4", "mov", "webm", "3gp", "mkv"
    )

    fun deriveExtension(context: Context, uri: Uri, mimeType: String?, isVideo: Boolean): String {
        val cleanMime = mimeType?.lowercase(Locale.US)?.ifBlank { null }
            ?: try {
                context.contentResolver.getType(uri)?.lowercase(Locale.US)
            } catch (_: Exception) {
                null
            }

        val rawExt = cleanMime?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }?.lowercase(Locale.US)

        val fallbackExt = if (isVideo) "mp4" else "jpg"

        val finalExt = if (rawExt != null && rawExt in ALLOWED_EXTENSIONS) {
            rawExt
        } else {
            fallbackExt
        }

        return ".$finalExt"
    }
}

object StorageGuard {
    private const val TWO_GB_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB Guard Limit

    fun calculateRequiredBytes(totalPickedBytes: Long): Long {
        return maxOf(TWO_GB_BYTES, totalPickedBytes + TWO_GB_BYTES)
    }

    fun evaluateStorageGuard(availableBytes: Long, totalPickedBytes: Long): Boolean {
        val requiredBytes = calculateRequiredBytes(totalPickedBytes)
        return availableBytes >= requiredBytes
    }

    fun getAvailableStorageBytes(dir: File): Long {
        return try {
            val stat = StatFs(dir.path)
            stat.availableBytes
        } catch (e: Exception) {
            Log.e("StorageGuard", "Error checking storage space", e)
            0L
        }
    }
}

class ProjectImporter(
    private val context: Context,
    private val repository: ProjectRepository = ProjectRepository(context),
) {
    private val tag = "ProjectImporter"

    suspend fun extractVideoDurationMs(filePath: String, defaultDurationMs: Long = 3000L): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull()?.takeIf { it > 0 } ?: defaultDurationMs
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(tag, "Failed to extract video duration for $filePath. Fallback to $defaultDurationMs ms.", e)
            defaultDurationMs
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun getUriSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(tag, "Failed to query size for $uri", e)
            0L
        }
    }

    fun importMediaFlow(
        pickedUris: List<Uri>,
        projectName: String,
        aspectRatio: AspectRatio = AspectRatio.NINE_SIXTEEN,
    ): Flow<ImportState> = flow {
        if (pickedUris.isEmpty()) {
            emit(ImportState.Error("No media files selected"))
            return@flow
        }

        // 1. Storage Guard Evaluation
        val totalPickedBytes = pickedUris.sumOf { getUriSize(it) }
        val availableBytes = StorageGuard.getAvailableStorageBytes(context.filesDir)
        val requiredBytes = StorageGuard.calculateRequiredBytes(totalPickedBytes)

        if (!StorageGuard.evaluateStorageGuard(availableBytes, totalPickedBytes)) {
            Log.w(tag, "Storage Guard Blocked Import: Available $availableBytes bytes < Required $requiredBytes bytes")
            emit(ImportState.StorageBlocked(availableBytes, requiredBytes))
            return@flow
        }

        // 2. Setup Project Folder
        val projectId = "proj_${System.currentTimeMillis()}"
        val projectDir = File(context.filesDir, "projects/$projectId")
        val mediaDir = File(projectDir, "media")

        if (!mediaDir.exists()) {
            mediaDir.mkdirs()
        }

        val clips = mutableListOf<Clip>()
        var skippedCount = 0

        try {
            for ((index, uri) in pickedUris.withIndex()) {
                if (!coroutineContext.isActive) {
                    Log.i(tag, "Import cancelled during processing clip $index")
                    cleanupFolder(projectDir)
                    throw CancellationException("Import cancelled")
                }

                val mimeType = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
                val isVideo = mimeType?.startsWith("video", ignoreCase = true) == true
                val clipType = if (isVideo) ClipType.VIDEO else ClipType.PHOTO
                val ext = ExtensionHelper.deriveExtension(context, uri, mimeType, isVideo)

                val clipId = "clip_${System.currentTimeMillis()}_$index"
                val destFile = File(mediaDir, "$clipId$ext")
                val tmpFile = File(mediaDir, "$clipId$ext.tmp")

                emit(ImportState.Progress(index + 1, pickedUris.size, "$clipId$ext"))

                // Atomic copy: Stream to .tmp file first
                var copySuccess = false
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        inputStream.use { input ->
                            FileOutputStream(tmpFile).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    if (!coroutineContext.isActive) {
                                        throw CancellationException("Import cancelled during byte stream copy")
                                    }
                                    output.write(buffer, 0, read)
                                }
                                output.flush()
                            }
                        }
                    }

                    // Atomic rename
                    if (tmpFile.exists() && tmpFile.length() > 0) {
                        copySuccess = tmpFile.renameTo(destFile)
                        if (!copySuccess) {
                            tmpFile.copyTo(destFile, overwrite = true)
                            tmpFile.delete()
                            copySuccess = true
                        }
                    }
                } catch (e: CancellationException) {
                    Log.i(tag, "Import stream copy cancelled for $uri")
                    cleanupFolder(projectDir)
                    throw e
                } catch (e: Exception) {
                    Log.e(tag, "Error copying stream for $uri to $tmpFile", e)
                    if (tmpFile.exists()) tmpFile.delete()
                    copySuccess = false
                }

                if (!copySuccess || !destFile.exists()) {
                    Log.w(tag, "Skipping uncopyable media file $uri")
                    skippedCount++
                    continue
                }

                val durationMs = if (clipType == ClipType.PHOTO) {
                    3000L
                } else {
                    extractVideoDurationMs(destFile.absolutePath)
                }

                val clip = Clip(
                    id = clipId,
                    sourceUri = Uri.fromFile(destFile).toString(),
                    type = clipType,
                    durationMs = durationMs,
                )

                if (clip.validate()) {
                    clips.add(clip)
                } else {
                    Log.w(tag, "Clip $clipId failed validation. Skipping.")
                    skippedCount++
                }
            }

            if (clips.isEmpty()) {
                cleanupFolder(projectDir)
                emit(ImportState.Error("No valid clips were imported"))
                return@flow
            }

            val project = Project(
                id = projectId,
                name = projectName,
                aspect = aspectRatio,
                clips = clips,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )

            val saved = repository.saveProject(project)
            if (saved) {
                Log.i(tag, "Project $projectId successfully created with ${clips.size} clips (skipped $skippedCount)")
                emit(ImportState.Success(project, skippedCount = skippedCount))
            } else {
                cleanupFolder(projectDir)
                emit(ImportState.Error("Failed to save project metadata"))
            }

        } catch (e: CancellationException) {
            Log.i(tag, "Import job cancelled")
            cleanupFolder(projectDir)
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Unhandled exception during media import", e)
            cleanupFolder(projectDir)
            emit(ImportState.Error("Import failed: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    private fun cleanupFolder(dir: File) {
        try {
            if (dir.exists()) {
                dir.deleteRecursively()
                Log.i(tag, "Cleaned up temporary project directory ${dir.name}")
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to cleanup directory ${dir.name}", e)
        }
    }
}
