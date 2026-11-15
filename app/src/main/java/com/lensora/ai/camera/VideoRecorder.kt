package com.lensora.ai.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import java.io.File

class VideoRecorder(
    private val context: Context,
) {
    private val tag = "VideoRecorder"

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecordingActive = false

    @Suppress("DEPRECATION")
    fun prepare(
        hasAudioPermission: Boolean,
        sensorOrientation: Int,
        displayRotationDegrees: Int,
    ): Surface {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }

        if (hasAudioPermission) {
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            } catch (e: Exception) {
                Log.w(tag, "Audio source configuration failed. Proceeding without audio.", e)
            }
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        val timestamp = System.currentTimeMillis()
        val outputFile = File(context.cacheDir, "LENSORA_VIDEO_$timestamp.mp4.tmp")
        currentOutputFile = outputFile
        recorder.setOutputFile(outputFile.absolutePath)

        recorder.setVideoEncodingBitRate(10_000_000)
        recorder.setVideoFrameRate(30)
        recorder.setVideoSize(1920, 1080)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        if (hasAudioPermission) {
            try {
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            } catch (e: Exception) {
                Log.w(tag, "Audio encoder configuration failed.", e)
            }
        }

        val rotation = (sensorOrientation - displayRotationDegrees + 360) % 360
        recorder.setOrientationHint(rotation)

        recorder.prepare()
        mediaRecorder = recorder
        Log.i(tag, "MediaRecorder prepared successfully for ${outputFile.name}")

        return recorder.surface
    }

    fun start() {
        try {
            mediaRecorder?.start()
            isRecordingActive = true
            Log.i(tag, "MediaRecorder video recording started.")
        } catch (e: Exception) {
            Log.e(tag, "Error starting MediaRecorder", e)
            throw e
        }
    }

    fun stop(): Uri? {
        if (!isRecordingActive) return null
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            isRecordingActive = false
            Log.i(tag, "MediaRecorder video recording stopped.")

            val tmpFile = currentOutputFile ?: return null
            if (!tmpFile.exists()) return null

            // Save completed video to MediaStore Movies/Lensora_Ai
            val timestamp = System.currentTimeMillis()
            val fileName = "LENSORA_VIDEO_$timestamp.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Lensora_Ai")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            )

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    tmpFile.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                tmpFile.delete()
                Log.i(tag, "Saved recorded video to MediaStore Movies/Lensora_Ai/$fileName at $uri")
                uri
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for video")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping MediaRecorder", e)
            null
        }
    }

    fun release() {
        try {
            if (isRecordingActive) {
                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    Log.w(tag, "Error stopping active MediaRecorder during release", e)
                }
            }
            mediaRecorder?.reset()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecordingActive = false
            currentOutputFile?.delete()
            Log.i(tag, "MediaRecorder released safely.")
        } catch (e: Exception) {
            Log.e(tag, "Error releasing MediaRecorder", e)
        }
    }
}
