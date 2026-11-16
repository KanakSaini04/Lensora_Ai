package com.lensora.ai

import com.lensora.ai.ai.AiPlanner
import com.lensora.ai.ai.SceneType
import com.lensora.ai.ui.studio.ImageEditor
import com.lensora.ai.ui.vibes.VibePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPlannerTest {

    private val aiPlanner = AiPlanner()

    @Test
    fun testPortraitPlanGeneration() {
        val plan = aiPlanner.createPlan(SceneType.PORTRAIT)
        assertEquals(SceneType.PORTRAIT, plan.sceneType)
        assertEquals(0.3f, plan.targetEvOffset, 0.01f)
        assertTrue(plan.faceAeLock)
    }

    @Test
    fun testLandscapePlanGeneration() {
        val plan = aiPlanner.createPlan(SceneType.LANDSCAPE)
        assertEquals(SceneType.LANDSCAPE, plan.sceneType)
        assertEquals(-0.2f, plan.targetEvOffset, 0.01f)
        assertEquals(100, plan.isoTarget)
        assertEquals(0.0f, plan.focusDistance!!, 0.01f)
    }

    @Test
    fun testNightPlanGeneration() {
        val plan = aiPlanner.createPlan(SceneType.NIGHT)
        assertEquals(SceneType.NIGHT, plan.sceneType)
        assertEquals(1.0f, plan.targetEvOffset, 0.01f)
    }

    @Test
    fun testSceneTypeFromLabel() {
        assertEquals(SceneType.PORTRAIT, AiPlanner.fromLabel("✨ AI: Portrait Detected"))
        assertEquals(SceneType.LANDSCAPE, AiPlanner.fromLabel("✨ AI: Landscape Scene"))
        assertEquals(SceneType.NIGHT, AiPlanner.fromLabel("✨ AI: Night Vision"))
        assertEquals(SceneType.SKY, AiPlanner.fromLabel("✨ AI: Sky Blue"))
        assertEquals(SceneType.AUTO, AiPlanner.fromLabel("✨ AI: Auto Mode"))
    }

    @Test
    fun testImageEditorColorMatrixMath() {
        val baseMatrix = ImageEditor.createAndroidColorMatrix(exposure = 1.0f, contrast = 25.0f)
        assertNotNull(baseMatrix)

        val vibeMatrix = ImageEditor.createVibeAndroidColorMatrix(VibePreset.ROYAL_GOLD)
        assertNotNull(vibeMatrix)
    }
}
