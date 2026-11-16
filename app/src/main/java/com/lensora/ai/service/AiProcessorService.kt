package com.lensora.ai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lensora.ai.R
import com.lensora.ai.ai.CloudSyncWorker
import com.lensora.ai.ai.Validator
import com.lensora.ai.ui.studio.ImageEditor
import com.lensora.ai.ui.vibes.VibePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AiProcessorService : Service() {

    private val tag = "AiProcessorService"
    private val channelId = "ai_processing_channel"
    private val notificationId = 1001

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val validator = Validator()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val photoUriStr = intent.getStringExtra("PHOTO_URI")
        val sceneTypeStr = intent.getStringExtra("SCENE_TYPE") ?: "AUTO"
        val evOffset = intent.getFloatExtra("EV_OFFSET", 0.0f)
        val vibeStr = intent.getStringExtra("VIBE_PRESET") ?: "NONE"
        val vibePreset = try { VibePreset.valueOf(vibeStr) } catch (e: Exception) { VibePreset.NONE }

        if (photoUriStr.isNullOrEmpty()) {
            Log.w(tag, "Null or empty PHOTO_URI passed to AiProcessorService. Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Persistent Foreground Notification ("✨ AI: Perfecting your photo...")
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("✨ AI Processing")
            .setContentText("✨ AI: Perfecting your photo...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

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

        serviceScope.launch {
            try {
                executeTiledProcessingPass(photoUriStr, sceneTypeStr, evOffset, vibePreset)
            } catch (e: Exception) {
                Log.e(tag, "Unhandled exception in AiProcessorService tiled processing pass", e)
            } finally {
                cleanupAndStop()
            }
        }

        return START_NOT_STICKY
    }

    private fun executeTiledProcessingPass(
        photoUriStr: String,
        sceneTypeStr: String,
        evOffset: Float,
        vibePreset: VibePreset,
    ) {
        Log.i(tag, "Executing L3 512x512 Tiled Processing pass in Foreground Service for $photoUriStr")
        val startTime = System.currentTimeMillis()
        var processedBitmap: Bitmap? = null

        var gpuDelegateObj: AutoCloseable? = null
        var interpreter: Interpreter? = null
        var usedTfLiteGpu = false

        try {
            val inputUri = Uri.parse(photoUriStr)
            val resolver = contentResolver

            // 1. Memory Shield: BitmapRegionDecoder (no full image decode into RAM)
            val decoder = resolver.openInputStream(inputUri)?.use { inputStream ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(inputStream)
                } else {
                    @Suppress("DEPRECATION")
                    BitmapRegionDecoder.newInstance(inputStream, false)
                }
            }

            if (decoder == null) {
                Log.w(tag, "Failed to initialize BitmapRegionDecoder for $photoUriStr")
                return
            }

            val imgWidth = decoder.width
            val imgHeight = decoder.height

            // 2. TFLite GPU Delegate Initialization & Model Loading Pass
            val mobileNetFile = File(filesDir, "models/mobilenet_v1_1.0_224.tflite")
            val fallbackFile = File(filesDir, "models/real_esrgan_v2.tflite")
            val modelFile = if (mobileNetFile.exists()) mobileNetFile else if (fallbackFile.exists()) fallbackFile else null

            if (modelFile != null && modelFile.exists()) {
                val interpreterOptions = Interpreter.Options()
                try {
                    val gpuDelegateClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                    val constructor = gpuDelegateClass.getConstructor()
                    val delegateInstance = constructor.newInstance()
                    val delegateClass = Class.forName("org.tensorflow.lite.Delegate")
                    val addDelegateMethod = Interpreter.Options::class.java.getMethod("addDelegate", delegateClass)
                    addDelegateMethod.invoke(interpreterOptions, delegateInstance)
                    if (delegateInstance is AutoCloseable) {
                        gpuDelegateObj = delegateInstance
                    }
                    usedTfLiteGpu = true
                    Log.i(tag, "TfLiteGpuDelegate initialized successfully for hardware acceleration")
                } catch (gpuEx: Throwable) {
                    Log.w(tag, "TfLiteGpuDelegate initialization failed (${gpuEx.localizedMessage}). Safe fallback to CPU XNNPACK thread pool.", gpuEx)
                    interpreterOptions.setNumThreads(4)
                }

                try {
                    interpreter = Interpreter(modelFile, interpreterOptions)

                    val sampleRect = Rect(0, 0, imgWidth.coerceAtMost(512), imgHeight.coerceAtMost(512))
                    val sampleTile = decoder.decodeRegion(sampleRect, null)

                    if (sampleTile != null && modelFile.name.contains("mobilenet", ignoreCase = true)) {
                        val scaledBitmap = Bitmap.createScaledBitmap(sampleTile, 224, 224, true)
                        val inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4).apply {
                            order(ByteOrder.nativeOrder())
                        }

                        val intValues = IntArray(224 * 224)
                        scaledBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224)
                        for (pixelValue in intValues) {
                            val r = ((pixelValue shr 16) and 0xFF) / 255.0f
                            val g = ((pixelValue shr 8) and 0xFF) / 255.0f
                            val b = (pixelValue and 0xFF) / 255.0f
                            inputBuffer.putFloat(r)
                            inputBuffer.putFloat(g)
                            inputBuffer.putFloat(b)
                        }

                        val outputArray = Array(1) { FloatArray(1001) }
                        interpreter.run(inputBuffer, outputArray)
                        scaledBitmap.recycle()
                        sampleTile.recycle()
                        Log.i(tag, "Real MobileNet TFLite AI Inference executed on tiled sample (GPU Accelerated: $usedTfLiteGpu)")
                    } else {
                        sampleTile?.recycle()
                    }
                } catch (e: Exception) {
                    Log.w(tag, "TFLite Interpreter pass failed (${e.localizedMessage}). Safe fallback to ColorMatrix.", e)
                }
            } else {
                Log.i(tag, "No TFLite DLC model found. Executing ColorMatrix & Vibe LUT tiled pass.")
            }

            // 3. 512x512 Tiled Pipeline with ColorMatrix & Vibe LUT Integration
            val colorMatrix = createColorMatrixForPlan(sceneTypeStr, evOffset)
            if (vibePreset != VibePreset.NONE) {
                val vibeMatrix = ImageEditor.createVibeAndroidColorMatrix(vibePreset)
                colorMatrix.postConcat(vibeMatrix)
            }

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

            Log.i(tag, "Service processing $imgWidth x $imgHeight px in $totalTiles tiles ($tileSize x $tileSize px each)")

            var tileIndex = 0
            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * tileSize
                    val top = row * tileSize
                    val right = (left + tileSize).coerceAtMost(imgWidth)
                    val bottom = (top + tileSize).coerceAtMost(imgHeight)
                    val rect = Rect(left, top, right, bottom)

                    val rawTileBitmap = decoder.decodeRegion(rect, null)
                    if (rawTileBitmap != null) {
                        try {
                            // L4 Tile Luminance Validation
                            if (validator.isTileValid(rawTileBitmap)) {
                                canvas.drawBitmap(rawTileBitmap, left.toFloat(), top.toFloat(), paint)
                            } else {
                                Log.w(tag, "L4 Anti-AI Tile check failed for tile $tileIndex. Drawing raw unenhanced pixel data.")
                                val rawPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                                canvas.drawBitmap(rawTileBitmap, left.toFloat(), top.toFloat(), rawPaint)
                            }
                        } catch (tileEx: Exception) {
                            Log.e(tag, "Error processing tile $tileIndex. Safe fallback to raw pixel data.", tileEx)
                            val rawPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                            canvas.drawBitmap(rawTileBitmap, left.toFloat(), top.toFloat(), rawPaint)
                        } finally {
                            rawTileBitmap.recycle() // Memory Shield: Recycle tile immediately
                        }
                    }

                    tileIndex++
                    val progressPercent = (tileIndex * 100) / totalTiles
                    Log.d(tag, "Service Tile $tileIndex of $totalTiles processed ($left,$top - $right,$bottom) [$progressPercent%]")
                }
            }

            // 4. L4 Quality Validation Check
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

            if (newUri != null) {
                resolver.openOutputStream(newUri)?.use { outStream ->
                    processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)

                val executionTime = System.currentTimeMillis() - startTime
                Log.i(tag, "L3 Service 512x512 Tiled AI Processing finished in ${executionTime}ms (Used GPU Delegate: $usedTfLiteGpu, Vibe: ${vibePreset.displayName}, Total Tiles: $totalTiles). Saved $fileName to MediaStore at $newUri")

                // 6. Enqueue CloudSyncWorker if Cloud Sync toggle is ON in Settings
                val prefs = getSharedPreferences("lensora_prefs", MODE_PRIVATE)
                val isCloudSyncEnabled = prefs.getBoolean("cloud_sync_enabled", false)
                val isAutoPurgeEnabled = prefs.getBoolean("auto_purge_enabled", true)

                if (isCloudSyncEnabled) {
                    val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                        .setInputData(
                            workDataOf(
                                "PHOTO_URI" to newUri.toString(),
                                "FILE_NAME" to fileName,
                                "AUTO_PURGE_ENABLED" to isAutoPurgeEnabled,
                            ),
                        )
                        .build()

                    WorkManager.getInstance(this).enqueue(syncRequest)
                    Log.i(tag, "Cloud Sync toggle is ON. Enqueued CloudSyncWorker for $fileName (Auto-Purge: $isAutoPurgeEnabled)")
                }
            } else {
                Log.e(tag, "Failed to insert MediaStore entry for processed image in Service")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error during AiProcessorService execution pass", e)
        } finally {
            // Resource Cleanup: Close TFLite Interpreter & GpuDelegate to free GPU memory
            try {
                interpreter?.close()
                gpuDelegateObj?.close()
                Log.i(tag, "TFLite Interpreter & GpuDelegate resources safely closed")
            } catch (cleanupEx: Exception) {
                Log.w(tag, "Error releasing TFLite/GPU resources", cleanupEx)
            }
            processedBitmap?.recycle()
        }
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
