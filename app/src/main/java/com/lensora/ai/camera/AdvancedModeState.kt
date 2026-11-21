package com.lensora.ai.camera

class AdvancedModeState {
    var activeModeName: String = "AUTO"
        private set

    fun selectAdvancedMode(modeName: String) {
        activeModeName = modeName
    }

    fun onCarouselModeChanged() {
        activeModeName = "AUTO"
    }

    fun isHighResActive(): Boolean {
        return activeModeName.equals("High-Res", ignoreCase = true) || activeModeName.equals("HIGH-RES", ignoreCase = true)
    }
}
