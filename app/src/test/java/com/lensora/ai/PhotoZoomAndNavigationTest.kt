package com.lensora.ai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.lensora.ai.ui.components.StrokePaintStyle
import com.lensora.ai.ui.components.TouchMode
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.navigation.shouldShowBottomBarForRoute
import com.lensora.ai.ui.studio.StudioPrimitiveState
import com.lensora.ai.ui.studio.StudioStateSaver
import com.lensora.ai.ui.studio.StudioStroke
import com.lensora.ai.ui.studio.StrokeHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoZoomAndNavigationTest {

    @Test
    fun stepZ6b_pointMappingAndRadiusConversion_with3060x4080ImageIn1080x1400Canvas() {
        val imageSize = Size(3060f, 4080f) // 3060x4080
        val canvasSize = Size(1080f, 1400f) // 1080x1400

        // Letterbox scaleFactor = minOf(1080 / 3060, 1400 / 4080) = minOf(0.352941, 0.343137) = 0.34313725f
        val expectedScaleFactor = 0.34313725f

        // Test Canvas point mapping to image pixel
        val canvasCenter = Offset(540f, 700f)
        val imagePixel = ZoomTransformMath.mapCanvasLocalPointToImagePixel(canvasCenter, canvasSize, imageSize)

        // Drawn height = 4080 * 0.34313725 = 1400f
        // Drawn width = 3060 * 0.34313725 = 1050f
        // imageLeft = (1080 - 1050) / 2 = 15f
        // imagePixel.x = (540 - 15) / 0.34313725 = 525 / 0.34313725 = 1530f
        // imagePixel.y = (700 - 0) / 0.34313725 = 700 / 0.34313725 = 2040f
        assertEquals(1530f, imagePixel.x, 0.1f)
        assertEquals(2040f, imagePixel.y, 0.1f)

        // Test Canvas-local brush radius conversion to image space radius
        val density = 2.75f // e.g. 440 dpi
        val screenDp = 40f
        val localRadius1x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenDp, density, 1.0f) / 2f
        assertEquals(55f, localRadius1x, 0.01f)

        val imageRadius1x = ZoomTransformMath.calculateImageRadiusFromCanvasPx(localRadius1x, canvasSize, imageSize)
        assertEquals(55f / expectedScaleFactor, imageRadius1x, 0.1f) // ~160.28 px in image space

        // At 1.4x scale
        val localRadius1_4x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenDp = screenDp, density = density, scale = 1.4f) / 2f
        assertEquals(39.2857f, localRadius1_4x, 0.01f)

        // At 4.0x scale
        val localRadius4x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenDp = screenDp, density = density, scale = 4.0f) / 2f
        assertEquals(13.75f, localRadius4x, 0.01f)

        // At 8.0x scale
        val localRadius8x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenDp = screenDp, density = density, scale = 8.0f) / 2f
        assertEquals(6.875f, localRadius8x, 0.01f)
    }

    @Test
    fun stepZ6b_decideStrokePaintStyle_singleTapVsLineStrokeDecisions() {
        // Single-tap stroke (1 point) -> FILL
        assertEquals(StrokePaintStyle.FILL, ZoomTransformMath.decideStrokePaintStyle(1))

        // Line stroke (2+ points) -> STROKE
        assertEquals(StrokePaintStyle.STROKE, ZoomTransformMath.decideStrokePaintStyle(2))
        assertEquals(StrokePaintStyle.STROKE, ZoomTransformMath.decideStrokePaintStyle(50))

        // Zero points -> NONE
        assertEquals(StrokePaintStyle.NONE, ZoomTransformMath.decideStrokePaintStyle(0))
    }

    @Test
    fun stepZ6_brushRadiusAndConstantScreenSpaceMath_calculatesCorrectlyAcrossScales() {
        val density = 2.5f
        val screenBrushDp = 40.0f // 40dp

        // At 1.0x scale: local width = 100px, image radius (scaleFactor 1.0) = 50px
        val localWidth1x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenBrushDp, density, 1.0f)
        assertEquals(100f, localWidth1x, 0.01f)
        val imageRadius1x = ZoomTransformMath.calculateImageRadiusFromCanvasPx(50f, Size(1000f, 1000f), Size(1000f, 1000f))
        assertEquals(50f, imageRadius1x, 0.01f)

        // At 1.4x scale: local width = 71.428f px (when scaled by 1.4x graphicsLayer, screen width remains 100px!)
        val localWidth1_4x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenBrushDp, density, 1.4f)
        assertEquals(71.42857f, localWidth1_4x, 0.01f)

        // At 4.0x scale: local width = 25px
        val localWidth4x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenBrushDp, density, 4.0f)
        assertEquals(25f, localWidth4x, 0.01f)

        // At 8.0x scale: local width = 12.5px
        val localWidth8x = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(screenBrushDp, density, 8.0f)
        assertEquals(12.5f, localWidth8x, 0.01f)
    }

    @Test
    fun stepZ6_strokeHistory_clampsTo25StepsAndHandlesUndoRedoClear() {
        val history = StrokeHistory(maxUndoSteps = 25)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        // Add 30 strokes
        for (i in 1..30) {
            history.addStroke(
                StudioStroke(
                    points = listOf(Offset(i.toFloat(), i.toFloat()), Offset(i + 1f, i + 1f)),
                    isEraseMode = true,
                    radiusCanvasPx = 20f,
                ),
            )
        }

        // Must be clamped to exactly 25 strokes
        assertEquals(25, history.strokes.size)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)

        // Perform Undo
        val undoSuccess = history.undo()
        assertTrue(undoSuccess)
        assertEquals(24, history.strokes.size)
        assertTrue(history.canRedo)

        // Perform Redo
        val redoSuccess = history.redo()
        assertTrue(redoSuccess)
        assertEquals(25, history.strokes.size)

        // Condition 3 & 4: Clear mask and history (on photo switch or process eraser pass)
        history.clear()
        assertEquals(0, history.strokes.size)
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun stepZ6_studioStateSaver_serializesAndDeserializesSmallPrimitiveValues() {
        val originalState = StudioPrimitiveState(
            selectedToolCategory = 2,
            exposureValue = 1.25f,
            contrastValue = -15.0f,
            brushSizeDp = 45.0f,
            opacityFloat = 0.65f,
            isEraseModeActive = false,
        )

        // Save state to Bundle list
        val savedBundleList = with(StudioStateSaver) { dummySaverScope.save(originalState) }
        assertTrue(savedBundleList is List<*>)

        // Restore state from Bundle list
        val restoredState = StudioStateSaver.restore(savedBundleList!!)
        assertEquals(2, restoredState!!.selectedToolCategory)
        assertEquals(1.25f, restoredState.exposureValue, 0.001f)
        assertEquals(-15.0f, restoredState.contrastValue, 0.001f)
        assertEquals(45.0f, restoredState.brushSizeDp, 0.001f)
        assertEquals(0.65f, restoredState.opacityFloat, 0.001f)
        assertFalse(restoredState.isEraseModeActive)
    }

    @Test
    fun stepZ6_maskAccuracy_canvasLocalPointToImagePixel_mapsAccurately() {
        val canvasSize = Size(1000f, 1000f)
        val imageSize = Size(500f, 500f) // 1:1 aspect ratio, letterbox scaleFactor = 2.0

        // Canvas local center point (500, 500) -> image pixel (250, 250)
        val imagePixel = ZoomTransformMath.mapCanvasLocalPointToImagePixel(Offset(500f, 500f), canvasSize, imageSize)
        assertEquals(250f, imagePixel.x, 0.01f)
        assertEquals(250f, imagePixel.y, 0.01f)

        // Canvas local point (200, 300) -> image pixel (100, 150)
        val offCenterPixel = ZoomTransformMath.mapCanvasLocalPointToImagePixel(Offset(200f, 300f), canvasSize, imageSize)
        assertEquals(100f, offCenterPixel.x, 0.01f)
        assertEquals(150f, offCenterPixel.y, 0.01f)
    }

    private val dummySaverScope = object : androidx.compose.runtime.saveable.SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

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
