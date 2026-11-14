package com.lensora.ai.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import kotlin.math.abs

class CameraController(
    private val context: Context,
) {
    private val tag = "CameraController"

    enum class FlashMode {
        OFF,
        ON,
        AUTO
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewSurface: Surface? = null
    private var cameraId: String? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var sensorOrientation: Int = 90
    private var cameraCharacteristics: CameraCharacteristics? = null

    private var supportsZoomRatioControl: Boolean = false
    private var activeSensorArraySize: Rect? = null
    private var minZoomRatio: Float = 1.0f
    private var maxZoomRatio: Float = 10.0f
    private var currentZoomRatio: Float = 1.0f

    private var currentFlashMode: FlashMode = FlashMode.AUTO
    private var currentCameraMode: CameraMode = CameraMode.PHOTO

    private var powerManager: PowerManager? = null
    private var thermalStatusListener: PowerManager.OnThermalStatusChangedListener? = null
    private var currentThermalStatus: Int = PowerManager.THERMAL_STATUS_NONE

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            val thread = HandlerThread("CameraBackground").apply { start() }
            backgroundThread = thread
            backgroundHandler = Handler(thread.looper)
        }
    }

    fun stopBackgroundThread() {
        try {
            backgroundThread?.quitSafely()
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            Log.e(tag, "Error stopping background thread", e)
        }
    }

    private fun initThermalGuard() {
        try {
            powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val listener = PowerManager.OnThermalStatusChangedListener { status ->
                    currentThermalStatus = status
                    val statusStr = when (status) {
                        PowerManager.THERMAL_STATUS_NONE -> "NONE"
                        PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                        PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                        PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                        PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                        PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                        PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                        else -> "UNKNOWN ($status)"
                    }
                    if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                        Log.w(tag, "Thermal Guard WARNING: Status changed to $statusStr")
                    } else {
                        Log.i(tag, "Thermal Guard Status: $statusStr")
                    }
                }
                thermalStatusListener = listener
                val executor = ContextCompat.getMainExecutor(context)
                powerManager?.addThermalStatusListener(executor, listener)
                currentThermalStatus = powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
                Log.i(tag, "Thermal Guard initialized. Current status code: $currentThermalStatus")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing Thermal Guard", e)
        }
    }

    private fun closeThermalGuard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thermalStatusListener?.let {
                    powerManager?.removeThermalStatusListener(it)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error closing Thermal Guard", e)
        }
    }

    fun checkAvailableStorageBytes(): Long {
        return try {
            val statFs = StatFs(context.cacheDir.absolutePath)
            statFs.availableBytes
        } catch (e: Exception) {
            Log.e(tag, "Error checking storage capacity", e)
            Long.MAX_VALUE
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        startBackgroundThread()
        initThermalGuard()
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return

            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }

            val currentCameraId = cameraId ?: cameraManager.cameraIdList.firstOrNull() ?: return
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            cameraCharacteristics = characteristics

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

            val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            supportsZoomRatioControl = zoomRange != null
            minZoomRatio = zoomRange?.lower ?: 1.0f
            maxZoomRatio = zoomRange?.upper
                ?: (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 10.0f)
            activeSensorArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return

            val jpegSizes = map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            val captureSize = jpegSizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

            imageReader = ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                ImageFormat.JPEG,
                2,
            )

            val previewSizes = map.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()
            val previewSize = chooseOptimalPreviewSize(previewSizes, width, height)

            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewSurface = Surface(surfaceTexture)

            cameraManager.openCamera(currentCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(tag, "Error opening camera", e)
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val device = cameraDevice ?: return
            val surface = previewSurface ?: return
            val reader = imageReader ?: return
            val handler = backgroundHandler ?: return

            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            builder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            )
            applyZoom(builder, currentZoomRatio)
            applyFlashMode(builder, currentFlashMode)
            previewRequestBuilder = builder

            val outputs = listOf(
                OutputConfiguration(surface),
                OutputConfiguration(reader.surface),
            )

            val executor = Executor { command -> handler.post(command) }

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                executor,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session
                        try {
                            builder.set(
                                CaptureRequest.CONTROL_MODE,
                                CaptureRequest.CONTROL_MODE_AUTO,
                            )
                            session.setRepeatingRequest(
                                builder.build(),
                                null,
                                backgroundHandler,
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error starting preview", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(tag, "Capture session configuration failed")
                    }
                },
            )

            device.createCaptureSession(sessionConfig)
        } catch (e: Exception) {
            Log.e(tag, "Error creating capture session", e)
        }
    }

    fun setCameraMode(mode: CameraMode) {
        currentCameraMode = mode
        Log.i(tag, "Camera mode changed to: ${mode.displayName} (${mode.name})")

        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return

        try {
            when (mode) {
                CameraMode.AI_AUTO -> {
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
                }
                CameraMode.PHOTO -> {
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                }
                CameraMode.PORTRAIT -> {
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT)
                }
                CameraMode.VIDEO -> {
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                }
                CameraMode.PRO -> {
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                }
            }
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(tag, "Error setting camera mode ${mode.name}", e)
        }
    }

    fun setZoomRatio(ratio: Float) {
        try {
            val builder = previewRequestBuilder ?: return
            val session = captureSession ?: return

            applyZoom(builder, ratio)
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(tag, "Error setting zoom ratio", e)
        }
    }

    private fun applyZoom(builder: CaptureRequest.Builder, ratio: Float) {
        try {
            val clampedRatio = ratio.coerceIn(minZoomRatio, maxZoomRatio)
            currentZoomRatio = clampedRatio

            if (supportsZoomRatioControl) {
                builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clampedRatio)
            } else {
                val sensorRect = activeSensorArraySize ?: return
                if (clampedRatio > 1.0f) {
                    val xCenter = sensorRect.width() / 2
                    val yCenter = sensorRect.height() / 2
                    val xDelta = (sensorRect.width() / (2 * clampedRatio)).toInt()
                    val yDelta = (sensorRect.height() / (2 * clampedRatio)).toInt()

                    val cropRect = Rect(
                        xCenter - xDelta,
                        yCenter - yDelta,
                        xCenter + xDelta,
                        yCenter + yDelta,
                    )
                    builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
                } else {
                    builder.set(CaptureRequest.SCALER_CROP_REGION, sensorRect)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error applying zoom ratio", e)
        }
    }

    fun setFlashMode(mode: FlashMode) {
        currentFlashMode = mode
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return

        applyFlashMode(builder, mode)
        try {
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(tag, "Error setting flash mode", e)
        }
    }

    private fun applyFlashMode(builder: CaptureRequest.Builder, mode: FlashMode) {
        try {
            when (mode) {
                FlashMode.OFF -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
                FlashMode.ON -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                    builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
                }
                FlashMode.AUTO -> {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error applying flash mode", e)
        }
    }

    fun triggerTapToFocus(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        try {
            val builder = previewRequestBuilder ?: return
            val session = captureSession ?: return

            if (viewWidth <= 0 || viewHeight <= 0) return

            val sensorRect = activeSensorArraySize
            if (sensorRect != null) {
                val halfTouchWidth = 150
                val halfTouchHeight = 150

                val sensorX = (x / viewWidth * sensorRect.width()).toInt()
                val sensorY = (y / viewHeight * sensorRect.height()).toInt()

                val left = (sensorX - halfTouchWidth).coerceIn(0, sensorRect.width() - 1)
                val top = (sensorY - halfTouchHeight).coerceIn(0, sensorRect.height() - 1)
                val right = (sensorX + halfTouchWidth).coerceIn(0, sensorRect.width() - 1)
                val bottom = (sensorY + halfTouchHeight).coerceIn(0, sensorRect.height() - 1)

                val afRect = MeteringRectangle(
                    Rect(left, top, right, bottom),
                    MeteringRectangle.METERING_WEIGHT_MAX,
                )

                val maxAfRegions = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0
                if (maxAfRegions > 0) {
                    builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(afRect))
                }
            }

            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)

            session.capture(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult,
                ) {
                    try {
                        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
                        session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(tag, "Error resetting AF trigger", e)
                    }
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(tag, "Error triggering tap to focus", e)
        }
    }

    fun takePicture(
        displayRotationDegrees: Int,
        onPhotoSaved: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            // Storage Guard: Check for minimum 2GB free storage space
            val minRequiredStorageBytes = 2L * 1024L * 1024L * 1024L // 2GB
            val freeBytes = checkAvailableStorageBytes()
            if (freeBytes < minRequiredStorageBytes) {
                val freeMb = freeBytes / (1024 * 1024)
                Log.w(tag, "Storage Guard triggered: Available space $freeMb MB is less than 2GB limit")
                onError("Storage Low: ${freeMb}MB free. Minimum 2GB space required to capture.")
                return
            }

            val device = cameraDevice ?: run {
                onError("Camera device unavailable")
                return
            }
            val session = captureSession ?: run {
                onError("Capture session unavailable")
                return
            }
            val reader = imageReader ?: run {
                onError("ImageReader unavailable")
                return
            }

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            )
            applyZoom(captureBuilder, currentZoomRatio)
            applyFlashMode(captureBuilder, currentFlashMode)

            // Calculate correct JPEG orientation
            val jpegOrientation = (sensorOrientation - displayRotationDegrees + 360) % 360
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)

            reader.setOnImageAvailableListener({ imageReader ->
                backgroundHandler?.post {
                    val image = imageReader.acquireNextImage() ?: return@post
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        val timestamp = System.currentTimeMillis()
                        val fileName = "LENSORA_$timestamp.jpg"
                        val tmpFile = File(context.cacheDir, "$fileName.tmp")

                        FileOutputStream(tmpFile).use { out ->
                            out.write(bytes)
                            out.flush()
                        }

                        // MediaStore Atomic Save
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                "Pictures/Lensora_Ai",
                            )
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                        val resolver = context.contentResolver
                        val uri = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues,
                        )

                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { outStream ->
                                tmpFile.inputStream().use { inputStream ->
                                    inputStream.copyTo(outStream)
                                }
                            }

                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)

                            tmpFile.delete()
                            onPhotoSaved(uri, "Saved: Pictures/Lensora_Ai/$fileName")
                        } else {
                            onError("Failed to insert MediaStore entry")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error saving picture", e)
                        onError("Save error: ${e.localizedMessage}")
                    } finally {
                        image.close()
                    }
                }
            }, backgroundHandler)

            session.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        try {
                            previewRequestBuilder?.let {
                                session.setRepeatingRequest(it.build(), null, backgroundHandler)
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Error resetting preview", e)
                        }
                    }
                },
                backgroundHandler,
            )
        } catch (e: Exception) {
            Log.e(tag, "Error taking picture", e)
            onError("Capture error: ${e.localizedMessage}")
        }
    }

    fun closeCamera() {
        try {
            closeThermalGuard()

            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            previewSurface?.release()
            previewSurface = null

            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(tag, "Error closing camera", e)
        }
    }

    private fun chooseOptimalPreviewSize(choices: Array<Size>, width: Int, height: Int): Size {
        val targetRatio = if (width > 0 && height > 0) height.toDouble() / width.toDouble() else 16.0 / 9.0
        return choices.filter {
            val ratio = it.width.toDouble() / it.height.toDouble()
            abs(ratio - targetRatio) < 0.2
        }.maxByOrNull { it.width * it.height }
            ?: choices.maxByOrNull { it.width * it.height }
            ?: Size(1920, 1080)
    }
}
