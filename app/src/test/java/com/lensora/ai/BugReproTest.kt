package com.lensora.ai

import com.lensora.ai.ui.components.ZoomTransformMath
import org.junit.Assert.assertEquals
import org.junit.Test

class BugReproTest {

    @Test
    fun bugA_resolved_tappingItemN_opensItemN_correctly() {
        val photos = listOf(
            "content://media/external/images/media/1000",
            "content://media/external/images/media/1001",
            "content://media/external/images/media/1002",
            "content://media/external/images/media/1003",
        )
        val targetUri = "content://media/external/images/media/1002" // Item at index 2

        val resolvedPage = ZoomTransformMath.resolveInitialPageIndex(targetUri, photos)

        assertEquals(2, resolvedPage)
    }

    @Test
    fun bugB_resolved_pinchGesture_calculatesScaleCorrectly() {
        val currentScale = 1.0f
        val pinchZoomChange = 2.5f

        val newScale = ZoomTransformMath.calculateScale(currentScale, pinchZoomChange)

        assertEquals(2.5f, newScale, 0.001f)
    }
}
