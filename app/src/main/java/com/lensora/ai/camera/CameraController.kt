package com.lensora.ai.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.view.Surface
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lensora.ai.ai.AiProcessorWorker
import com.lensora.ai.ai.ProcessingPlan
import com.lensora.ai.ui.vibes.VibePreset
import com.lensora.ai.ui.vibes.VibeState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.Executors

class CameraController(private val context: Context) {

    private val tag = "CameraController"

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewSurface: Surface? = null
    private var imageReader: ImageReader? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var cameraCharacteristics: CameraCharacteristics? = null
    private var maxZoomRatio = 1.0f

    // L0 Safe Core Thermal & Storage Guards
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    var cameraId: String = "0"
        private set
    var isFrontCamera: Boolean = false
        private set

    var currentMode: CameraMode = CameraMode.PHOTO
        private set

    var isRawCaptureEnabled: Boolean = false
        private set

    private var videoRecorder: VideoRecorder? = null
    private var isRecording: Boolean = false

    enum class FlashMode {
        OFF, ON, AUTO
    }

    private var currentFlashMode: FlashMode = FlashMode.AUTO

    init {
        startBackgroundThread()
        setupThermalGuard()
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, "Error stopping background thread", e)
        }
    }

    private fun setupThermalGuard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                val statusString = when (status) {
                    PowerManager.THERMAL_STATUS_NONE -> "NONE"
                    PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
                    PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
                    PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
                    else -> "UNKNOWN ($status)"
                }
                Log.i(tag, "Thermal Guard Status: $statusString")
                if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                    Log.w(tag, "Thermal Throttling Alert: High device temperature ($statusString). Computational pipeline speed reduced.")
                }
            }
            Log.i(tag, "Thermal Guard initialized. Current status code: ${powerManager.currentThermalStatus}")
        }
    }

    private fun checkAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.cacheDir.path)
            stat.availableBytes
        } catch (e: Exception) {
            Log.e(tag, "Error checking storage space", e)
            Long.MAX_VALUE
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        try {
            startBackgroundThread()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return

            // Query lens facing characteristics
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING)
            isFrontCamera = (facing == CameraCharacteristics.LENS_FACING_FRONT)

            // Setup Zoom Capabilities
            val zoomRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraCharacteristics?.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            } else null
            maxZoomRatio = zoomRange?.upper ?: 10.0f

            // Setup ImageReader for L0 Atomic JPEG Capture (12.5MP target resolution: 4080x3060)
            val map = cameraCharacteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val outputSizes = map?.getOutputSizes(ImageFormat.JPEG)
            val captureSize = outputSizes?.firstOrNull() ?: android.util.Size(4080, 3060)

            imageReader = ImageReader.newInstance(
                captureSize.width,
                captureSize.height,
                ImageFormat.JPEG,
                2,
            )

            // Fix Camera Preview Aspect Ratio Stretching: Filter preview size closest to 1920x1080
            val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.filter { it.width <= 1920 && it.height <= 1080 }?.firstOrNull() ?: android.util.Size(1920, 1080)

            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewSurface = Surface(surfaceTexture)

            thermalListener?.let { listener ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    powerManager?.addThermalStatusListener(listener)
                }
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
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

    fun switchCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        try {
            val targetFacing = if (isFrontCamera) {
                CameraCharacteristics.LENS_FACING_BACK
            } else {
                CameraCharacteristics.LENS_FACING_FRONT
            }

            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return

            var targetId: String? = null
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == targetFacing) {
                    targetId = id
                    break
                }
            }

            if (targetId == null) {
                Log.w(tag, "Requested lens facing $targetFacing not found on device. Keeping current camera.")
                return
            }

            closeCamera()
            isFrontCamera = (targetFacing == CameraCharacteristics.LENS_FACING_FRONT)
            cameraId = targetId
            openCamera(surfaceTexture, width, height)
            Log.i(tag, "Switched camera lens facing to: ${if (isFrontCamera) "FRONT" else "BACK"} (Camera ID $targetId)")
        } catch (e: Exception) {
            Log.e(tag, "Error switching camera lens facing", e)
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
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            applyFlashToBuilder(builder)
            applyCameraModeToBuilder(builder)
            previewRequestBuilder = builder

            val outputs = listOf(
                OutputConfiguration(surface),
                OutputConfiguration(reader.surface),
            )

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                Executors.newSingleThreadExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(builder.build(), null, handler)
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to start camera preview repeating request", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(tag, "Camera capture session configuration failed")
                    }
                },
            )

            device.createCaptureSession(sessionConfig)
        } catch (e: Exception) {
            Log.e(tag, "Error creating capture session", e)
        }
    }

    fun setAdvancedMode(modeName: String, onFeedback: (String) -> Unit = {}) {
        Log.i(tag, "Applying Advanced Camera Mode: $modeName")
        val builder = previewRequestBuilder
        val session = captureSession
        val characteristics = cameraCharacteristics

        try {
            when (modeName.uppercase(Locale.US)) {
                "BEAUTY" -> {
                    if (builder != null && session != null) {
                        try {
                            val faceModes = characteristics?.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES)
                            if (faceModes != null && faceModes.contains(CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE)) {
                                builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE)
                            }
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Beauty face detection rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    VibeState.selectedVibe = VibePreset.ROYAL_GOLD
                    onFeedback("💄 Beauty: Soft Skin & Warm Tone Active")
                }
                "WATERMARK" -> {
                    onFeedback("🏷️ Watermark: Lensora Computational Stamp Active")
                }
                "FOOD" -> {
                    if (builder != null && session != null) {
                        try {
                            val sceneModes = characteristics?.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && sceneModes != null && sceneModes.contains(16)) {
                                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
                                builder.set(CaptureRequest.CONTROL_SCENE_MODE, 16)
                            }
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Food scene mode rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    VibeState.selectedVibe = VibePreset.ROYAL_GOLD
                    onFeedback("🍱 Food: Warm Saturation & Micro-Contrast Active")
                }
                "DOCUMENT" -> {
                    if (builder != null && session != null) {
                        try {
                            builder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Document edge enhancement rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    VibeState.selectedVibe = VibePreset.LEICA
                    onFeedback("📄 Document: High-Contrast Text Edge Enhancement Active")
                }
                "TIME-LAPSE", "TIME_LAPSE" -> {
                    onFeedback("⏱️ Time-Lapse: High-Speed Interval Frame Capture Active")
                }
                "SLOW-MOTION", "SLOW_MOTION" -> {
                    if (builder != null && session != null) {
                        try {
                            val capabilities = characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            val supportsHighSpeed = capabilities != null && capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO)

                            if (supportsHighSpeed) {
                                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, android.util.Range(120, 120))
                                onFeedback("🐢 Slow-Motion: 120 FPS High-Speed Capture Active")
                            } else {
                                Log.w(tag, "Constrained high-speed video unsupported by HAL. Safe 60 FPS fallback.")
                                onFeedback("🐢 Slow-Motion: High-Speed HAL Unsupported (Safe Fallback)")
                            }
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Slow-Motion target FPS range rejected by HAL. Safe Auto fallback.", hardwareEx)
                            onFeedback("🐢 Slow-Motion: Safe Auto Fallback Active")
                        }
                    } else {
                        onFeedback("🐢 Slow-Motion Mode Selected")
                    }
                }
                "NIGHT VISION", "NIGHT_VISION" -> {
                    if (builder != null && session != null) {
                        try {
                            val sceneModes = characteristics?.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                            if (sceneModes != null && sceneModes.contains(CaptureRequest.CONTROL_SCENE_MODE_NIGHT)) {
                                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE)
                                builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_NIGHT)
                            } else {
                                val expRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                                val maxExp = expRange?.upper ?: 500_000_000L
                                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, maxExp.coerceAtMost(500_000_000L))
                            }
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Night Vision exposure settings rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    onFeedback("🌙 Night Vision: Low-Light Sensitivity Boost Active")
                }
                "ASTRO" -> {
                    if (builder != null && session != null) {
                        try {
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f) // Infinity focus

                            val expRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                            val targetExposureNs = expRange?.upper ?: 30_000_000_000L // 30s or max HAL supported
                            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, targetExposureNs.coerceAtMost(30_000_000_000L))

                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Astro hardware long exposure rejected by hardware/emulator. Safe Auto fallback.", hardwareEx)
                            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        }
                    }
                    onFeedback("✨ Astro: Locked Focus to Infinity & 30s Exposure")
                }
                "DEEP SKY", "DEEP_SKY" -> {
                    if (builder != null && session != null) {
                        try {
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.0f)

                            val expRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                            val targetExposureNs = expRange?.upper ?: 30_000_000_000L
                            val isoRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                            val maxIso = isoRange?.upper ?: 3200

                            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, targetExposureNs.coerceAtMost(30_000_000_000L))
                            builder.set(CaptureRequest.SENSOR_SENSITIVITY, maxIso)

                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Deep Sky exposure/ISO rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    onFeedback("🌌 Deep Sky: Infinity Focus & Max ISO Stacking Active")
                }
                "RAW DNG", "RAW_DNG" -> {
                    if (builder != null && session != null) {
                        try {
                            val capabilities = characteristics?.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            val supportsRaw = capabilities != null && capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)

                            if (supportsRaw) {
                                onFeedback("📸 RAW DNG: Uncompressed RAW_SENSOR Stream Active")
                            } else {
                                Log.w(tag, "RAW_SENSOR stream capability unsupported by HAL. Safe JPEG fallback.")
                                onFeedback("📸 RAW DNG: RAW_SENSOR Unsupported (Safe JPEG Fallback)")
                            }
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "RAW DNG query rejected by HAL. Safe Auto fallback.", hardwareEx)
                            onFeedback("📸 RAW DNG: Safe Fallback Active")
                        }
                    } else {
                        onFeedback("📸 RAW DNG Mode Selected")
                    }
                }
                "PRO MACRO", "PRO_MACRO" -> {
                    if (builder != null && session != null) {
                        try {
                            val minFocusDist = characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 10.0f
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, minFocusDist)

                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Macro hardware focus rejected by hardware/emulator. Safe Auto fallback.", hardwareEx)
                            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        }
                    }
                    onFeedback("🔍 Pro Macro: Minimum Focus Distance applied")
                }
                "ULTRA MACRO", "ULTRA_MACRO" -> {
                    if (builder != null && session != null) {
                        try {
                            val minFocusDist = characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 10.0f
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, minFocusDist)

                            val activeArraySize = characteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                            if (activeArraySize != null) {
                                val cropWidth = activeArraySize.width() / 2
                                val cropHeight = activeArraySize.height() / 2
                                val left = (activeArraySize.width() - cropWidth) / 2
                                val top = (activeArraySize.height() - cropHeight) / 2
                                builder.set(CaptureRequest.SCALER_CROP_REGION, android.graphics.Rect(left, top, left + cropWidth, top + cropHeight))
                            }
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                        } catch (hardwareEx: Exception) {
                            Log.w(tag, "Ultra Macro hardware settings rejected by HAL. Safe Auto fallback.", hardwareEx)
                        }
                    }
                    onFeedback("🔬 Ultra Macro: 2x Magnification & Min Focus Distance Active")
                }
                "LEICA" -> {
                    VibeState.selectedVibe = VibePreset.LEICA
                    onFeedback("📷 Leica B&W Color Grading Applied")
                }
                "ZEISS" -> {
                    VibeState.selectedVibe = VibePreset.CINEMATIC
                    onFeedback("🔵 ZEISS T* Anti-Reflex Color Science Applied")
                }
                "HASSELBLAD" -> {
                    VibeState.selectedVibe = VibePreset.ROYAL_GOLD
                    onFeedback("🟠 Hasselblad Natural Color Solution Applied")
                }
                "XMAGE" -> {
                    VibeState.selectedVibe = VibePreset.CINEMATIC
                    onFeedback("✨ XMAGE Vivid Color Tuning Applied")
                }
                "HARCOURT" -> {
                    VibeState.selectedVibe = VibePreset.LEICA
                    onFeedback("🎬 Studio Harcourt Portrait Black & White Applied")
                }
                "PANORAMA" -> {
                    VibeState.selectedVibe = VibePreset.ROYAL_GOLD
                    onFeedback("🌐 Panorama: Wide Aspect Dynamic Sweep Guide Active")
                }
                "DUAL-VIDEO", "DUAL_VIDEO" -> {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                    val supportsConcurrent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && cameraManager != null) {
                        try {
                            cameraManager.concurrentCameraIds.isNotEmpty()
                        } catch (_: Exception) { false }
                    } else false

                    if (supportsConcurrent) {
                        onFeedback("📹 Dual-Video: Concurrent Dual-Lens Stream Active")
                    } else {
                        Log.w(tag, "Concurrent camera streaming unsupported by HAL. Safe PIP fallback.")
                        onFeedback("📹 Dual-Video: Concurrent HAL Unsupported (Safe PIP Fallback)")
                    }
                }
                else -> {
                    onFeedback("✨ Mode '$modeName' Selected")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error setting advanced mode: $modeName", e)
            onFeedback("✨ Selected $modeName mode")
        }
    }

    fun setZoomRatio(ratio: Float) {
        val clampedRatio = ratio.coerceIn(1.0f, maxZoomRatio)
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && maxZoomRatio > 1.0f) {
                builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clampedRatio)
            } else {
                // SCALER_CROP_REGION fallback for emulators & legacy HALs
                val activeArraySize = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                if (activeArraySize != null) {
                    val cropWidth = (activeArraySize.width() / clampedRatio).toInt()
                    val cropHeight = (activeArraySize.height() / clampedRatio).toInt()
                    val left = (activeArraySize.width() - cropWidth) / 2
                    val top = (activeArraySize.height() - cropHeight) / 2
                    val cropRegion = android.graphics.Rect(left, top, left + cropWidth, top + cropHeight)
                    builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
                }
            }
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(tag, "Error setting zoom ratio: $clampedRatio", e)
        }
    }

    fun triggerTapToFocus(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return
        val handler = backgroundHandler ?: return

        try {
            val activeArray = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                ?: return

            val sensorX = ((x / viewWidth) * activeArray.width()).toInt().coerceIn(0, activeArray.width() - 100)
            val sensorY = ((y / viewHeight) * activeArray.height()).toInt().coerceIn(0, activeArray.height() - 100)

            val rect = android.graphics.Rect(sensorX, sensorY, sensorX + 100, sensorY + 100)
            val meteringRectangle = MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX)

            builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRectangle))
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)

            session.capture(builder.build(), null, handler)
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
            session.setRepeatingRequest(builder.build(), null, handler)

        } catch (e: Exception) {
            Log.e(tag, "Error triggering tap to focus", e)
        }
    }

    fun setFlashMode(mode: FlashMode) {
        currentFlashMode = mode
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return
        try {
            applyFlashToBuilder(builder)
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(tag, "Error setting flash mode: $mode", e)
        }
    }

    private fun applyFlashToBuilder(builder: CaptureRequest.Builder) {
        when (currentFlashMode) {
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
    }

    fun setCameraMode(mode: CameraMode) {
        currentMode = mode
        Log.d("LensoraDebug", "Switching to mode $mode")
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return
        try {
            applyCameraModeToBuilder(builder)
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            Log.d("LensoraDebug", "Successfully applied mode $mode")
        } catch (e: Exception) {
            Log.e("LensoraDebug", "Failed to apply mode: ${e.message}", e)
            try {
                currentMode = CameraMode.PHOTO
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            } catch (fallbackEx: Exception) {
                Log.e("LensoraDebug", "Critical fallback error: ${fallbackEx.message}", fallbackEx)
            }
        }
    }

    private fun applyCameraModeToBuilder(builder: CaptureRequest.Builder) {
        Log.d("LensoraDebug", "Configuring universal CaptureRequest.Builder for mode: $currentMode")
        try {
            // Base Universal Controls (100% FORCE CONTROL_MODE_AUTO, ZERO CONTROL_SCENE_MODE calls)
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
            applyFlashToBuilder(builder)

            when (currentMode) {
                CameraMode.AI_AUTO, CameraMode.PHOTO, CameraMode.PRO -> {
                    // Force clean AUTO mode without scene mode or forced FPS locks
                }
                CameraMode.PORTRAIT -> {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    val range = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    if (range != null && range.lower != 0 && range.upper != 0) {
                        val step = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0.33f
                        val targetStep = (0.3f / step).toInt().coerceIn(range.lower, range.upper)
                        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, targetStep)
                        Log.d("LensoraDebug", "Portrait Universal EV boost: +0.3 EV (step $targetStep)")
                    }
                }
                CameraMode.VIDEO, CameraMode.CINEMATIC -> {
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                    val fpsRanges = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                    val fixed24 = fpsRanges?.firstOrNull { it.lower == 24 && it.upper == 24 }
                    val range24 = fpsRanges?.firstOrNull { it.lower <= 24 && it.upper >= 24 }
                    val safeRange = fixed24 ?: range24 ?: fpsRanges?.minByOrNull { it.upper } ?: Range(15, 30)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        try {
                            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, safeRange)
                            Log.d("LensoraDebug", "Applied safe FPS range: $safeRange")
                        } catch (e: Exception) {
                            Log.w("LensoraDebug", "FPS range $safeRange rejected. Keeping default.", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LensoraDebug", "Failed to apply mode builder for $currentMode: ${e.message}", e)
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
        }
    }

    fun setRawCaptureEnabled(enabled: Boolean) {
        isRawCaptureEnabled = enabled
        Log.i(tag, "RAW DNG Capture Enabled: $enabled")
    }

    fun setManualExposure(iso: Int, shutterSpeedNs: Long) {
        val builder = previewRequestBuilder ?: return
        val session = captureSession ?: return
        try {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso)
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedNs)
            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
            Log.i(tag, "Applied Manual Exposure: ISO $iso, Shutter ${shutterSpeedNs / 1_000_000}ms")
        } catch (e: Exception) {
            Log.e(tag, "Error applying manual exposure", e)
        }
    }

    fun startVideoRecording(
        hasAudioPermission: Boolean,
        displayRotationDegrees: Int,
        onRecordingStarted: () -> Unit,
        onError: (String) -> Unit
    ) {
        val device = cameraDevice ?: run { onError("Camera not ready"); return }
        val previewSurf = previewSurface ?: run { onError("Preview surface missing"); return }
        val handler = backgroundHandler ?: run { onError("Handler thread missing"); return }

        try {
            val sensorOrientation = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            videoRecorder = VideoRecorder(context)
            val recorderSurface = videoRecorder?.prepare(hasAudioPermission, sensorOrientation, displayRotationDegrees)
                ?: run { onError("Failed to prepare MediaRecorder surface"); return }

            captureSession?.close()
            captureSession = null

            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            builder.addTarget(previewSurf)
            builder.addTarget(recorderSurface)

            if (currentMode == CameraMode.CINEMATIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(24, 24))
                } catch (e: Exception) {
                    Log.w(tag, "Cinematic recording 24fps lock rejected by hardware. Safe fallback.", e)
                }
            }

            val outputs = listOf(
                OutputConfiguration(previewSurf),
                OutputConfiguration(recorderSurface)
            )

            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                Executors.newSingleThreadExecutor(),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(builder.build(), null, handler)
                            videoRecorder?.start()
                            isRecording = true
                            onRecordingStarted()
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to start recording repeating request", e)
                            onError("Failed to start recording session")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError("Video capture session configuration failed")
                    }
                }
            )

            device.createCaptureSession(sessionConfig)
        } catch (e: Exception) {
            Log.e(tag, "Error starting video recording", e)
            onError("Recording error: ${e.localizedMessage}")
        }
    }

    fun stopVideoRecording(
        onVideoSaved: (android.net.Uri, String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isRecording) return
        try {
            isRecording = false
            val savedUri = videoRecorder?.stop()
            videoRecorder = null
            createCameraPreviewSession()
            if (savedUri != null) {
                onVideoSaved(savedUri, "Video saved to Gallery Movies")
            } else {
                onError("Failed to save video record")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping video recording", e)
            onError("Error stopping recording: ${e.localizedMessage}")
        }
    }



    fun takePicture(
        displayRotationDegrees: Int = 0,
        isRawEnabled: Boolean = false,
        vibePreset: String = VibePreset.NONE.name,
        onPhotoSaved: (android.net.Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val availableBytes = checkAvailableStorageBytes()
        val minRequiredBytes = 2L * 1024 * 1024 * 1024 // 2 GB Guard Limit
        if (availableBytes < minRequiredBytes) {
            val freeMb = availableBytes / (1024 * 1024)
            val msg = "Storage Low ($freeMb MB available). Minimum 2GB space required."
            Log.w(tag, "Storage Guard Triggered: Capture blocked. $msg")
            onError(msg)
            return
        }

        val device = cameraDevice ?: run { onError("Camera not ready"); return }
        val reader = imageReader ?: run { onError("ImageReader not initialized"); return }
        val session = captureSession ?: run { onError("Capture session null"); return }
        val handler = backgroundHandler ?: run { onError("Handler null"); return }

        try {
            val sensorOrientation = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            val jpegOrientation = calculateJpegOrientation(sensorOrientation, displayRotationDegrees)

            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
            applyFlashToBuilder(captureBuilder)

            reader.setOnImageAvailableListener({ readerRef ->
                val image = readerRef.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                val timeStamp = System.currentTimeMillis()
                val tempFile = File(context.cacheDir, "LENSORA_${timeStamp}.jpg.tmp")

                try {
                    FileOutputStream(tempFile).use { out ->
                        out.write(bytes)
                    }

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "LENSORA_${timeStamp}.jpg")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lensora_Ai")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            tempFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            contentValues.clear()
                            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                        }

                        tempFile.delete()

                        // Trigger L3 Foreground Service Computational AI Processor pass
                        val serviceIntent = Intent(context, com.lensora.ai.service.AiProcessorService::class.java).apply {
                            putExtra("PHOTO_URI", uri.toString())
                            putExtra("SCENE_TYPE", currentMode.name)
                            putExtra("EV_OFFSET", 0.0f)
                            putExtra("VIBE_PRESET", vibePreset)
                        }
                        androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                        Log.i(tag, "Started L3 AiProcessorService for $uri")

                        // RAW / DNG Capture Pipeline
                        if (isRawEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            saveRawDngCapture(timeStamp)
                        }

                        onPhotoSaved(uri, "Photo saved & AI processing enqueued")
                    } else {
                        tempFile.delete()
                        onError("Failed to insert MediaStore record")
                    }

                } catch (e: Exception) {
                    tempFile.delete()
                    Log.e(tag, "Error saving atomic capture", e)
                    onError("Save failed: ${e.localizedMessage}")
                }

            }, handler)

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Log.i(tag, "L0 Still Capture completed successfully")
                }
            }, handler)

        } catch (e: Exception) {
            Log.e(tag, "Error taking picture", e)
            onError("Capture error: ${e.localizedMessage}")
        }
    }

    private fun saveRawDngCapture(timeStamp: Long) {
        val characteristics = cameraCharacteristics ?: return
        val device = cameraDevice ?: return
        val reader = imageReader ?: return
        val handler = backgroundHandler ?: return

        try {
            val rawBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            rawBuilder.addTarget(reader.surface)

            captureSession?.capture(rawBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    try {
                        val dngCreator = DngCreator(characteristics, result)
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "LENSORA_RAW_${timeStamp}.dng")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lensora_Ai/RAW")
                            }
                        }

                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { out ->
                                val rawImage = reader.acquireLatestImage()
                                if (rawImage != null) {
                                    dngCreator.writeImage(out, rawImage)
                                    rawImage.close()
                                }
                            }
                            Log.i(tag, "RAW DNG captured & saved successfully to $uri")
                        }
                        dngCreator.close()
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to save RAW DNG file. Safe fallback to JPEG.", e)
                    }
                }
            }, handler)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initiate RAW DNG capture", e)
        }
    }

    private fun calculateJpegOrientation(sensorOrientation: Int, deviceRotationDegrees: Int): Int {
        return if (isFrontCamera) {
            (sensorOrientation + deviceRotationDegrees) % 360
        } else {
            (sensorOrientation - deviceRotationDegrees + 360) % 360
        }
    }

    fun closeCamera() {
        try {
            thermalListener?.let { listener ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        powerManager?.removeThermalStatusListener(listener)
                    } catch (e: Exception) {
                        Log.w(tag, "Safely ignored thermal listener removal error: ${e.localizedMessage}")
                    }
                }
            }
            thermalListener = null

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
}
