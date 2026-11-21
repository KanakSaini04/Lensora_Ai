package com.lensora.ai

import androidx.compose.ui.geometry.Rect
import com.lensora.ai.ui.components.CameraSwipeDecision
import com.lensora.ai.ui.components.ZoomTransformMath
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSwipeGestureTest {

    private val screenWidth = 1080f
    private val screenHeight = 2400f
    private val topBarRect = Rect(0f, 0f, 1080f, 120f)
    private val aiBadgeRect = Rect(400f, 130f, 680f, 180f)
    private val zoomPillsRect = Rect(300f, 1950f, 780f, 2020f)
    private val carouselRect = Rect(0f, 2030f, 1080f, 2150f)
    private val shutterRowRect = Rect(0f, 2160f, 1080f, 2350f)
    private val excludedRects = listOf(topBarRect, aiBadgeRect, zoomPillsRect, carouselRect, shutterRowRect)

    @Test
    fun validLeftSwipe_navigatesToStudio() {
        // Valid left swipe: start in middle viewfinder (500, 1000), totalDx = -300f (>= 15% of 1080 = 162f), totalDy = 10f, 1 pointer
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f,
            startY = 1000f,
            totalDx = -300f,
            totalDy = 10f,
            pointerCount = 1,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            edgeInsetPx = 60f,
            excludedRects = excludedRects,
        )
        assertEquals(CameraSwipeDecision.NAVIGATE_NEXT_TAB, decision)
    }

    @Test
    fun rightSwipe_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = 300f, totalDy = 10f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun verticalUpSwipe_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -50f, totalDy = -300f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun verticalDownSwipe_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -50f, totalDy = 300f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun diagonalSwipe_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -300f, totalDy = -200f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun shortSwipe_below15PercentThreshold_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -100f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun twoPointers_pinch_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -300f, totalDy = 0f, pointerCount = 2,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInLeftEdgeInset_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 20f, startY = 1000f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInRightEdgeInset_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 1050f, startY = 1000f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInTopBarRect_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 50f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInAiBadgeRect_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 150f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInZoomPillsRect_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1980f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInModeCarouselRect_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 2080f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun startInShutterRowRect_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 2200f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = screenWidth, screenHeight = screenHeight, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }

    @Test
    fun zeroScreenWidthOrHeight_returnsNone() {
        val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
            startX = 500f, startY = 1000f, totalDx = -300f, totalDy = 0f, pointerCount = 1,
            screenWidth = 0f, screenHeight = 0f, edgeInsetPx = 60f, excludedRects = excludedRects
        )
        assertEquals(CameraSwipeDecision.NONE, decision)
    }
}
