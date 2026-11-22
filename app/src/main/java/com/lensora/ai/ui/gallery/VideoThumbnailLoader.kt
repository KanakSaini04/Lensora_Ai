package com.lensora.ai.ui.gallery

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

fun formatDurationMs(durationMs: Long?): String {
    if (durationMs == null || durationMs <= 0L) return "00:00"
    val totalSeconds = durationMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun isVideoMimeType(mimeType: String?): Boolean {
    if (mimeType.isNullOrBlank()) return false
    return mimeType.startsWith("video", ignoreCase = true)
}

fun buildThumbnailCacheKey(uriStr: String, dateAddedMs: Long, sizeBytes: Long): String {
    return "${uriStr}_${dateAddedMs}_${sizeBytes}"
}

class VideoThumbnailLoader(
    private val context: Context,
) {
    private val maxMemoryBytes = 16 * 1024 * 1024 // 16 MB LruCache
    private val memoryCache = object : LruCache<String, Bitmap>(maxMemoryBytes) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    suspend fun loadVideoThumbnail(
        uri: Uri,
        dateAddedMs: Long = 0L,
        sizeBytes: Long = 0L,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = buildThumbnailCacheKey(uri.toString(), dateAddedMs, sizeBytes)
        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        try {
            val thumbnail = context.contentResolver.loadThumbnail(
                uri,
                Size(512, 512),
                null,
            )
            if (thumbnail != null) {
                memoryCache.put(cacheKey, thumbnail)
            }
            thumbnail
        } catch (e: Exception) {
            android.util.Log.e("VideoThumbnailLoader", "Failed to load video thumbnail for $uri", e)
            null
        }
    }
}
