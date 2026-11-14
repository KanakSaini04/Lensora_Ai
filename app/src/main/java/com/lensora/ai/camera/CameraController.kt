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
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import kotlin.math.abs

class CameraController(
    private val context: Context,
) {
    private val tag = "CameraController"

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var previewSurface: Surface? = null
    private var cameraId: String? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var sensorOrientation: Int = 90

    private var supportsZoomRatioControl: Boolean = false
    private var activeSensorArraySize: Rect? = null
    private var minZoomRatio: Float = 1.0f
    private var maxZoomRatio: Float = 10.0f
    private var currentZoomRatio: Float = 1.0f

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

    @SuppressLint("MissingPermission")
    fun openCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        startBackgroundThread()
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

    fun takePicture(
        displayRotationDegrees: Int,
        onPhotoSaved: (Uri, String) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
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
