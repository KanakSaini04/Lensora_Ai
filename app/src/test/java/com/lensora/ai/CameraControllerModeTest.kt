package com.lensora.ai

import com.lensora.ai.camera.AdvancedModeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControllerModeTest {

    @Test
    fun advancedModeState_highResSelection_andCarouselReset() {
        val state = AdvancedModeState()

        // Initial state
        assertEquals("AUTO", state.activeModeName)
        assertFalse(state.isHighResActive())

        // 1. High-Res selection -> isHighResActive is true
        state.selectAdvancedMode("High-Res")
        assertTrue(state.isHighResActive())
        assertEquals("High-Res", state.activeModeName)

        // 2. Carousel mode changed (e.g. PHOTO) -> resets to AUTO
        state.onCarouselModeChanged()
        assertFalse(state.isHighResActive())
        assertEquals("AUTO", state.activeModeName)

        // 3. High-Res selection -> isHighResActive is true
        state.selectAdvancedMode("HIGH-RES")
        assertTrue(state.isHighResActive())

        // 4. Carousel mode changed (e.g. PORTRAIT) -> resets to AUTO
        state.onCarouselModeChanged()
        assertFalse(state.isHighResActive())
        assertEquals("AUTO", state.activeModeName)

        // 5. Astro selection -> isHighResActive is false
        state.selectAdvancedMode("Astro")
        assertFalse(state.isHighResActive())
        assertEquals("Astro", state.activeModeName)
    }
}
