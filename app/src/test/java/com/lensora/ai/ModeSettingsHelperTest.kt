package com.lensora.ai

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import com.lensora.ai.camera.CameraHalCapabilities
import com.lensora.ai.camera.HalRange
import com.lensora.ai.camera.HalRect
import com.lensora.ai.camera.ModeSettingsHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModeSettingsHelperTest {

    private val defaultCapabilities = CameraHalCapabilities(
        exposureTimeRangeNs = HalRange(83_000L, 1_000_000_000L), // 0.083ms to 1000ms
        isoSensitivityRange = HalRange(50, 3200),
        minFocusDistanceDiopters = 10.0f,
        faceDetectModes = intArrayOf(0, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE),
        edgeModes = intArrayOf(0, 1, CaptureRequest.EDGE_MODE_HIGH_QUALITY),
        sceneModes = intArrayOf(0, CaptureRequest.CONTROL_SCENE_MODE_NIGHT),
        aeModes = intArrayOf(1, CaptureRequest.CONTROL_AE_MODE_OFF),
        afModes = intArrayOf(1, CaptureRequest.CONTROL_AF_MODE_OFF),
        activeArraySize = HalRect(0, 0, 4080, 3060),
        capabilities = intArrayOf(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO),
        fpsRanges = listOf(HalRange(30, 30))
    )

    @Test
    fun astroMode_clampsExposureAndIsoToHalMaxAndSetsInfinityFocus() {
        val result = ModeSettingsHelper.calculateModeSettings("ASTRO", defaultCapabilities)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(0.0f, result.focusDistance)
        assertEquals(CaptureRequest.CONTROL_AE_MODE_OFF, result.aeMode)
        // Requested 30s (30_000_000_000L), clamped to HAL max (1_000_000_000L = 1000ms)
        assertEquals(1_000_000_000L, result.exposureTimeNs)
        // Sets explicit ISO 3200 consistent with long exposure and probe range
        assertEquals(3200, result.isoSensitivity)
    }

    @Test
    fun astroMode_whenAeModeOffUnsupported_returnsNullAeExposureAndIso() {
        val capsNoAeOff = defaultCapabilities.copy(aeModes = intArrayOf(1))
        val result = ModeSettingsHelper.calculateModeSettings("ASTRO", capsNoAeOff)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(0.0f, result.focusDistance)
        assertNull(result.aeMode)
        assertNull(result.exposureTimeNs)
        assertNull(result.isoSensitivity)
    }

    @Test
    fun deepSkyMode_clampsExposureAndIsoToHalRanges() {
        val result = ModeSettingsHelper.calculateModeSettings("DEEP SKY", defaultCapabilities)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(0.0f, result.focusDistance)
        assertEquals(CaptureRequest.CONTROL_AE_MODE_OFF, result.aeMode)
        assertEquals(1_000_000_000L, result.exposureTimeNs)
        assertEquals(3200, result.isoSensitivity)
    }

    @Test
    fun deepSkyMode_whenIsoRangeSmaller_clampsIsoToUpperBound() {
        val capsSmallIso = defaultCapabilities.copy(isoSensitivityRange = HalRange(100, 1600))
        val result = ModeSettingsHelper.calculateModeSettings("DEEP_SKY", capsSmallIso)

        assertEquals(1600, result.isoSensitivity)
    }

    @Test
    fun nightVisionMode_whenSceneModeAdvertised_usesSceneMode() {
        val result = ModeSettingsHelper.calculateModeSettings("NIGHT VISION", defaultCapabilities)

        assertEquals(CaptureRequest.CONTROL_MODE_USE_SCENE_MODE, result.controlMode)
        assertEquals(CaptureRequest.CONTROL_SCENE_MODE_NIGHT, result.sceneMode)
        assertNull(result.aeMode)
        assertNull(result.exposureTimeNs)
    }

    @Test
    fun nightVisionMode_whenSceneModeNotAdvertised_fallbacksToManualExposureClamped() {
        val capsNoNightScene = defaultCapabilities.copy(sceneModes = intArrayOf(0))
        val result = ModeSettingsHelper.calculateModeSettings("NIGHT_VISION", capsNoNightScene)

        assertNull(result.controlMode)
        assertEquals(CaptureRequest.CONTROL_AE_MODE_OFF, result.aeMode)
        assertEquals(500_000_000L, result.exposureTimeNs)
    }

    @Test
    fun beautyMode_whenFaceDetectAdvertised_setsSimpleFaceMode() {
        val result = ModeSettingsHelper.calculateModeSettings("BEAUTY", defaultCapabilities)

        assertEquals(CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE, result.faceDetectMode)
    }

    @Test
    fun beautyMode_whenFaceDetectNotAdvertised_returnsNull() {
        val capsNoFace = defaultCapabilities.copy(faceDetectModes = intArrayOf(0))
        val result = ModeSettingsHelper.calculateModeSettings("BEAUTY", capsNoFace)

        assertNull(result.faceDetectMode)
    }

    @Test
    fun documentMode_whenEdgeModeAdvertised_setsHighQualityEdgeMode() {
        val result = ModeSettingsHelper.calculateModeSettings("DOCUMENT", defaultCapabilities)

        assertEquals(CaptureRequest.EDGE_MODE_HIGH_QUALITY, result.edgeMode)
    }

    @Test
    fun documentMode_whenEdgeModeNotAdvertised_returnsNull() {
        val capsNoEdge = defaultCapabilities.copy(edgeModes = intArrayOf(0, 1))
        val result = ModeSettingsHelper.calculateModeSettings("DOCUMENT", capsNoEdge)

        assertNull(result.edgeMode)
    }

    @Test
    fun proMacroMode_whenValidMinFocusDistance_setsMinFocus() {
        val result = ModeSettingsHelper.calculateModeSettings("PRO MACRO", defaultCapabilities)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(10.0f, result.focusDistance)
    }

    @Test
    fun proMacroMode_whenZeroMinFocusDistance_returnsNull() {
        val capsZeroFocus = defaultCapabilities.copy(minFocusDistanceDiopters = 0.0f)
        val result = ModeSettingsHelper.calculateModeSettings("PRO_MACRO", capsZeroFocus)

        assertNull(result.afMode)
        assertNull(result.focusDistance)
    }

    @Test
    fun ultraMacroMode_setsMinFocusAnd2xCropRegion() {
        val result = ModeSettingsHelper.calculateModeSettings("ULTRA MACRO", defaultCapabilities)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(10.0f, result.focusDistance)
        assertEquals(HalRect(1020, 765, 3060, 2295), result.cropRegion)
    }

    @Test
    fun foodMode_returnsEmptyResult_andSendsNoSceneOverride() {
        val result = ModeSettingsHelper.calculateModeSettings("FOOD", defaultCapabilities)

        assertNull(result.controlMode)
        assertNull(result.sceneMode)
    }

    @Test
    fun foodMode_withNullCapabilities_returnsEmptyResult() {
        val result = ModeSettingsHelper.calculateModeSettings("FOOD", CameraHalCapabilities())

        assertNull(result.controlMode)
        assertNull(result.sceneMode)
    }

    @Test
    fun slowMotionMode_returnsEmptyResult_andSendsNoFpsRangeOnStillCapture() {
        val result = ModeSettingsHelper.calculateModeSettings("SLOW-MOTION", defaultCapabilities)

        assertNull(result.aeMode)
        assertNull(result.exposureTimeNs)
    }

    @Test
    fun slowMotionMode_withNullCapabilities_returnsEmptyResult() {
        val result = ModeSettingsHelper.calculateModeSettings("SLOW-MOTION", CameraHalCapabilities())

        assertNull(result.aeMode)
        assertNull(result.exposureTimeNs)
    }

    @Test
    fun allModes_withNullCapabilities_returnEmptyResult() {
        val emptyCaps = CameraHalCapabilities()
        val modes = listOf("ASTRO", "DEEP SKY", "NIGHT VISION", "BEAUTY", "DOCUMENT", "PRO MACRO", "ULTRA MACRO", "SLOW-MOTION", "FOOD")

        for (mode in modes) {
            val result = ModeSettingsHelper.calculateModeSettings(mode, emptyCaps)
            assertNull("mode $mode should have null aeMode", result.aeMode)
            assertNull("mode $mode should have null sceneMode", result.sceneMode)
        }
    }

    @Test
    fun unlistedMode_returnsEmptyResult() {
        val resultPhoto = ModeSettingsHelper.calculateModeSettings("PHOTO", defaultCapabilities)
        val resultPortrait = ModeSettingsHelper.calculateModeSettings("PORTRAIT", defaultCapabilities)
        val resultPro = ModeSettingsHelper.calculateModeSettings("PRO", defaultCapabilities)

        assertNull(resultPhoto.aeMode)
        assertNull(resultPhoto.exposureTimeNs)
        assertNull(resultPortrait.aeMode)
        assertNull(resultPro.aeMode)
    }

    @Test
    fun astroMode_whenIsoRangeHigherThan3200_clampsToLowerBound() {
        val capsHighIso = defaultCapabilities.copy(isoSensitivityRange = HalRange(4000, 8000))
        val result = ModeSettingsHelper.calculateModeSettings("ASTRO", capsHighIso)

        assertEquals(4000, result.isoSensitivity)
    }

    @Test
    fun astroMode_whenIsoRange50To6400_givesIso3200() {
        val capsWideIso = defaultCapabilities.copy(isoSensitivityRange = HalRange(50, 6400))
        val result = ModeSettingsHelper.calculateModeSettings("ASTRO", capsWideIso)

        assertEquals(3200, result.isoSensitivity)
    }

    @Test
    fun astroMode_whenInvertedHalRange_returnsNullAndDoesNotThrow() {
        val capsInverted = defaultCapabilities.copy(
            isoSensitivityRange = HalRange(6400, 100),
            exposureTimeRangeNs = HalRange(1_000_000_000L, 1_000_000L)
        )
        val result = ModeSettingsHelper.calculateModeSettings("ASTRO", capsInverted)

        assertEquals(CaptureRequest.CONTROL_AF_MODE_OFF, result.afMode)
        assertEquals(0.0f, result.focusDistance)
        assertNull(result.isoSensitivity)
        assertNull(result.exposureTimeNs)
    }

    @Test
    fun deepSkyMode_whenIsoRange50To6400_givesIso3200() {
        val capsWideIso = defaultCapabilities.copy(isoSensitivityRange = HalRange(50, 6400))
        val result = ModeSettingsHelper.calculateModeSettings("DEEP SKY", capsWideIso)

        assertEquals(3200, result.isoSensitivity)
    }

    @Test
    fun deepSkyMode_whenIsoRange4000To8000_clampsToLowerBound() {
        val capsHighIso = defaultCapabilities.copy(isoSensitivityRange = HalRange(4000, 8000))
        val result = ModeSettingsHelper.calculateModeSettings("DEEP SKY", capsHighIso)

        assertEquals(4000, result.isoSensitivity)
    }

    @Test
    fun nightVisionMode_whenInvertedExposureRange_returnsNullAndDoesNotThrow() {
        val capsInverted = defaultCapabilities.copy(
            sceneModes = intArrayOf(0),
            exposureTimeRangeNs = HalRange(1_000_000_000L, 1_000_000L)
        )
        val result = ModeSettingsHelper.calculateModeSettings("NIGHT_VISION", capsInverted)

        assertNull(result.controlMode)
        assertNull(result.exposureTimeNs)
    }

    @Test
    fun formatModeFeedbackLabel_subSecondExposures_returnsMsText() {
        val settings1s = ModeSettingsHelper.calculateModeSettings("ASTRO", defaultCapabilities)
        val labelAstro = ModeSettingsHelper.formatModeFeedbackLabel("ASTRO", settings1s)
        val labelDeepSky = ModeSettingsHelper.formatModeFeedbackLabel("DEEP SKY", settings1s)

        assertEquals("✨ Astro: Locked focus to infinity, 1 s exposure, ISO 3200", labelAstro)
        assertEquals("🌌 Deep Sky: Locked focus to infinity, 1 s exposure, ISO 3200", labelDeepSky)

        val settingsHalfSec = settings1s.copy(exposureTimeNs = 500_000_000L, isoSensitivity = 1600)
        val labelHalfSec = ModeSettingsHelper.formatModeFeedbackLabel("ASTRO", settingsHalfSec)
        assertEquals("✨ Astro: Locked focus to infinity, 500 ms exposure, ISO 1600", labelHalfSec)

        val settings40ms = settings1s.copy(exposureTimeNs = 40_000_000L, isoSensitivity = 1000)
        val label40ms = ModeSettingsHelper.formatModeFeedbackLabel("ASTRO", settings40ms)
        assertEquals("✨ Astro: Locked focus to infinity, 40 ms exposure, ISO 1000", label40ms)

        val settingsNullExp = settings1s.copy(exposureTimeNs = null, isoSensitivity = null)
        val labelNullExp = ModeSettingsHelper.formatModeFeedbackLabel("ASTRO", settingsNullExp)
        assertEquals("✨ Astro: Locked focus to infinity", labelNullExp)
    }
}
