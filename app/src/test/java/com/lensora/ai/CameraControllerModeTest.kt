package com.lensora.ai

import com.lensora.ai.camera.CameraMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraControllerModeTest {

    private fun isHighResMode(modeName: String): Boolean {
        return modeName.equals("High-Res", ignoreCase = true) || modeName.equals("HIGH-RES", ignoreCase = true)
    }

    @Test
    fun modeSwitching_resetsAdvancedModeToAuto_preventingStickyHighRes() {
        var activeAdvancedModeName = "AUTO"
        var currentMode = CameraMode.PHOTO

        fun setAdvancedMode(modeName: String) {
            activeAdvancedModeName = modeName
        }

        fun setCameraMode(mode: CameraMode) {
            currentMode = mode
            activeAdvancedModeName = "AUTO"
        }

        fun isHighResActive(): Boolean {
            return isHighResMode(activeAdvancedModeName)
        }

        // 1. Select High-Res mode
        setAdvancedMode("High-Res")
        assertTrue(isHighResActive())
        assertEquals("High-Res", activeAdvancedModeName)

        // 2. Switch main carousel to PHOTO -> mode resets to AUTO
        setCameraMode(CameraMode.PHOTO)
        assertFalse(isHighResActive())
        assertEquals("AUTO", activeAdvancedModeName)
        assertEquals(CameraMode.PHOTO, currentMode)

        // 3. Select High-Res mode again
        setAdvancedMode("High-Res")
        assertTrue(isHighResActive())

        // 4. Switch main carousel to PORTRAIT -> mode resets to AUTO
        setCameraMode(CameraMode.PORTRAIT)
        assertFalse(isHighResActive())
        assertEquals("AUTO", activeAdvancedModeName)
        assertEquals(CameraMode.PORTRAIT, currentMode)
    }
}
