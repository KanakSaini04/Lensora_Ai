package com.lensora.ai.data

import android.content.ContentUris
import android.content.Context
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GalleryFilter(val displayName: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    RAW_DNG("RAW DNG"),
    EDITED("Edited"),
}

data class MediaItemInfo(
    val uri: Uri,
    val isVideo: Boolean,
    val isRaw: Boolean,
    val isEdited: Boolean,
    val displayName: String,
    val dateAddedMs: Long,
    val sizeBytes: Long,
    val mimeType: String,
    val durationMs: Long = 0L,
)

data class ExifMetadata(
    val fileName: String,
    val fileSizeFormatted: String,
    val resolution: String,
    val dateFormatted: String,
    val mimeType: String,
    val path: String,
    val iso: String? = null,
    val exposureTime: String? = null,
    val fNumber: String? = null,
    val cameraModel: String? = null,
)

class GalleryRepository(
    private val context: Context,
) {
    private val tag = "GalleryRepository"

    suspend fun getRecentPhotos(limit: Int = 10): List<Uri> = withContext(Dispatchers.IO) {
        return@withContext getFilteredMediaItems(GalleryFilter.PHOTOS).take(limit).map { it.uri }
    }

    suspend fun getAllPhotos(): List<Uri> = withContext(Dispatchers.IO) {
        return@withContext getFilteredMediaItems(GalleryFilter.PHOTOS).map { it.uri }
    }

    suspend fun getFilteredMediaItems(filter: GalleryFilter = GalleryFilter.ALL): List<MediaItemInfo> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItemInfo>()

        // 1. Query Images (Pictures/Lensora_Ai)
        if (filter != GalleryFilter.VIDEOS) {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.RELATIVE_PATH,
            )

            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%Pictures/Lensora_Ai%")
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Lensora_Photo.jpg"
                        val dateAddedSecs = cursor.getLong(dateCol)
                        val size = cursor.getLong(sizeCol)
                        val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        val isRaw = name.endsWith(".dng", ignoreCase = true) || mime.contains("dng", ignoreCase = true)
                        val isEdited = name.startsWith("LENSORA_EDITED", ignoreCase = true) || name.startsWith("LENSORA_AI_PROCESSED", ignoreCase = true)

                        val matchesFilter = when (filter) {
                            GalleryFilter.ALL -> true
                            GalleryFilter.PHOTOS -> !isRaw
                            GalleryFilter.VIDEOS -> false
                            GalleryFilter.RAW_DNG -> isRaw
                            GalleryFilter.EDITED -> isEdited
                        }

                        if (matchesFilter) {
                            items.add(
                                MediaItemInfo(
                                    uri = contentUri,
                                    isVideo = false,
                                    isRaw = isRaw,
                                    isEdited = isEdited,
                                    displayName = name,
                                    dateAddedMs = dateAddedSecs * 1000L,
                                    sizeBytes = size,
                                    mimeType = mime,
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error querying MediaStore images", e)
            }
        }

        // 2. Query Videos (Movies/Lensora_Ai)
        if (filter == GalleryFilter.ALL || filter == GalleryFilter.VIDEOS) {
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.RELATIVE_PATH,
                MediaStore.Video.Media.DURATION,
            )

            val selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("%Movies/Lensora_Ai%")
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            try {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Lensora_Video.mp4"
                        val dateAddedSecs = cursor.getLong(dateCol)
                        val size = cursor.getLong(sizeCol)
                        val mime = cursor.getString(mimeCol) ?: "video/mp4"
                        val duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        items.add(
                            MediaItemInfo(
                                uri = contentUri,
                                isVideo = true,
                                isRaw = false,
                                isEdited = false,
                                displayName = name,
                                dateAddedMs = dateAddedSecs * 1000L,
                                sizeBytes = size,
                                mimeType = mime,
                                durationMs = duration,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error querying MediaStore videos", e)
            }
        }

        items.sortedByDescending { it.dateAddedMs }
    }

    suspend fun getExifMetadata(uri: Uri): ExifMetadata = withContext(Dispatchers.IO) {
        var name = "Lensora_Media"
        var sizeBytes = 0L
        var mimeType = "image/jpeg"
        var dateAddedMs = System.currentTimeMillis()

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val mimeIdx = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                    val dateIdx = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

                    if (nameIdx != -1) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                    if (mimeIdx != -1) mimeType = cursor.getString(mimeIdx) ?: mimeType
                    if (dateIdx != -1) dateAddedMs = cursor.getLong(dateIdx) * 1000L
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to query metadata for $uri", e)
        }

        val sizeMb = String.format(Locale.US, "%.2f MB", sizeBytes / (1024f * 1024f))
        val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(dateAddedMs))

        var width = 0
        var height = 0
        var iso: String? = null
        var exposureTime: String? = null
        var fNumber: String? = null
        var cameraModel: String? = null

        if (mimeType.startsWith("video")) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                width = w?.toIntOrNull() ?: 0
                height = h?.toIntOrNull() ?: 0
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        } else {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL)
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to parse EXIF tags for $uri", e)
            }
        }

        val resStr = if (width > 0 && height > 0) "${width}x${height}" else "Unknown Resolution"

        ExifMetadata(
            fileName = name,
            fileSizeFormatted = sizeMb,
            resolution = resStr,
            dateFormatted = dateStr,
            mimeType = mimeType,
            path = uri.toString(),
            iso = iso,
            exposureTime = exposureTime,
            fNumber = fNumber,
            cameraModel = cameraModel,
        )
    }
}
