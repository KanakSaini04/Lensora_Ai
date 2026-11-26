package com.lensora.ai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lensora.ai.R
import com.lensora.ai.ai.CloudSyncWorker
import com.lensora.ai.ai.ModelManager
import com.lensora.ai.ai.Validator
import com.lensora.ai.ui.studio.ImageEditor
import com.lensora.ai.ui.vibes.VibePreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class AiJobRequest(
    val jobId: String,
    val photoUriStr: String,
    val sceneTypeStr: String,
    val evOffset: Float,
    val vibePreset: VibePreset,
    val isHighRes: Boolean,
    val downscaleFactor: Float,
    val forceCpu: Boolean,
)

class AiProcessorService : Service() {

    private val tag = "AiProcessorService"
    private val channelId = "ai_processing_channel"
    private val notificationId = 1001

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val validator = Validator()
    private lateinit var modelManager: ModelManager

    private val jobQueue = Channel<AiJobRequest>(Channel.UNLIMITED)
    private val pendingJobsMap = ConcurrentHashMap<String, AiJobRequest>()
    private var isQueueLoopStarted = false

    private var currentProcessingJob: Job? = null
    @Volatile private var activeJobId: String? = null
    @Volatile private var activePendingUri: Uri? = null
    @Volatile private var isCancelRequested = false

    companion object {
        const val ACTION_CANCEL_JOB = "com.lensora.ai.CANCEL_AI_JOB"
        const val EXTRA_JOB_ID = "JOB_ID"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        modelManager = ModelManager(this)
        startQueueProcessorLoop()
    }

    private fun startQueueProcessorLoop() {
        if (isQueueLoopStarted) return
        isQueueLoopStarted = true

        serviceScope.launch {
            for (jobRequest in jobQueue) {
                pendingJobsMap.remove(jobRequest.jobId)
                isCancelRequested = false
                activeJobId = jobRequest.jobId
                activePendingUri = null

                val remainingInQueue = pendingJobsMap.size
                val queueText = if (remainingInQueue > 0) " ($remainingInQueue waiting in queue)" else ""
                updateProgressNotification(
                    title = "✨ Lensora AI Super-Res (Processing...)",
                    text = "Starting upscaling$queueText...",
                    progressPercent = 0,
                )

                val job = serviceScope.launch {
                    try {
                        executeRealImageEnhancementPipeline(jobRequest, remainingInQueue)
                    } catch (e: Exception) {
                        if (e is CancellationException || isCancelRequested) {
                            Log.i(tag, "Job ${jobRequest.jobId} cancelled cleanly. Cleaning up partial outputs.")
                        } else {
                            Log.e(tag, "Unhandled exception in AiProcessorService image enhancement pass", e)
                        }
                    }
                }
                currentProcessingJob = job
                job.join()
                currentProcessingJob = null
                activeJobId = null

                if (pendingJobsMap.isEmpty()) {
                    cleanupAndStop()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (pendingJobsMap.isEmpty()) stopSelf()
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_CANCEL_JOB) {
            Log.i(tag, "Received ACTION_CANCEL_JOB request for active job $activeJobId")
            isCancelRequested = true
            currentProcessingJob?.cancel()
            activePendingUri?.let { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                    Log.i(tag, "Deleted partial MediaStore entry $uri upon cancellation")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to delete partial MediaStore entry $uri", e)
                }
                activePendingUri = null
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Processing cancelled. Original photo kept.", Toast.LENGTH_SHORT).show()
            }
            postCompletionNotification(
                title = "Processing Cancelled",
                text = "AI upscaling cancelled. Original photo saved in Gallery.",
                contentUri = null,
            )
            return START_NOT_STICKY
        }

        val photoUriStr = intent.getStringExtra("PHOTO_URI")
        val sceneTypeStr = intent.getStringExtra("SCENE_TYPE") ?: "AUTO"
        val evOffset = intent.getFloatExtra("EV_OFFSET", 0.0f)
        val vibeStr = intent.getStringExtra("VIBE_PRESET") ?: "NONE"
        val vibePreset = try { VibePreset.valueOf(vibeStr) } catch (e: Exception) { VibePreset.NONE }
        val isHighRes = intent.getBooleanExtra("IS_HIGH_RES", false)
        val downscaleFactor = intent.getFloatExtra("DOWNSCALE_FACTOR", 0.5f)
        val forceCpu = intent.getBooleanExtra("FORCE_CPU", false)

        if (photoUriStr.isNullOrEmpty()) {
            Log.w(tag, "Null or empty PHOTO_URI passed to AiProcessorService.")
            if (pendingJobsMap.isEmpty()) stopSelf()
            return START_NOT_STICKY
        }

        val jobId = "job_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val jobRequest = AiJobRequest(
            jobId = jobId,
            photoUriStr = photoUriStr,
            sceneTypeStr = sceneTypeStr,
            evOffset = evOffset,
            vibePreset = vibePreset,
            isHighRes = isHighRes,
            downscaleFactor = downscaleFactor,
            forceCpu = forceCpu,
        )

        pendingJobsMap[jobId] = jobRequest

        // Persistent Foreground Notification ("✨ AI: Processing...")
        val queueCount = pendingJobsMap.size - 1
        val initialText = if (queueCount > 0) "Queued ($queueCount pending)..." else "✨ AI: Perfecting your photo..."
        updateProgressNotification("✨ Lensora AI Processing", initialText, 0)

        val notification = createNotificationBuilder("✨ Lensora AI Processing", initialText, 0).build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    notificationId,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else { 0 },
                )
            } else {
                startForeground(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting foreground service notification", e)
        }

        jobQueue.trySend(jobRequest)
        Log.i(tag, "Enqueued AiJobRequest $jobId for $photoUriStr [Pending in queue: ${pendingJobsMap.size}]")

        return START_NOT_STICKY
    }

    private fun executeRealImageEnhancementPipeline(
        jobRequest: AiJobRequest,
        remainingInQueue: Int,
    ) {
        val photoUriStr = jobRequest.photoUriStr
        val sceneTypeStr = jobRequest.sceneTypeStr
        val evOffset = jobRequest.evOffset
        val vibePreset = jobRequest.vibePreset
        val isHighRes = jobRequest.isHighRes
        val downscaleFactor = jobRequest.downscaleFactor
        val forceCpu = jobRequest.forceCpu

        Log.i(tag, "Executing L3 Real Image-to-Image AI Enhancement & LUT Pipeline for $photoUriStr [Job: ${jobRequest.jobId}]")
        val startTime = System.currentTimeMillis()
        var processedBitmap: Bitmap? = null

        var esrganInterpreterObj: com.lensora.ai.ai.EsrganInterpreter? = null
        var usedImageToImageModel = false

        try {
            val inputUri = Uri.parse(photoUriStr)
            val resolver = contentResolver

            // 1. Memory Shield: BitmapRegionDecoder
            var imgWidth = 0
            var imgHeight = 0

            val decoder: BitmapRegionDecoder? = try {
                resolver.openInputStream(inputUri)?.use { inputStream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        BitmapRegionDecoder.newInstance(inputStream)
                    } else {
                        @Suppress("DEPRECATION")
                        BitmapRegionDecoder.newInstance(inputStream, false)
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "BitmapRegionDecoder initialization failed for $photoUriStr: ${e.localizedMessage}")
                null
            }

            if (decoder != null) {
                imgWidth = decoder.width
                imgHeight = decoder.height
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(inputUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, boundsOpts)
                }
                imgWidth = boundsOpts.outWidth
                imgHeight = boundsOpts.outHeight
            }

            if (imgWidth <= 0 || imgHeight <= 0) {
                Log.w(tag, "Invalid image dimensions ($imgWidth x $imgHeight) for $photoUriStr")
                return
            }

            // 2. Select Real Image-to-Image AI Enhancement Model ONLY when isHighRes is true
            var enhancementModelFile: File? = null
            if (isHighRes) {
                enhancementModelFile = modelManager.getEnhancementModelFile()
                if (enhancementModelFile != null && enhancementModelFile.exists()) {
                    try {
                        val candidateObj = com.lensora.ai.ai.RealEsrganEngine.createInterpreter(enhancementModelFile, forceCpu)
                        val provider = com.lensora.ai.ai.InterpreterTensorInfoProvider(candidateObj.interpreter)
                        val contractStatus = com.lensora.ai.ai.ModelContractChecker.checkContract(enhancementModelFile, com.lensora.ai.ai.ModelContracts.REAL_ESRGAN_V2, provider)
                        Log.i(tag, "ModelContractChecker Real-ESRGAN v2 Status: $contractStatus")
                        esrganInterpreterObj = candidateObj
                        usedImageToImageModel = true
                        Log.i(tag, "Loaded Image-to-Image AI Model: ${enhancementModelFile.name} (${enhancementModelFile.length()} bytes) [Backend: ${if (candidateObj.usingGpu) "GPU" else "CPU"}]")
                    } catch (e: Exception) {
                        Log.w(tag, "Failed to initialize TFLite Interpreter for ${enhancementModelFile.name}. Falling back to ColorMatrix engine.", e)
                    }
                } else {
                    Log.w(tag, "FALLBACK WARNING: Real-ESRGAN DLC model missing at filesDir/models/. Executing ColorMatrix fallback pass.")
                }
            }

            // 3. Prepare Master Target Bitmap & Paint with Stacked ColorMatrix / Vibe LUT
            val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)
            if (vibePreset != VibePreset.NONE) {
                val vibeMatrix = ImageEditor.createVibeAndroidColorMatrix(vibePreset)
                colorMatrix.postConcat(vibeMatrix)
                Log.i(tag, "Applied Vibe Preset ColorMatrix pass: ${vibePreset.displayName}")
            }

            val activeEsrganObj = esrganInterpreterObj
            if (isHighRes && usedImageToImageModel && activeEsrganObj != null) {
                val fullSourceBitmap = resolver.openInputStream(inputUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
                if (fullSourceBitmap != null) {
                    try {
                        Log.i(tag, "Executing Real-ESRGAN super-resolution pass in Service (downscaleFactor=$downscaleFactor) [Backend: ${if (activeEsrganObj.usingGpu) "GPU" else "CPU"}]")
                        processedBitmap = com.lensora.ai.ai.RealEsrganEngine.runSuperResolutionPipeline(
                            sourceBitmap = fullSourceBitmap,
                            downscaleFactor = downscaleFactor,
                            interpreter = activeEsrganObj.interpreter,
                            validator = validator,
                            colorMatrix = colorMatrix,
                            modelFile = enhancementModelFile,
                            esrganObj = activeEsrganObj,
                            onProgress = { progressPercent ->
                                if (isCancelRequested) {
                                    throw CancellationException("Job ${jobRequest.jobId} cancelled by user")
                                }
                                val queueText = if (remainingInQueue > 0) " ($remainingInQueue waiting)" else ""
                                updateProgressNotification(
                                    title = "✨ Lensora AI Super-Res (Processing...)",
                                    text = "Upscaling photo ($progressPercent% complete)$queueText",
                                    progressPercent = progressPercent,
                                )
                            },
                        )
                    } catch (oom: OutOfMemoryError) {
                        Log.w(tag, "OOM during 50% Real-ESRGAN pass in Service. Retrying at 25% scale.", oom)
                        processedBitmap?.recycle()
                        processedBitmap = null
                        try {
                            processedBitmap = com.lensora.ai.ai.RealEsrganEngine.runSuperResolutionPipeline(
                                sourceBitmap = fullSourceBitmap,
                                downscaleFactor = 0.25f,
                                interpreter = activeEsrganObj.interpreter,
                                validator = validator,
                                colorMatrix = colorMatrix,
                                modelFile = enhancementModelFile,
                                esrganObj = activeEsrganObj,
                                onProgress = { progressPercent ->
                                    if (isCancelRequested) {
                                        throw CancellationException("Job ${jobRequest.jobId} cancelled by user")
                                    }
                                    updateProgressNotification(
                                        title = "✨ Lensora AI Super-Res (Processing...)",
                                        text = "Upscaling photo ($progressPercent% complete)",
                                        progressPercent = progressPercent,
                                    )
                                },
                            )
                        } catch (retryOom: OutOfMemoryError) {
                            Log.e(tag, "OOM during 25% fallback pass in Service as well.", retryOom)
                            processedBitmap?.recycle()
                            processedBitmap = null
                        } catch (e: Exception) {
                            if (e is CancellationException || isCancelRequested) throw e
                            Log.w(tag, "Exception during 25% Real-ESRGAN fallback pass in Service.", e)
                            processedBitmap?.recycle()
                            processedBitmap = null
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException || isCancelRequested) throw e
                        Log.w(tag, "Exception during 50% Real-ESRGAN pass in Service. Falling back.", e)
                        processedBitmap?.recycle()
                        processedBitmap = null
                    } finally {
                        fullSourceBitmap.recycle()
                    }
                }
            }

            if (isCancelRequested) {
                throw CancellationException("Job ${jobRequest.jobId} cancelled before tile rendering")
            }

            if (processedBitmap == null) {
                processedBitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(processedBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                    isFilterBitmap = true
                }

                val tileSize = 512
                val numCols = (imgWidth + tileSize - 1) / tileSize
                val numRows = (imgHeight + tileSize - 1) / tileSize
                val totalTiles = numCols * numRows

                Log.i(tag, "Processing $imgWidth x $imgHeight px in $totalTiles tiles ($tileSize x $tileSize px each) [ImageToImage AI: $usedImageToImageModel]")

                var tileIndex = 0
                for (row in 0 until numRows) {
                    for (col in 0 until numCols) {
                        if (isCancelRequested) {
                            throw CancellationException("Job ${jobRequest.jobId} cancelled during tile rendering")
                        }
                        val left = col * tileSize
                        val top = row * tileSize
                        val right = (left + tileSize).coerceAtMost(imgWidth)
                        val bottom = (top + tileSize).coerceAtMost(imgHeight)
                        val rect = Rect(left, top, right, bottom)

                        val rawTileBitmap = decoder?.decodeRegion(rect, null)
                        if (rawTileBitmap != null) {
                            try {
                                canvas.drawBitmap(rawTileBitmap, left.toFloat(), top.toFloat(), paint)
                            } finally {
                                rawTileBitmap.recycle()
                            }
                        }

                        tileIndex++
                        val progressPercent = (tileIndex * 100) / totalTiles
                        updateProgressNotification(
                            title = "✨ Lensora AI Processing",
                            text = "Rendering tiles ($progressPercent% complete)",
                            progressPercent = progressPercent,
                        )
                    }
                }
            }

            if (isCancelRequested) {
                throw CancellationException("Job ${jobRequest.jobId} cancelled before saving")
            }

            // 4. L4 Quality Validation Check on Output
            if (!validator.isValid(processedBitmap)) {
                Log.w(tag, "L4 Validator failed: Processed bitmap invalid or pitch black. Keeping raw photo $photoUriStr")
                return
            }

            // 5. Save processed Bitmap to MediaStore
            val timestamp = System.currentTimeMillis()
            val fileName = "LENSORA_AI_PROCESSED_$timestamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Lensora_Ai")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val newUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            activePendingUri = newUri

            if (newUri != null) {
                resolver.openOutputStream(newUri)?.use { outStream ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)
                activePendingUri = null

                val executionTime = System.currentTimeMillis() - startTime
                Log.i(tag, "L3 Service Real Image-to-Image AI Processing finished in ${executionTime}ms (Model: ${enhancementModelFile?.name ?: "ColorMatrix"}, Backend: ${if (esrganInterpreterObj?.usingGpu == true) "GPU" else "CPU"}, Vibe: ${vibePreset.displayName}, Dimensions: ${processedBitmap?.width}x${processedBitmap?.height}). Saved $fileName to MediaStore at $newUri")

                // Explicit User Feedback: Toast + Persistent Completion Notification
                val isFallbackPass = isHighRes && (!usedImageToImageModel || enhancementModelFile == null || !enhancementModelFile.exists())
                val finalW = processedBitmap?.width ?: 0
                val finalH = processedBitmap?.height ?: 0

                if (isFallbackPass) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "⚠️ AI Super-Res Model Unavailable: Photo saved without AI upscaling",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    postCompletionNotification(
                        title = "⚠️ Lensora AI Photo Saved",
                        text = "⚠️ AI Model Missing: Photo saved without AI upscaling",
                        contentUri = newUri,
                    )
                } else if (isHighRes && usedImageToImageModel) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "✨ AI Super-Res Complete: 2x Net Scale ($finalW x $finalH)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    postCompletionNotification(
                        title = "✨ Lensora AI Super-Res Complete",
                        text = "✨ Photo upscaled 2x net scale ($finalW x $finalH)",
                        contentUri = newUri,
                    )
                }

                // 6. Enqueue CloudSyncWorker if Cloud Sync toggle is ON in Settings
                val prefs = getSharedPreferences("lensora_prefs", MODE_PRIVATE)
                val isCloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", false)
                val isAutoPurgeEnabled = prefs.getBoolean("auto_purge_enabled", true)

                if (isCloudSyncEnabled) {
                    val syncRequest = CloudSyncWorker.createWifiOnlySyncRequest(
                        photoUriStr = newUri.toString(),
                        fileName = fileName,
                        autoPurgeEnabled = isAutoPurgeEnabled,
                    )

                    WorkManager.getInstance(this).enqueue(syncRequest)
                    Log.i(tag, "Cloud Sync toggle is ON. Enqueued Wi-Fi only CloudSyncWorker for $fileName (Auto-Purge: $isAutoPurgeEnabled)")
                }
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for processed image in Service")
            }
        } catch (e: Exception) {
            if (e is CancellationException || isCancelRequested) {
                Log.i(tag, "Job ${jobRequest.jobId} cancelled. Partial file cleanup handled.")
                activePendingUri?.let { uri ->
                    try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                    activePendingUri = null
                }
            } else {
                Log.e(tag, "Error during AiProcessorService execution pass for ${jobRequest.jobId}", e)
                activePendingUri?.let { uri ->
                    try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                    activePendingUri = null
                }
            }
        } finally {
            try {
                esrganInterpreterObj?.close()
                modelManager.close()
                Log.i(tag, "TFLite Interpreter resources safely closed")
            } catch (cleanupEx: Exception) {
                Log.w(tag, "Error releasing TFLite/GPU resources", cleanupEx)
            }
            processedBitmap?.recycle()
        }
    }

    private fun updateProgressNotification(title: String, text: String, progressPercent: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notification = createNotificationBuilder(title, text, progressPercent).build()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationBuilder(title: String, text: String, progressPercent: Int): NotificationCompat.Builder {
        val cancelIntent = Intent(this, AiProcessorService::class.java).apply {
            action = ACTION_CANCEL_JOB
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel", cancelPendingIntent)
    }

    private fun postCompletionNotification(title: String, text: String, contentUri: Uri?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val completionChannelId = "ai_processing_completion_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                completionChannelId,
                "Lensora AI Completion",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Completion notifications for AI photo processing"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, completionChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (contentUri != null) {
            val contentIntent = Intent(this, com.lensora.ai.MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(contentUri, "image/jpeg")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("OPEN_URI", contentUri.toString())
            }
            val pendingContentIntent = PendingIntent.getActivity(
                this,
                contentUri.hashCode(),
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingContentIntent)
        }

        notificationManager.notify(1002, builder.build())
    }

    private fun createColorMatrixForPlan(sceneType: String, evOffset: Float): ColorMatrix {
        val matrix = ColorMatrix()

        val evScale = 1.0f + (evOffset * 0.20f)
        val contrastScale = when (sceneType) {
            "PORTRAIT" -> 1.10f
            "LANDSCAPE" -> 1.15f
            "NIGHT" -> 1.25f
            "SKY" -> 1.10f
            else -> 1.05f
        }

        val combinedScale = contrastScale * evScale
        val translateOffset = 128f * (1f - contrastScale) + (evOffset * 15f)

        val floatArray = floatArrayOf(
            combinedScale, 0f, 0f, 0f, translateOffset,
            0f, combinedScale, 0f, 0f, translateOffset,
            0f, 0f, combinedScale, 0f, translateOffset,
            0f, 0f, 0f, 1f, 0f,
        )

        matrix.set(floatArray)
        return matrix
    }

    private fun cleanupAndStop() {
        Log.i(tag, "Cleaning up AiProcessorService and dismissing notification.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lensora AI Processing",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Silent foreground notification for L3 AI photo processing pipeline"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(tag, "AiProcessorService destroyed")
    }
}
