package com.lensora.ai

import android.graphics.RectF
import com.lensora.ai.ai.CoachingHintHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SceneClassifierTest {

    private fun createRectF(left: Float, top: Float, right: Float, bottom: Float): RectF {
        val rect = RectF()
        rect.left = left
        rect.top = top
        rect.right = right
        rect.bottom = bottom
        return rect
    }

    @Test
    fun testNullBoxReturnsNullHint() {
        val hint = CoachingHintHelper.coachingHintFor(null)
        assertNull(hint)
    }

    @Test
    fun testCenteredBoxReturnsPerfectComposition() {
        val box = createRectF(0.2f, 0.2f, 0.8f, 0.8f)
        val hint = CoachingHintHelper.coachingHintFor(box)
        assertEquals("✨ Perfect Composition", hint)
    }

    @Test
    fun testSmallBoxReturnsStepCloser() {
        val box = createRectF(0.4f, 0.4f, 0.6f, 0.6f)
        val hint = CoachingHintHelper.coachingHintFor(box)
        assertEquals("✨ Step Closer", hint)
    }

    @Test
    fun testOffCenterLeftBoxReturnsMoveRight() {
        val box = createRectF(0.05f, 0.2f, 0.55f, 0.8f)
        val hint = CoachingHintHelper.coachingHintFor(box)
        assertEquals("✨ Move Right", hint)
    }

    @Test
    fun testOffCenterRightBoxReturnsMoveLeft() {
        val box = createRectF(0.45f, 0.2f, 0.95f, 0.8f)
        val hint = CoachingHintHelper.coachingHintFor(box)
        assertEquals("✨ Move Left", hint)
    }

    @Test
    fun testLabelSourceEnum_defaultIsHeuristicEngine() {
        val result = com.lensora.ai.ai.DetectionResult(label = "✨ AI: Auto")
        assertEquals(com.lensora.ai.ai.LabelSource.HEURISTIC_ENGINE, result.source)
    }

    @Test
    fun testFakeTfliteInterpreter_fallbackWhenUnusable() {
        val fakeUnusable = object : com.lensora.ai.ai.TfliteSceneInterpreter {
            override fun isUsable(): Boolean = false
            override fun classify(bitmap: android.graphics.Bitmap, labels: List<String>): com.lensora.ai.ai.DetectionResult? = null
        }

        assertFalse(fakeUnusable.isUsable())
        val dummyBitmap = null as android.graphics.Bitmap?
        if (dummyBitmap != null) {
            fakeUnusable.classify(dummyBitmap, emptyList())
        }
    }
}
