package com.lensora.ai.ui.studio

import androidx.compose.ui.graphics.ColorMatrix

object ImageEditor {

    fun createColorMatrix(
        exposure: Float,
        contrast: Float,
    ): ColorMatrix {
        // Exposure gain scaling (-2.0 EV to +2.0 EV)
        val evScale = 1.0f + (exposure * 0.25f)

        // Contrast scaling (-50 to +50 -> 0.5x to 1.5x)
        val contrastScale = 1.0f + (contrast / 50.0f) * 0.5f

        val combinedScale = contrastScale * evScale
        val translate = 128f * (1f - contrastScale) + (exposure * 20f)

        return ColorMatrix(
            floatArrayOf(
                combinedScale, 0f, 0f, 0f, translate,
                0f, combinedScale, 0f, 0f, translate,
                0f, 0f, combinedScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
