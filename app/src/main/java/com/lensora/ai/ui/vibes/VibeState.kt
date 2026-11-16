package com.lensora.ai.ui.vibes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class VibePreset(
    val id: String,
    val displayName: String,
    val description: String,
) {
    NONE("none", "None", "Default Natural Color"),
    CINEMATIC("cinematic", "Cinematic", "High Contrast Teal & Red Shift"),
    LEICA("leica", "Leica B&W", "Monochromatic High Contrast"),
    VINTAGE("vintage", "Vintage Teal", "Warm Tint & Lifted Blacks"),
    ROYAL_GOLD("royal_gold", "Royal Gold", "Warm Golden Glow & Shadows"),
}

object VibeState {
    var selectedVibe by mutableStateOf(VibePreset.NONE)
}
