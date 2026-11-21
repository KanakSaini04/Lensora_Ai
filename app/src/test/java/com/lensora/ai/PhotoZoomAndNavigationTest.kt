package com.lensora.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.navigation.shouldShowBottomBarForRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoZoomAndNavigationTest {

    @Test
    fun stepZ3_bottomBarVisibilityRule_hidesBarOnCameraAndShowsOnSecondaryTabs() {
        // Camera tab: bottom bar MUST be hidden (false)
        assertFalse(shouldShowBottomBarForRoute("camera"))

        // Secondary tabs: bottom bar MUST be shown (true)
        assertTrue(shouldShowBottomBarForRoute("studio"))
        assertTrue(shouldShowBottomBarForRoute("gallery"))
        assertTrue(shouldShowBottomBarForRoute("vibes"))
        assertTrue(shouldShowBottomBarForRoute("settings"))

        // Nested screens: bottom bar MUST be hidden (false)
        assertFalse(shouldShowBottomBarForRoute("quick_view"))
        assertFalse(shouldShowBottomBarForRoute("probe_lab"))
        assertFalse(shouldShowBottomBarForRoute(null))
    }

    @Test
    fun bugA_navigationResolver_resolvesTargetPhotoIndexCorrectly() {
        val photos = listOf(
            "content://media/external/images/media/1000",
            "content://media/external/images/media/1001",
            "content://media/external/images/media/1002",
            "content://media/external/images/media/1003",
        )

        // Test item N (index 2) resolution
        val targetUri = "content://media/external/images/media/1002"
        val resolvedIndex = ZoomTransformMath.resolveInitialPageIndex(targetUri, photos)
        assertEquals(2, resolvedIndex)

        // Test first item (index 0)
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex("content://media/external/images/media/1000", photos))

        // Test last item (index 3)
        assertEquals(3, ZoomTransformMath.resolveInitialPageIndex("content://media/external/images/media/1003", photos))

        // Test missing item fallback to 0
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex("content://media/external/images/media/9999", photos))

        // Test null/empty targetUri fallback to 0
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex(null, photos))
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex("", photos))
    }

    @Test
    fun uriResolutionWithSpecialCharactersAndDeletedPhoto_resolvesOrFallsBackSafely() {
        val specialUri = "content://media/external/images/media/1005 %20 test/special:path"
        val photos = listOf(
            "content://media/external/images/media/1000",
            "content://media/external/images/media/1001",
            specialUri,
            "content://media/external/images/media/1003",
        )

        // Special characters Uri resolution (index 2)
        assertEquals(2, ZoomTransformMath.resolveInitialPageIndex(specialUri, photos))

        // Deleted photo Uri fallback to 0
        val deletedUri = "content://media/external/images/media/7777_deleted"
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex(deletedUri, photos))

        // Empty list fallback to 0
        assertEquals(0, ZoomTransformMath.resolveInitialPageIndex(specialUri, emptyList()))
    }

    @Test
    fun bugB_zoomMath_calculatesScalePanAndPercentageCorrectly() {
        // Test scale clamping (1.0..8.0)
        val initialScale = 1.0f
        val zoomedScale = ZoomTransformMath.calculateScale(initialScale, 2.5f)
        assertEquals(2.5f, zoomedScale, 0.001f)

        val clampedMaxScale = ZoomTransformMath.calculateScale(zoomedScale, 10.0f)
        assertEquals(8.0f, clampedMaxScale, 0.001f)

        val clampedMinScale = ZoomTransformMath.calculateScale(1.5f, 0.1f)
        assertEquals(1.0f, clampedMinScale, 0.001f)

        // Test double-tap toggle
        assertEquals(2.0f, ZoomTransformMath.toggleDoubleTapScale(1.0f), 0.001f)
        assertEquals(1.0f, ZoomTransformMath.toggleDoubleTapScale(2.0f), 0.001f)

        // Test pan clamping
        val containerSize = Size(1080f, 1920f)
        val pan = ZoomTransformMath.calculateClampedPan(Offset.Zero, Offset(1000f, 500f), 2.0f, containerSize)

        // At scale 2.0, maxPanX = (1080 * 2 - 1080)/2 = 540f
        // maxPanY = (1920 * 2 - 1920)/2 = 960f
        assertEquals(540f, pan.x, 0.001f)
        assertEquals(500f, pan.y, 0.001f)

        // Test zoom percentage formatting
        assertEquals("100%", ZoomTransformMath.formatZoomPercentage(1.0f))
        assertEquals("250%", ZoomTransformMath.formatZoomPercentage(2.5f))
        assertEquals("800%", ZoomTransformMath.formatZoomPercentage(8.0f))
    }

    @Test
    fun studio_screenPointToImagePixelMapping_mapsAccuratelyAt400PercentZoom() {
        val containerSize = Size(1000f, 1000f)
        val imageSize = Size(1000f, 1000f)
        val scale = 4.0f // 400% zoom
        val pan = Offset(0f, 0f)

        // Center point on screen (500, 500)
        val centerPixel = ZoomTransformMath.mapScreenPointToImagePixel(Offset(500f, 500f), containerSize, imageSize, scale, pan)
        assertEquals(500f, centerPixel.x, 0.01f)
        assertEquals(500f, centerPixel.y, 0.01f)
    }
}
