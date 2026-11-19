package com.lensora.ai.editor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
class ExportWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val tag = "ExportWorker"

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "lensora_export_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val projectId = inputData.getString("PROJECT_ID") ?: return@withContext Result.failure()
        val resolutionHeight = inputData.getInt("RESOLUTION_HEIGHT", 1080)

        // 1. Storage Guard Check
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBytes
        if (!ExportGuard.isStorageSufficient(availableBytes)) {
            Log.e(tag, "Export aborted: low storage space ($availableBytes bytes free)")
            return@withContext Result.failure(workDataOf("ERROR" to "Low storage space (< 2GB available)"))
        }

        // 2. Thermal Guard Check
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val initialThermalStatus = powerManager?.currentThermalStatus ?: 0
        if (!ExportGuard.isThermalSafe(initialThermalStatus)) {
            Log.e(tag, "Export aborted: severe thermal throttling (status $initialThermalStatus)")
            return@withContext Result.failure(workDataOf("ERROR" to "Device temperature too high"))
        }

        val repository = ProjectRepository(context)
        val project = repository.loadProject(projectId) ?: run {
            Log.e(tag, "Project $projectId not found")
            return@withContext Result.failure(workDataOf("ERROR" to "Project not found"))
        }

        // 3. Promote to Foreground Task with dataSync Service Type
        try {
            setForeground(createForegroundInfo(project.name))
        } catch (e: Exception) {
            Log.w(tag, "Could not set foreground service info for export worker", e)
        }

        val plan = ExportPlanBuilder.buildPlan(project, resolutionHeight)
        Log.i(tag, "Starting Real Media3 Transformer Export for ${plan.projectName} (${plan.targetWidth}x${plan.targetHeight})")

        val exportsDir = File(context.filesDir, "exports")
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }

        val outputFilename = "export_${project.id}_${System.currentTimeMillis()}.mp4"
        val outputFile = File(exportsDir, outputFilename)

        try {
            // Execute Transformer pass on Main Looper
            withContext(Dispatchers.Main) {
                runTransformerExport(context, plan, outputFile, powerManager) { progress ->
                    setProgressAsync(workDataOf("PROGRESS" to progress))
                }
            }

            if (!outputFile.exists() || outputFile.length() == 0L) {
                Log.e(tag, "Transformer produced an empty or missing output file")
                if (outputFile.exists()) outputFile.delete()
                return@withContext Result.failure(workDataOf("ERROR" to "Export output file is empty"))
            }

            // Expose exported file to MediaStore via IS_PENDING pattern
            val mediaStoreUri = saveToMediaStore(outputFile, outputFilename)

            if (mediaStoreUri == null) {
                Log.e(tag, "MediaStore save failed for $outputFilename. Keeping internal copy.")
                return@withContext Result.failure(workDataOf("ERROR" to "Failed to save export to MediaStore"))
            }

            // Delete internal staging copy after successful MediaStore save
            if (outputFile.exists()) {
                outputFile.delete()
                Log.i(tag, "Internal staging copy deleted after successful MediaStore save")
            }

            Log.i(tag, "Export completed successfully: MediaStore URI $mediaStoreUri")

            Result.success(
                workDataOf(
                    "MEDIASTORE_URI" to mediaStoreUri.toString(),
                    "DURATION_MS" to plan.totalDurationMs,
                )
            )
        } catch (e: CancellationException) {
            Log.w(tag, "Export worker cancelled, deleting partial output file")
            if (outputFile.exists()) outputFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(tag, "Export failed with error", e)
            if (outputFile.exists()) outputFile.delete()
            Result.failure(workDataOf("ERROR" to (e.localizedMessage ?: "Export failed")))
        }
    }

    private suspend fun runTransformerExport(
        context: Context,
        plan: ExportPlan,
        outputFile: File,
        powerManager: PowerManager?,
        onProgress: (Int) -> Unit,
    ) = suspendCancellableCoroutine<Unit> { continuation ->

        val editedMediaItems = mutableListOf<EditedMediaItem>()

        for (item in plan.items) {
            val uri = when {
                item.sourceUri.startsWith("content://") || item.sourceUri.startsWith("file://") -> Uri.parse(item.sourceUri)
                else -> Uri.fromFile(File(item.sourceUri))
            }

            val mediaItemBuilder = MediaItem.Builder().setUri(uri)

            if (!item.isPhoto) {
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(item.startMs)
                    .setEndPositionMs(item.endMs)
                    .build()
                mediaItemBuilder.setClippingConfiguration(clippingConfig)
            } else {
                mediaItemBuilder.setImageDurationMs(item.durationMs)
            }

            val videoEffects = mutableListOf<Effect>(
                Presentation.createForWidthAndHeight(
                    plan.targetWidth,
                    plan.targetHeight,
                    ExportPlanBuilder.ASPECT_MODE,
                ),
            )

            val audioProcessors = mutableListOf<AudioProcessor>()

            if (item.speed != 1.0f && item.speed > 0f) {
                videoEffects.add(SpeedChangeEffect(item.speed))
                audioProcessors.add(SonicAudioProcessor().apply { setSpeed(item.speed) })
            }

            val editedItemBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
                .setEffects(Effects(audioProcessors, videoEffects))

            if (item.isPhoto) {
                editedItemBuilder.setFrameRate(30)
                editedItemBuilder.setRemoveAudio(true)
            }

            editedMediaItems.add(editedItemBuilder.build())
        }

        val sequence = EditedMediaItemSequence.Builder(editedMediaItems).build()
        val composition = Composition.Builder(listOf(sequence)).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            })
            .build()

        val progressHolder = ProgressHolder()
        val handler = Handler(Looper.getMainLooper())

        val progressRunnable = object : Runnable {
            override fun run() {
                if (!continuation.isActive) return

                val currentThermal = powerManager?.currentThermalStatus ?: 0
                if (!ExportGuard.isThermalSafe(currentThermal)) {
                    Log.e(tag, "Thermal limit reached during export ($currentThermal). Cancelling.")
                    handler.post {
                        transformer.cancel()
                        if (outputFile.exists()) outputFile.delete()
                    }
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("Device overheating"))
                    }
                    return
                }

                val progressState = transformer.getProgress(progressHolder)
                if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress.coerceIn(0, 100))
                }

                handler.postDelayed(this, 150)
            }
        }

        continuation.invokeOnCancellation {
            handler.post {
                handler.removeCallbacks(progressRunnable)
                transformer.cancel()
                if (outputFile.exists()) outputFile.delete()
            }
        }

        transformer.start(composition, outputFile.absolutePath)
        handler.post(progressRunnable)
    }

    private fun saveToMediaStore(file: File, filename: String): Uri? {
        var uri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Lensora_Ai")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return null

            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                FileInputStream(file).use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Failed to open MediaStore output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }

            Log.i(tag, "Saved export to MediaStore Movies/Lensora_Ai: $uri")
            uri
        } catch (e: Exception) {
            Log.e(tag, "Failed to save export to MediaStore", e)
            if (uri != null) {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (cleanupEx: Exception) {
                    Log.w(tag, "Failed to cleanup pending MediaStore row", cleanupEx)
                }
            }
            null
        }
    }

    private fun createForegroundInfo(projectName: String): ForegroundInfo {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notification shown while exporting Lensora video projects"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Exporting Video")
            .setContentText("Rendering $projectName...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
