package com.lensora.ai.editor.ui

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VideoThumbnailLoader {
    private const val MAX_ENTRIES = 50
    private val bitmapCache = object : LruCache<String, Bitmap>(MAX_ENTRIES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return 1
        }
    }

    suspend fun loadThumbnail(context: Context, sourceUri: String): Bitmap? = withContext(Dispatchers.IO) {
        val cached = bitmapCache.get(sourceUri)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        try {
            val retriever = MediaMetadataRetriever()
            val uri = Uri.parse(sourceUri)
            if (sourceUri.startsWith("content://") || sourceUri.startsWith("file://")) {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(sourceUri)
            }

            val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            retriever.release()

            if (frame != null) {
                bitmapCache.put(sourceUri, frame)
            }
            frame
        } catch (e: Exception) {
            null
        }
    }
}
