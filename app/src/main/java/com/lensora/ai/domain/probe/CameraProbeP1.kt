package com.lensora.ai.domain.probe

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import java.util.Locale
import kotlin.system.measureTimeMillis

class CameraProbeP1 {

    fun execute(context: Context): ProbeResult {
        var status = ProbeStatus.FAIL
        var details = ""

        val executionTime = measureTimeMillis {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                if (cameraManager == null) {
                    status = ProbeStatus.ERROR
                    details = "CameraManager unavailable"
                    return@measureTimeMillis
                }

                val cameraIds = cameraManager.cameraIdList
                var backCameraId: String? = null

                for (id in cameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = id
                        break
                    }
                }

                if (backCameraId == null) {
                    status = ProbeStatus.UNSUPPORTED
                    details = "No back-facing camera found"
                    return@measureTimeMillis
                }

                val characteristics = cameraManager.getCameraCharacteristics(backCameraId)
                var maxMegapixels = 0.0
                var maxResolutionSize: Size? = null

                // Check standard stream configuration map
                val map: StreamConfigurationMap? = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

                map?.let { configMap ->
                    val formats = intArrayOf(
                        ImageFormat.JPEG,
                        ImageFormat.RAW_SENSOR,
                        ImageFormat.YUV_420_888,
                        ImageFormat.PRIVATE
                    )
                    for (format in formats) {
                        val sizes = configMap.getOutputSizes(format) ?: emptyArray()
                        for (size in sizes) {
                            val mp = (size.width.toDouble() * size.height) / 1_000_000.0
                            if (mp > maxMegapixels) {
                                maxMegapixels = mp
                                maxResolutionSize = size
                            }
                        }
                    }
                }

                // Check maximum resolution stream configuration map
                val maxResMap = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION
                )
                maxResMap?.let { configMap ->
                    val formats = intArrayOf(
                        ImageFormat.JPEG,
                        ImageFormat.RAW_SENSOR,
                        ImageFormat.YUV_420_888,
                        ImageFormat.PRIVATE
                    )
                    for (format in formats) {
                        val sizes = configMap.getOutputSizes(format) ?: emptyArray()
                        for (size in sizes) {
                            val mp = (size.width.toDouble() * size.height) / 1_000_000.0
                            if (mp > maxMegapixels) {
                                maxMegapixels = mp
                                maxResolutionSize = size
                            }
                        }
                    }
                }

                val roundedMp = String.format(Locale.US, "%.1f", maxMegapixels)
                val dimStr = maxResolutionSize?.let { "${it.width}x${it.height}" } ?: "Unknown"

                if (maxMegapixels >= 100.0) {
                    status = ProbeStatus.PASS
                    details = "PASSED: Ultra-high resolution stream detected ($roundedMp MP, $dimStr)."
                } else {
                    status = ProbeStatus.FAIL
                    details = "FAILED: Max resolution exposed is $roundedMp MP ($dimStr). 200MP full-res mode not exposed via standard Camera2 characteristics."
                }
            } catch (e: Exception) {
                status = ProbeStatus.ERROR
                details = "Camera2 HAL error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

        return ProbeResult(
            probeId = "P1",
            title = "P1: 200MP Sensor & High-Res Stream Probe",
            status = status,
            details = details,
            executionTimeMs = executionTime
        )
    }
}
