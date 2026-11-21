package com.lensora.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.lensora.ai.ui.components.TouchMode
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.navigation.shouldShowBottomBarForRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoZoomAndNavigationTest {

    @Test
    fun stepZ5_decideTouchMode_evaluatesCorrectModeAcrossAllScalesAndTools() {
        // 1 finger + Eraser active in Studio at scales 1.0, 1.29, 1.4, 4.0, 8.0 -> DRAW
        assertEquals(TouchMode.DRAW, ZoomTransformMath.decideTouchMode(1, 1.0f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.DRAW, ZoomTransformMath.decideTouchMode(1, 1.29f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.DRAW, ZoomTransformMath.decideTouchMode(1, 1.4f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.DRAW, ZoomTransformMath.decideTouchMode(1, 4.0f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.DRAW, ZoomTransformMath.decideTouchMode(1, 8.0f, isDrawToolActive = true, isStudio = true))

        // 2 fingers -> ZOOM_PAN
        assertEquals(TouchMode.ZOOM_PAN, ZoomTransformMath.decideTouchMode(2, 1.0f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.ZOOM_PAN, ZoomTransformMath.decideTouchMode(2, 4.0f, isDrawToolActive = false, isStudio = true))

        // 1 finger + Crop/Light/Color tool in Studio at scale > 1 -> PAN
        assertEquals(TouchMode.PAN, ZoomTransformMath.decideTouchMode(1, 1.4f, isDrawToolActive = false, isStudio = true))

        // Gallery context 1 finger scale > 1 -> PAN
        assertEquals(TouchMode.PAN, ZoomTransformMath.decideTouchMode(1, 2.0f, isDrawToolActive = false, isStudio = false))

        // Scale 1.0 non-draw -> NONE
        assertEquals(TouchMode.NONE, ZoomTransformMath.decideTouchMode(1, 1.0f, isDrawToolActive = false, isStudio = true))

        // 3 pointers -> ZOOM_PAN
        assertEquals(TouchMode.ZOOM_PAN, ZoomTransformMath.decideTouchMode(3, 2.0f, isDrawToolActive = true, isStudio = true))

        // Zero / negative values -> NONE (no crash)
        assertEquals(TouchMode.NONE, ZoomTransformMath.decideTouchMode(0, 1.0f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.NONE, ZoomTransformMath.decideTouchMode(1, 0f, isDrawToolActive = true, isStudio = true))
        assertEquals(TouchMode.NONE, ZoomTransformMath.decideTouchMode(-1, -2.0f, isDrawToolActive = true, isStudio = true))
    }

    @Test
    fun studio_screenPointToImagePixelMapping_mapsAccuratelyAcrossScalesAndPanOffsets() {
        val containerSize = Size(1000f, 1000f)
        val imageSize = Size(1000f, 1000f)

        // 1.0x scale, zero pan: center point (500, 500) -> image pixel (500, 500)
        var px = ZoomTransformMath.mapScreenPointToImagePixel(Offset(500f, 500f), containerSize, imageSize, 1.0f, Offset.Zero)
        assertEquals(500f, px.x, 0.01f)
        assertEquals(500f, px.y, 0.01f)

        // 1.4x scale, zero pan: center point (500, 500) -> image pixel (500, 500)
        px = ZoomTransformMath.mapScreenPointToImagePixel(Offset(500f, 500f), containerSize, imageSize, 1.4f, Offset.Zero)
        assertEquals(500f, px.x, 0.01f)
        assertEquals(500f, px.y, 0.01f)

        // 4.0x scale, zero pan: center point (500, 500) -> image pixel (500, 500)
        px = ZoomTransformMath.mapScreenPointToImagePixel(Offset(500f, 500f), containerSize, imageSize, 4.0f, Offset.Zero)
        assertEquals(500f, px.x, 0.01f)
        assertEquals(500f, px.y, 0.01f)

        // 8.0x scale, zero pan: center point (500, 500) -> image pixel (500, 500)
        px = ZoomTransformMath.mapScreenPointToImagePixel(Offset(500f, 500f), containerSize, imageSize, 8.0f, Offset.Zero)
        assertEquals(500f, px.x, 0.01f)
        assertEquals(500f, px.y, 0.01f)

        // 2.0x scale, pan Offset(100, 50): screen point (600, 550) -> image pixel (500, 500)
        px = ZoomTransformMath.mapScreenPointToImagePixel(Offset(600f, 550f), containerSize, imageSize, 2.0f, Offset(100f, 50f))
        assertEquals(500f, px.x, 0.01f)
        assertEquals(500f, px.y, 0.01f)
    }

    @Test
    fun studio_offCenterMappingAndPanClamping_evaluatesExactCoordinates() {
        val containerSize = Size(1000f, 1000f)
        val imageSize = Size(1000f, 1000f)
        val screenPoint = Offset(200f, 300f)

        // Off-center point (200, 300) at 1.4x scale, zero pan
        var px = ZoomTransformMath.mapScreenPointToImagePixel(screenPoint, containerSize, imageSize, 1.4f, Offset.Zero)
        assertEquals(285.714f, px.x, 0.01f)
        assertEquals(357.143f, px.y, 0.01f)

        // Off-center point (200, 300) at 4.0x scale, zero pan
        px = ZoomTransformMath.mapScreenPointToImagePixel(screenPoint, containerSize, imageSize, 4.0f, Offset.Zero)
        assertEquals(425f, px.x, 0.01f)
        assertEquals(450f, px.y, 0.01f)

        // Off-center point (200, 300) at 8.0x scale, zero pan
        px = ZoomTransformMath.mapScreenPointToImagePixel(screenPoint, containerSize, imageSize, 8.0f, Offset.Zero)
        assertEquals(462.5f, px.x, 0.01f)
        assertEquals(475f, px.y, 0.01f)

        // Off-center point (200, 300) at 4.0x scale with pan Offset(100f, -50f)
        px = ZoomTransformMath.mapScreenPointToImagePixel(screenPoint, containerSize, imageSize, 4.0f, Offset(100f, -50f))
        assertEquals(400f, px.x, 0.01f)
        assertEquals(462.5f, px.y, 0.01f)

        // Pan clamping tests
        // 1.0x scale: pan clamped to Offset.Zero
        val pan1x = ZoomTransformMath.calculateClampedPan(Offset.Zero, Offset(500f, 500f), 1.0f, containerSize)
        assertEquals(Offset.Zero, pan1x)

        // 2.0x scale: maxPan = 500f
        val pan2x = ZoomTransformMath.calculateClampedPan(Offset.Zero, Offset(2000f, -2000f), 2.0f, containerSize)
        assertEquals(500f, pan2x.x, 0.01f)
        assertEquals(-500f, pan2x.y, 0.01f)
    }

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
}
