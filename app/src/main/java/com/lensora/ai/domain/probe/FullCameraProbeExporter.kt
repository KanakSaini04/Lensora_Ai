package com.lensora.ai.domain.probe

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class SubCameraOpenResult(
    val subCameraId: String,
    val accessibilityStatus: String,
    val errorMessage: String? = null,
)

data class CameraProbeExportResult(
    val success: Boolean,
    val jsonFilePath: String,
    val jsonString: String,
    val errorMessage: String? = null,
)

class FullCameraProbeExporter {

    private val tag = "FullCameraProbeExporter"

    suspend fun export(context: Context): CameraProbeExportResult = withContext(Dispatchers.IO) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return@withContext CameraProbeExportResult(
                    success = false,
                    jsonFilePath = "",
                    jsonString = "",
                    errorMessage = "CameraManager system service unavailable",
                )

            val rootJson = JSONObject()
            val timestampIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            rootJson.put("timestampIso", timestampIso)

            // 1. Device Info
            val deviceJson = JSONObject().apply {
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("brand", Build.BRAND)
                put("device", Build.DEVICE)
                put("hardware", Build.HARDWARE)
                put("board", Build.BOARD)
                put("fingerprint", Build.FINGERPRINT)
                put("androidRelease", Build.VERSION.RELEASE)
                put("sdkInt", Build.VERSION.SDK_INT)
            }
            rootJson.put("deviceInfo", deviceJson)

            // 2. Concurrent Camera Groups
            val concurrentArray = JSONArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val concurrentSets = cameraManager.concurrentCameraIds
                    for (set in concurrentSets) {
                        val setArray = JSONArray()
                        for (id in set) setArray.put(id)
                        concurrentArray.put(setArray)
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error querying concurrentCameraIds", e)
                }
            }
            rootJson.put("concurrentCameras", concurrentArray)

            // 3. Per Camera ID Audit
            val cameraIds = cameraManager.cameraIdList
            val camerasArray = JSONArray()

            for (cameraId in cameraIds) {
                val characteristics = try {
                    cameraManager.getCameraCharacteristics(cameraId)
                } catch (e: Exception) {
                    Log.w(tag, "Failed to get characteristics for camera $cameraId", e)
                    continue
                }

                val camJson = JSONObject()
                camJson.put("cameraId", cameraId)

                // Lens facing
                val facing = characteristics[CameraCharacteristics.LENS_FACING]
                val facingStr = when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                    CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN ($facing)"
                }
                camJson.put("lensFacing", facingStr)

                // Hardware level
                val hwLevel = characteristics[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
                val hwLevelStr = when (hwLevel) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN ($hwLevel)"
                }
                camJson.put("hardwareLevel", hwLevelStr)

                // Focal lengths
                val focalLengths = characteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]
                val focalArray = JSONArray()
                focalLengths?.forEach { focalArray.put(it) }
                camJson.put("focalLengthsMm", focalArray)

                // Apertures
                val apertures = characteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES]
                val apertureArray = JSONArray()
                apertures?.forEach { apertureArray.put(it) }
                camJson.put("aperturesF", apertureArray)

                // Sensor physical size
                val sensorSize = characteristics[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
                camJson.put("sensorPhysicalSizeMm", sensorSize?.let { "${it.width}x${it.height}" } ?: "Unknown")

                // Pixel array size
                val pixelArray = characteristics[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]
                camJson.put("pixelArraySize", pixelArray?.let { "${it.width}x${it.height}" } ?: "Unknown")

                // Min focus distance & hyperfocal
                val minFocusDist = characteristics[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE] ?: 0.0f
                camJson.put("minFocusDistanceDiopters", minFocusDist)
                camJson.put("hyperfocalDistanceMeters", if (minFocusDist > 0) String.format(Locale.US, "%.2f", 1.0f / minFocusDist) else "Infinity")

                // Capabilities
                val caps = characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES] ?: intArrayOf()
                val capsArray = JSONArray()
                for (cap in caps) {
                    val capName = when (cap) {
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "BACKWARD_COMPATIBLE"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "MANUAL_SENSOR"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "MANUAL_POST_PROCESSING"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW_SENSOR"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "PRIVATE_REPROCESSING"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "READ_SENSOR_SETTINGS"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "BURST_CAPTURE"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV_REPROCESSING"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "DEPTH_OUTPUT"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "CONSTRAINED_HIGH_SPEED_VIDEO"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> "MOTION_TRACKING"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "LOGICAL_MULTI_CAMERA"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> "MONOCHROME"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA -> "SECURE_IMAGE_DATA"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_SYSTEM_CAMERA -> "SYSTEM_CAMERA"
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_OFFLINE_PROCESSING -> "OFFLINE_PROCESSING"
                        else -> "CAPABILITY_$cap"
                    }
                    capsArray.put(capName)
                }
                camJson.put("capabilities", capsArray)

                // Stream Configuration Map
                val map: StreamConfigurationMap? = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]

                val jpegSizes = map?.getOutputSizes(ImageFormat.JPEG)?.map { "${it.width}x${it.height}" } ?: emptyList()
                val jpegArray = JSONArray()
                jpegSizes.forEach { jpegArray.put(it) }
                camJson.put("outputResolutionsJpeg", jpegArray)

                val yuvSizes = map?.getOutputSizes(ImageFormat.YUV_420_888)?.map { "${it.width}x${it.height}" } ?: emptyList()
                val yuvArray = JSONArray()
                yuvSizes.forEach { yuvArray.put(it) }
                camJson.put("outputResolutionsYuv", yuvArray)

                val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)?.map { "${it.width}x${it.height}" } ?: emptyList()
                val rawArray = JSONArray()
                rawSizes.forEach { rawArray.put(it) }
                camJson.put("outputResolutionsRaw", rawArray)

                // Max Resolution Stream Map
                val maxResArray = JSONArray()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val maxMap = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION]
                    val maxJpeg = maxMap?.getOutputSizes(ImageFormat.JPEG)?.map { "${it.width}x${it.height}" } ?: emptyList()
                    maxJpeg.forEach { maxResArray.put(it) }
                }
                camJson.put("maxResolutionStreamJpeg", maxResArray)

                // AE Target FPS Ranges
                val fpsRanges = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES] ?: emptyArray()
                val fpsArray = JSONArray()
                fpsRanges.forEach { fpsArray.put("[${it.lower}, ${it.upper}]") }
                camJson.put("aeFpsRanges", fpsArray)

                // ISO Range
                val isoRange = characteristics[CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE]
                camJson.put("isoSensitivityRange", isoRange?.let { "${it.lower} - ${it.upper}" } ?: "Unsupported")

                // Exposure Time Range (ms)
                val exposureNs = characteristics[CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE]
                val exposureMsStr = exposureNs?.let {
                    val minMs = String.format(Locale.US, "%.3f", it.lower / 1_000_000.0)
                    val maxMs = String.format(Locale.US, "%.1f", it.upper / 1_000_000.0)
                    "$minMs ms - $maxMs ms"
                } ?: "Unsupported"
                camJson.put("exposureTimeRangeMs", exposureMsStr)

                // AE Compensation
                val aeCompRange = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]
                val aeCompStep = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]
                val aeCompStr = if (aeCompRange != null && aeCompStep != null) {
                    "${aeCompRange.lower}..${aeCompRange.upper} (step ${aeCompStep.numerator}/${aeCompStep.denominator})"
                } else "Unsupported"
                camJson.put("exposureCompensation", aeCompStr)

                // Face Detect Modes
                val faceModes = characteristics[CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES] ?: intArrayOf()
                val faceArray = JSONArray()
                for (mode in faceModes) {
                    val modeName = when (mode) {
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF -> "OFF"
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE -> "SIMPLE"
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL -> "FULL"
                        else -> "MODE_$mode"
                    }
                    faceArray.put(modeName)
                }
                camJson.put("faceDetectModes", faceArray)

                // Scene Modes
                val sceneModes = characteristics[CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES] ?: intArrayOf()
                val sceneArray = JSONArray()
                for (mode in sceneModes) {
                    val modeName = when (mode) {
                        CameraMetadata.CONTROL_SCENE_MODE_DISABLED -> "DISABLED"
                        CameraMetadata.CONTROL_SCENE_MODE_ACTION -> "ACTION"
                        CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT -> "PORTRAIT"
                        CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE -> "LANDSCAPE"
                        CameraMetadata.CONTROL_SCENE_MODE_NIGHT -> "NIGHT"
                        CameraMetadata.CONTROL_SCENE_MODE_THEATRE -> "THEATRE"
                        CameraMetadata.CONTROL_SCENE_MODE_BEACH -> "BEACH"
                        CameraMetadata.CONTROL_SCENE_MODE_SNOW -> "SNOW"
                        CameraMetadata.CONTROL_SCENE_MODE_SUNSET -> "SUNSET"
                        CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO -> "STEADYPHOTO"
                        CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS -> "FIREWORKS"
                        CameraMetadata.CONTROL_SCENE_MODE_SPORTS -> "SPORTS"
                        CameraMetadata.CONTROL_SCENE_MODE_PARTY -> "PARTY"
                        CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT -> "CANDLELIGHT"
                        CameraMetadata.CONTROL_SCENE_MODE_BARCODE -> "BARCODE"
                        CameraMetadata.CONTROL_SCENE_MODE_HDR -> "HDR"
                        else -> "SCENE_$mode"
                    }
                    sceneArray.put(modeName)
                }
                camJson.put("sceneModes", sceneArray)

                // Effect Modes
                val effectModes = characteristics[CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS] ?: intArrayOf()
                val effectArray = JSONArray()
                for (effect in effectModes) {
                    val effectName = when (effect) {
                        CameraMetadata.CONTROL_EFFECT_MODE_OFF -> "OFF"
                        CameraMetadata.CONTROL_EFFECT_MODE_MONO -> "MONO"
                        CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE -> "NEGATIVE"
                        CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE -> "SOLARIZE"
                        CameraMetadata.CONTROL_EFFECT_MODE_SEPIA -> "SEPIA"
                        CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE -> "POSTERIZE"
                        CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD -> "WHITEBOARD"
                        CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD -> "BLACKBOARD"
                        CameraMetadata.CONTROL_EFFECT_MODE_AQUA -> "AQUA"
                        else -> "EFFECT_$effect"
                    }
                    effectArray.put(effectName)
                }
                camJson.put("effectModes", effectArray)

                // AWB Modes
                val awbModes = characteristics[CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES] ?: intArrayOf()
                val awbArray = JSONArray()
                for (awb in awbModes) {
                    val awbName = when (awb) {
                        CameraMetadata.CONTROL_AWB_MODE_OFF -> "OFF"
                        CameraMetadata.CONTROL_AWB_MODE_AUTO -> "AUTO"
                        CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> "INCANDESCENT"
                        CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> "FLUORESCENT"
                        CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT -> "WARM_FLUORESCENT"
                        CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> "DAYLIGHT"
                        CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "CLOUDY_DAYLIGHT"
                        CameraMetadata.CONTROL_AWB_MODE_TWILIGHT -> "TWILIGHT"
                        CameraMetadata.CONTROL_AWB_MODE_SHADE -> "SHADE"
                        else -> "AWB_$awb"
                    }
                    awbArray.put(awbName)
                }
                camJson.put("awbModes", awbArray)

                // Optical Stabilization Modes
                val oisModes = characteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION] ?: intArrayOf()
                val oisArray = JSONArray()
                for (ois in oisModes) {
                    val oisName = when (ois) {
                        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF -> "OFF"
                        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON -> "ON"
                        else -> "OIS_$ois"
                    }
                    oisArray.put(oisName)
                }
                camJson.put("opticalStabilizationModes", oisArray)

                // Video Stabilization Modes
                val videoStab = characteristics[CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES] ?: intArrayOf()
                val videoStabArray = JSONArray()
                for (stab in videoStab) {
                    val stabName = when (stab) {
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF -> "OFF"
                        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON -> "ON"
                        else -> "VIDEO_STAB_$stab"
                    }
                    videoStabArray.put(stabName)
                }
                camJson.put("videoStabilizationModes", videoStabArray)

                // Camera2 Extensions Support
                val extArray = JSONArray()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val extChars = cameraManager.getCameraExtensionCharacteristics(cameraId)
                        val supportedExts = extChars.supportedExtensions
                        for (ext in supportedExts) {
                            val extName = when (ext) {
                                CameraExtensionCharacteristics.EXTENSION_AUTOMATIC -> "AUTOMATIC"
                                CameraExtensionCharacteristics.EXTENSION_BOKEH -> "BOKEH"
                                CameraExtensionCharacteristics.EXTENSION_HDR -> "HDR"
                                CameraExtensionCharacteristics.EXTENSION_NIGHT -> "NIGHT"
                                CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH -> "FACE_RETOUCH"
                                else -> "EXTENSION_$ext"
                            }
                            extArray.put(extName)
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Camera2 Extensions query failed for camera $cameraId", e)
                    }
                }
                camJson.put("extensionsSupported", extArray)

                // Logical Multi-Camera & Physical Sub-Cameras Open Test
                val logicalJson = JSONObject()
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    characteristics.physicalCameraIds ?: emptySet()
                } else emptySet()

                val isLogicalMulti = caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) || physicalIds.isNotEmpty()
                logicalJson.put("isLogicalMultiCamera", isLogicalMulti)

                val physicalIdsArray = JSONArray()
                physicalIds.forEach { physicalIdsArray.put(it) }
                logicalJson.put("physicalSubCameraIds", physicalIdsArray)

                val subOpenResultsArray = JSONArray()
                if (physicalIds.isNotEmpty()) {
                    for (subId in physicalIds) {
                        val subResult = performReadOnlySubCameraOpenTest(cameraManager, subId)
                        val subJson = JSONObject().apply {
                            put("subCameraId", subResult.subCameraId)
                            put("accessibilityStatus", subResult.accessibilityStatus)
                            put("errorMessage", subResult.errorMessage ?: JSONObject.NULL)
                        }
                        subOpenResultsArray.put(subJson)
                    }
                }
                logicalJson.put("subCameraOpenTestResults", subOpenResultsArray)
                camJson.put("logicalMultiCamera", logicalJson)

                camerasArray.put(camJson)
            }

            rootJson.put("cameras", camerasArray)

            val jsonString = rootJson.toString(2)

            // Save JSON report to primary filesDir/probe/ and app external files dir
            val primaryFile = File(File(context.filesDir, "probe"), "camera_probe_report.json").apply {
                parentFile?.mkdirs()
                writeText(jsonString)
            }

            try {
                val extFilesDir = context.getExternalFilesDir("probe")
                if (extFilesDir != null) {
                    File(extFilesDir, "camera_probe_report.json").writeText(jsonString)
                }
            } catch (e: Exception) {
                Log.w(tag, "Could not write to getExternalFilesDir", e)
            }

            try {
                val docsDir = File("/sdcard/Documents/Lensora_Ai/probe").apply { mkdirs() }
                File(docsDir, "camera_probe_report.json").writeText(jsonString)
            } catch (e: Exception) {
                Log.w(tag, "Could not write to /sdcard/Documents/Lensora_Ai/probe", e)
            }

            Log.i(tag, "Exported Camera Capability Probe JSON to ${primaryFile.absolutePath}")

            CameraProbeExportResult(
                success = true,
                jsonFilePath = primaryFile.absolutePath,
                jsonString = jsonString,
            )
        } catch (e: Exception) {
            Log.e(tag, "Error performing Camera Capability Probe export", e)
            CameraProbeExportResult(
                success = false,
                jsonFilePath = "",
                jsonString = "",
                errorMessage = e.localizedMessage ?: e.javaClass.simpleName,
            )
        }
    }

    private suspend fun performReadOnlySubCameraOpenTest(
        cameraManager: CameraManager,
        subCameraId: String,
    ): SubCameraOpenResult = withContext(Dispatchers.IO) {
        val thread = HandlerThread("SubCamProbeThread_$subCameraId").apply { start() }
        val handler = Handler(thread.looper)

        try {
            suspendCancellableCoroutine { continuation ->
                try {
                    @Suppress("MissingPermission")
                    cameraManager.openCamera(subCameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            Log.i("FullCameraProbeExporter", "Read-only open test SUCCESS for physical sub-camera ID $subCameraId")
                            camera.close()
                            if (continuation.isActive) {
                                continuation.resume(
                                    SubCameraOpenResult(
                                        subCameraId = subCameraId,
                                        accessibilityStatus = "ACCESSIBLE",
                                    )
                                )
                            }
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            camera.close()
                            if (continuation.isActive) {
                                continuation.resume(
                                    SubCameraOpenResult(
                                        subCameraId = subCameraId,
                                        accessibilityStatus = "BLOCKED_BY_HAL",
                                        errorMessage = "Sub-camera disconnected by HAL during open test",
                                    )
                                )
                            }
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            camera.close()
                            if (continuation.isActive) {
                                continuation.resume(
                                    SubCameraOpenResult(
                                        subCameraId = subCameraId,
                                        accessibilityStatus = "BLOCKED_BY_HAL",
                                        errorMessage = "Sub-camera open failed with HAL error code $error",
                                    )
                                )
                            }
                        }
                    }, handler)
                } catch (e: Exception) {
                    Log.w("FullCameraProbeExporter", "Error attempting read-only open on sub-camera $subCameraId", e)
                    if (continuation.isActive) {
                        continuation.resume(
                            SubCameraOpenResult(
                                subCameraId = subCameraId,
                                accessibilityStatus = "BLOCKED_BY_HAL",
                                errorMessage = e.localizedMessage ?: e.javaClass.simpleName,
                            )
                        )
                    }
                }
            }
        } finally {
            thread.quitSafely()
        }
    }
}
