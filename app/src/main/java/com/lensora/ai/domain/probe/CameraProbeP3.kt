package com.lensora.ai.domain.probe

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Range
import kotlin.system.measureTimeMillis

class CameraProbeP3 {

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
                val fpsRanges: Array<Range<Int>> = characteristics.get(
                    CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
                ) ?: emptyArray()

                val matching24FpsRanges = fpsRanges.filter { range ->
                    range.lower <= 24 && range.upper >= 24
                }

                val allRangesStr = fpsRanges.joinToString(", ") { "[${it.lower}, ${it.upper}]" }
                val matchingStr = matching24FpsRanges.joinToString(", ") { "[${it.lower}, ${it.upper}]" }

                if (matching24FpsRanges.isNotEmpty()) {
                    status = ProbeStatus.PASS
                    details = "PASSED: Target 24fps supported. Matching AE ranges: $matchingStr. All exposed ranges: $allRangesStr"
                } else {
                    status = ProbeStatus.FAIL
                    details = "FAILED: No AE FPS range supports 24fps. All exposed ranges: $allRangesStr"
                }
            } catch (e: Exception) {
                status = ProbeStatus.ERROR
                details = "Camera2 HAL error: ${e.localizedMessage ?: e.javaClass.simpleName}"
            }
        }

        return ProbeResult(
            probeId = "P3",
            title = "P3: 24fps Cinema Target Rate Probe",
            status = status,
            details = details,
            executionTimeMs = executionTime
        )
    }
}
