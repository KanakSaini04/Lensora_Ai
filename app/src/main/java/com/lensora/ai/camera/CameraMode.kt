package com.lensora.ai.camera

enum class CameraMode(val displayName: String) {
    AI_AUTO("\u2728 AI AUTO"),
    PHOTO("PHOTO"),
    PORTRAIT("PORTRAIT"),
    VIDEO("VIDEO"),
    PRO("PRO");

    companion object {
        val modes = entries.toList()
    }
}
