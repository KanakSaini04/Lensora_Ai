package com.lensora.ai.ai

import android.hardware.camera2.CaptureRequest
import android.util.Log
import java.util.Locale

enum class SceneType {
    PORTRAIT,
    LANDSCAPE,
    NIGHT,
    SKY,
    AUTO
}

data class ProcessingPlan(
    val sceneType: SceneType,
    val targetEvOffset: Float,
    val targetAfMode: Int,
    val isoTarget: Int? = null,
    val focusDistance: Float? = null,
    val faceAeLock: Boolean = false,
)

class AiPlanner {
    private val tag = "AiPlanner"

    fun createPlan(sceneType: SceneType): ProcessingPlan {
        val plan = when (sceneType) {
            SceneType.PORTRAIT -> ProcessingPlan(
                sceneType = SceneType.PORTRAIT,
                targetEvOffset = 0.3f,
                targetAfMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
                faceAeLock = true,
            )
            SceneType.LANDSCAPE -> ProcessingPlan(
                sceneType = SceneType.LANDSCAPE,
                targetEvOffset = -0.2f,
                targetAfMode = CaptureRequest.CONTROL_AF_MODE_OFF,
                isoTarget = 100,
                focusDistance = 0.0f, // Infinity focus
            )
            SceneType.NIGHT -> ProcessingPlan(
                sceneType = SceneType.NIGHT,
                targetEvOffset = 1.0f,
                targetAfMode = CaptureRequest.CONTROL_AF_MODE_AUTO,
            )
            SceneType.SKY -> ProcessingPlan(
                sceneType = SceneType.SKY,
                targetEvOffset = -0.1f,
                targetAfMode = CaptureRequest.CONTROL_AF_MODE_AUTO,
            )
            SceneType.AUTO -> ProcessingPlan(
                sceneType = SceneType.AUTO,
                targetEvOffset = 0.0f,
                targetAfMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            )
        }

        if (plan.isoTarget != null || plan.focusDistance != null || plan.faceAeLock) {
            Log.i(
                tag,
                "L2 Hardware Tuning: ${plan.sceneType} detected (ISO: ${plan.isoTarget ?: "Auto"}, FocusDist: ${plan.focusDistance ?: "Auto"}, FaceAE: ${plan.faceAeLock})",
            )
        } else {
            Log.i(
                tag,
                "Applied ${plan.sceneType} plan: EV ${String.format(Locale.US, "%+.1f", plan.targetEvOffset)}, AF Mode ${plan.targetAfMode}",
            )
        }
        return plan
    }

    companion object {
        fun fromLabel(label: String): SceneType {
            return when {
                label.contains("Portrait", ignoreCase = true) -> SceneType.PORTRAIT
                label.contains("Landscape", ignoreCase = true) -> SceneType.LANDSCAPE
                label.contains("Night", ignoreCase = true) -> SceneType.NIGHT
                label.contains("Sky", ignoreCase = true) -> SceneType.SKY
                else -> SceneType.AUTO
            }
        }
    }
}
