package com.lensora.ai.domain.probe

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import kotlin.system.measureTimeMillis

class CameraProbeP2 {

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
                val capabilities = characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                ) ?: intArrayOf()

                val hasRawSupport = capabilities.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
                )

                if (hasRawSupport) {
                    status = ProbeStatus.PASS
                    details = "PASSED: Camera2 REQUEST_AVAILABLE_CAPABILITIES_RAW (RAW_SENSOR) is supported on camera ID $backCameraId."
                } else {
                    status = ProbeStatus.FAIL
                    details = "FAILED: Camera2 RAW_SENSOR capability is missing on camera ID $backCameraId."
                }
            } catch (e: Exception) {
                status = ProbeStatus.ERROR
                details = "Camera2 HAL error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

        return ProbeResult(
            probeId = "P2",
            title = "P2: RAW Sensor Capability Probe",
            status = status,
            details = details,
            executionTimeMs = executionTime
        )
    }
}
