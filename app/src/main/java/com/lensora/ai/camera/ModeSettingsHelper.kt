package com.lensora.ai.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import java.util.Locale

data class HalRange<T : Comparable<T>>(val lower: T, val upper: T)

data class HalRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class CameraHalCapabilities(
    val exposureTimeRangeNs: HalRange<Long>? = null,
    val isoSensitivityRange: HalRange<Int>? = null,
    val minFocusDistanceDiopters: Float? = null,
    val faceDetectModes: IntArray? = null,
    val edgeModes: IntArray? = null,
    val sceneModes: IntArray? = null,
    val aeModes: IntArray? = null,
    val afModes: IntArray? = null,
    val activeArraySize: HalRect? = null,
    val capabilities: IntArray? = null,
    val fpsRanges: List<HalRange<Int>>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraHalCapabilities

        if (exposureTimeRangeNs != other.exposureTimeRangeNs) return false
        if (isoSensitivityRange != other.isoSensitivityRange) return false
        if (minFocusDistanceDiopters != other.minFocusDistanceDiopters) return false
        if (faceDetectModes != null) {
            if (other.faceDetectModes == null) return false
            if (!faceDetectModes.contentEquals(other.faceDetectModes)) return false
        } else if (other.faceDetectModes != null) return false
        if (edgeModes != null) {
            if (other.edgeModes == null) return false
            if (!edgeModes.contentEquals(other.edgeModes)) return false
        } else if (other.edgeModes != null) return false
        if (sceneModes != null) {
            if (other.sceneModes == null) return false
            if (!sceneModes.contentEquals(other.sceneModes)) return false
        } else if (other.sceneModes != null) return false
        if (aeModes != null) {
            if (other.aeModes == null) return false
            if (!aeModes.contentEquals(other.aeModes)) return false
        } else if (other.aeModes != null) return false
        if (afModes != null) {
            if (other.afModes == null) return false
            if (!afModes.contentEquals(other.afModes)) return false
        } else if (other.afModes != null) return false
        if (activeArraySize != other.activeArraySize) return false
        if (capabilities != null) {
            if (other.capabilities == null) return false
            if (!capabilities.contentEquals(other.capabilities)) return false
        } else if (other.capabilities != null) return false
        if (fpsRanges != other.fpsRanges) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exposureTimeRangeNs?.hashCode() ?: 0
        result = 31 * result + (isoSensitivityRange?.hashCode() ?: 0)
        result = 31 * result + (minFocusDistanceDiopters?.hashCode() ?: 0)
        result = 31 * result + (faceDetectModes?.contentHashCode() ?: 0)
        result = 31 * result + (edgeModes?.contentHashCode() ?: 0)
        result = 31 * result + (sceneModes?.contentHashCode() ?: 0)
        result = 31 * result + (aeModes?.contentHashCode() ?: 0)
        result = 31 * result + (afModes?.contentHashCode() ?: 0)
        result = 31 * result + (activeArraySize?.hashCode() ?: 0)
        result = 31 * result + (capabilities?.contentHashCode() ?: 0)
        result = 31 * result + (fpsRanges?.hashCode() ?: 0)
        return result
    }
}

data class ModeSettingsResult(
    val aeMode: Int? = null,
    val afMode: Int? = null,
    val exposureTimeNs: Long? = null,
    val isoSensitivity: Int? = null,
    val focusDistance: Float? = null,
    val faceDetectMode: Int? = null,
    val edgeMode: Int? = null,
    val controlMode: Int? = null,
    val sceneMode: Int? = null,
    val cropRegion: HalRect? = null
)

object ModeSettingsHelper {

    fun extractCapabilities(characteristics: CameraCharacteristics?): CameraHalCapabilities {
        if (characteristics == null) return CameraHalCapabilities()

        val expRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)

        return CameraHalCapabilities(
            exposureTimeRangeNs = expRange?.let { HalRange(it.lower, it.upper) },
            isoSensitivityRange = isoRange?.let { HalRange(it.lower, it.upper) },
            minFocusDistanceDiopters = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
            faceDetectModes = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES),
            edgeModes = characteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES),
            sceneModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES),
            aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
            afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
            activeArraySize = activeArray?.let { HalRect(it.left, it.top, it.right, it.bottom) },
            capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES),
            fpsRanges = fpsRanges?.map { HalRange(it.lower, it.upper) }
        )
    }

    fun calculateModeSettings(modeName: String, caps: CameraHalCapabilities): ModeSettingsResult {
        return when (modeName.uppercase(Locale.US)) {
            "ASTRO" -> {
                val supportsAfOff = caps.afModes?.contains(CaptureRequest.CONTROL_AF_MODE_OFF) == true
                val afMode = if (supportsAfOff) CaptureRequest.CONTROL_AF_MODE_OFF else null
                val focusDist = if (supportsAfOff) 0.0f else null

                val supportsAeOff = caps.aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true
                val expRange = caps.exposureTimeRangeNs
                val isoRange = caps.isoSensitivityRange
                val aeMode = if (supportsAeOff && expRange != null && expRange.lower <= expRange.upper) CaptureRequest.CONTROL_AE_MODE_OFF else null
                val expTime = if (aeMode != null && expRange != null && expRange.lower <= expRange.upper) {
                    30_000_000_000L.coerceIn(expRange.lower, expRange.upper)
                } else null
                val iso = if (aeMode != null && isoRange != null && isoRange.lower <= isoRange.upper) {
                    3200.coerceIn(isoRange.lower, isoRange.upper)
                } else null

                ModeSettingsResult(
                    afMode = afMode,
                    focusDistance = focusDist,
                    aeMode = aeMode,
                    exposureTimeNs = expTime,
                    isoSensitivity = iso
                )
            }
            "DEEP SKY", "DEEP_SKY" -> {
                val supportsAfOff = caps.afModes?.contains(CaptureRequest.CONTROL_AF_MODE_OFF) == true
                val afMode = if (supportsAfOff) CaptureRequest.CONTROL_AF_MODE_OFF else null
                val focusDist = if (supportsAfOff) 0.0f else null

                val supportsAeOff = caps.aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true
                val expRange = caps.exposureTimeRangeNs
                val isoRange = caps.isoSensitivityRange
                val aeMode = if (supportsAeOff && expRange != null && expRange.lower <= expRange.upper) CaptureRequest.CONTROL_AE_MODE_OFF else null
                val expTime = if (aeMode != null && expRange != null && expRange.lower <= expRange.upper) {
                    30_000_000_000L.coerceIn(expRange.lower, expRange.upper)
                } else null
                val iso = if (aeMode != null && isoRange != null && isoRange.lower <= isoRange.upper) {
                    3200.coerceIn(isoRange.lower, isoRange.upper)
                } else null

                ModeSettingsResult(
                    afMode = afMode,
                    focusDistance = focusDist,
                    aeMode = aeMode,
                    exposureTimeNs = expTime,
                    isoSensitivity = iso
                )
            }
            "NIGHT VISION", "NIGHT_VISION" -> {
                val supportsNightScene = caps.sceneModes?.contains(CaptureRequest.CONTROL_SCENE_MODE_NIGHT) == true
                if (supportsNightScene) {
                    ModeSettingsResult(
                        controlMode = CaptureRequest.CONTROL_MODE_USE_SCENE_MODE,
                        sceneMode = CaptureRequest.CONTROL_SCENE_MODE_NIGHT
                    )
                } else {
                    val supportsAeOff = caps.aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true
                    val expRange = caps.exposureTimeRangeNs
                    if (supportsAeOff && expRange != null && expRange.lower <= expRange.upper) {
                        val expTime = 500_000_000L.coerceIn(expRange.lower, expRange.upper)
                        ModeSettingsResult(
                            aeMode = CaptureRequest.CONTROL_AE_MODE_OFF,
                            exposureTimeNs = expTime
                        )
                    } else {
                        ModeSettingsResult()
                    }
                }
            }
            "BEAUTY" -> {
                val supportsFaceDetect = caps.faceDetectModes?.contains(CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE) == true
                if (supportsFaceDetect) {
                    ModeSettingsResult(faceDetectMode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE)
                } else {
                    ModeSettingsResult()
                }
            }
            "DOCUMENT" -> {
                val supportsHighQualityEdge = caps.edgeModes?.contains(CaptureRequest.EDGE_MODE_HIGH_QUALITY) == true
                if (supportsHighQualityEdge) {
                    ModeSettingsResult(edgeMode = CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                } else {
                    ModeSettingsResult()
                }
            }
            "PRO MACRO", "PRO_MACRO" -> {
                val minFocus = caps.minFocusDistanceDiopters
                val supportsAfOff = caps.afModes?.contains(CaptureRequest.CONTROL_AF_MODE_OFF) == true
                if (minFocus != null && minFocus > 0.0f && supportsAfOff) {
                    ModeSettingsResult(
                        afMode = CaptureRequest.CONTROL_AF_MODE_OFF,
                        focusDistance = minFocus
                    )
                } else {
                    ModeSettingsResult()
                }
            }
            "ULTRA MACRO", "ULTRA_MACRO" -> {
                val minFocus = caps.minFocusDistanceDiopters
                val supportsAfOff = caps.afModes?.contains(CaptureRequest.CONTROL_AF_MODE_OFF) == true
                val afMode = if (minFocus != null && minFocus > 0.0f && supportsAfOff) CaptureRequest.CONTROL_AF_MODE_OFF else null
                val focusDist = if (afMode != null) minFocus else null

                val cropRect = caps.activeArraySize?.let { activeArray ->
                    val cropWidth = activeArray.width / 2
                    val cropHeight = activeArray.height / 2
                    val left = (activeArray.width - cropWidth) / 2
                    val top = (activeArray.height - cropHeight) / 2
                    HalRect(left, top, left + cropWidth, top + cropHeight)
                }

                ModeSettingsResult(
                    afMode = afMode,
                    focusDistance = focusDist,
                    cropRegion = cropRect
                )
            }
            else -> ModeSettingsResult()
        }
    }

    fun formatModeFeedbackLabel(modeName: String, settings: ModeSettingsResult): String {
        val upperMode = modeName.uppercase(Locale.US)
        val prefix = when (upperMode) {
            "ASTRO" -> "✨ Astro: Locked focus to infinity"
            "DEEP SKY", "DEEP_SKY" -> "🌌 Deep Sky: Locked focus to infinity"
            else -> modeName
        }
        val expSec = settings.exposureTimeNs?.let { ns ->
            val sec = ns / 1_000_000_000.0
            if (sec >= 1.0 && sec == sec.toLong().toDouble()) {
                "${sec.toLong()} s exposure"
            } else {
                String.format(Locale.US, "%.1f s exposure", sec)
            }
        }
        val isoText = settings.isoSensitivity?.let { "ISO $it" }

        val details = listOfNotNull(expSec, isoText).joinToString(", ")
        return if (details.isNotEmpty()) "$prefix, $details" else prefix
    }
}

