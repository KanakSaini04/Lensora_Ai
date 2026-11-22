package com.lensora.ai.editor

import kotlinx.serialization.Serializable

@Serializable
enum class AspectRatio {
    NINE_SIXTEEN,
    SIXTEEN_NINE,
    ONE_ONE,
    FOUR_FIVE,
}

@Serializable
enum class ClipType {
    PHOTO,
    VIDEO,
}

@Serializable
enum class TransitionType {
    NONE,
    FADE,
    ZOOM,
    SLIDE,
    GLITCH,
}

@Serializable
data class Clip(
    val id: String,
    val sourceUri: String,
    val type: ClipType,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val vibePreset: String? = null,
    val transitionIn: TransitionType = TransitionType.NONE,
    val textOverlay: String? = null,
) {
    fun validate(): Boolean {
        return durationMs > 0 &&
                trimStartMs >= 0 &&
                trimEndMs > trimStartMs &&
                speed in 0.25f..4.0f &&
                volume in 0.0f..1.0f
    }
}

@Serializable
data class MusicTrack(
    val uri: String,
    val startMs: Long = 0L,
    val volume: Float = 1.0f,
) {
    fun validate(): Boolean {
        return startMs >= 0L && volume in 0.0f..1.0f
    }
}

@Serializable
data class Project(
    val id: String,
    val name: String,
    val aspect: AspectRatio,
    val clips: List<Clip>,
    val music: MusicTrack? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun validate(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                clips.isNotEmpty() &&
                clips.all { it.validate() } &&
                (music == null || music.validate())
    }
}

@Serializable
data class TemplateSlot(
    val durationMs: Long,
    val vibePreset: String? = null,
    val transitionIn: TransitionType = TransitionType.NONE,
    val text: String? = null,
) {
    fun validate(): Boolean {
        return durationMs > 0
    }
}

@Serializable
data class Template(
    val id: String,
    val name: String,
    val aspect: AspectRatio,
    val musicAsset: String? = null,
    val slots: List<TemplateSlot>,
) {
    fun validate(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                slots.isNotEmpty() &&
                slots.all { it.validate() }
    }
}
