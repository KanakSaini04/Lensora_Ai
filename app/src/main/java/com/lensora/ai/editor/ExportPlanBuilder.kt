package com.lensora.ai.editor

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation

data class ExportItem(
    val id: String,
    val sourceUri: String,
    val startMs: Long,
    val endMs: Long,
    val isPhoto: Boolean,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val vibePreset: String? = null,
    val textOverlay: String? = null,
) {
    val durationMs: Long
        get() = ((endMs - startMs).coerceAtLeast(0L) / if (speed > 0f) speed else 1.0f).toLong()
}

data class ClipMappingParams(
    val id: String,
    val sourceUri: String,
    val startMs: Long,
    val endMs: Long,
    val effectiveDurationMs: Long,
    val isPhoto: Boolean,
    val speed: Float,
    val volume: Float = 1.0f,
    val vibePreset: String? = null,
    val textOverlay: String? = null,
)

object ExportItemMapper {
    fun toMappingParams(item: ExportItem): ClipMappingParams {
        return ClipMappingParams(
            id = item.id,
            sourceUri = item.sourceUri,
            startMs = item.startMs,
            endMs = item.endMs,
            effectiveDurationMs = item.durationMs,
            isPhoto = item.isPhoto,
            speed = item.speed,
            volume = item.volume,
            vibePreset = item.vibePreset,
            textOverlay = item.textOverlay,
        )
    }
}

data class ExportPlan(
    val projectId: String,
    val projectName: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val items: List<ExportItem>,
    val totalDurationMs: Long,
)

object ExportGuard {
    const val MIN_REQUIRED_BYTES: Long = 200_000_000L // 200MB Guard Limit

    fun isStorageSufficient(availableBytes: Long, minRequiredBytes: Long = MIN_REQUIRED_BYTES): Boolean {
        return availableBytes >= minRequiredBytes
    }

    fun isThermalSafe(thermalStatus: Int): Boolean {
        // ThermalStatus >= 4 corresponds to SEVERE / CRITICAL / EMERGENCY
        return thermalStatus < 4
    }
}

@OptIn(UnstableApi::class)
object ExportPlanBuilder {
    const val ASPECT_MODE: Int = Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP

    fun buildPlan(project: Project, resolutionHeight: Int = 1080): ExportPlan {
        val targetHeight = if (resolutionHeight == 720) 720 else 1080
        var targetWidth = when (project.aspect) {
            AspectRatio.NINE_SIXTEEN -> (targetHeight * 9) / 16
            AspectRatio.SIXTEEN_NINE -> (targetHeight * 16) / 9
            AspectRatio.ONE_ONE -> targetHeight
            AspectRatio.FOUR_FIVE -> (targetHeight * 4) / 5
        }
        if (targetWidth % 2 != 0) {
            targetWidth += 1 // Coerce to even number for H.264 video encoder compatibility
        }

        val items = project.clips.map { clip ->
            val totalMs = if (clip.durationMs <= 0L && clip.type == ClipType.PHOTO) {
                TimelineEditEngine.DEFAULT_PHOTO_DURATION_MS
            } else {
                clip.durationMs
            }

            val start = clip.trimStartMs.coerceIn(0L, totalMs)
            val end = if (clip.trimEndMs <= 0L && clip.type == ClipType.PHOTO) totalMs else clip.trimEndMs.coerceIn(start, totalMs)

            ExportItem(
                id = clip.id,
                sourceUri = clip.sourceUri,
                startMs = start,
                endMs = end,
                isPhoto = clip.type == ClipType.PHOTO,
                speed = clip.speed,
                volume = clip.volume,
                vibePreset = clip.vibePreset,
                textOverlay = clip.textOverlay,
            )
        }

        val totalMs = items.sumOf { it.durationMs }

        return ExportPlan(
            projectId = project.id,
            projectName = project.name,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            items = items,
            totalDurationMs = totalMs,
        )
    }
}
