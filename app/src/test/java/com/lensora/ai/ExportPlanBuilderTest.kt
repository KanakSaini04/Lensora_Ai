package com.lensora.ai

import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.ExportGuard
import com.lensora.ai.editor.ExportItem
import com.lensora.ai.editor.ExportItemMapper
import com.lensora.ai.editor.ExportPlanBuilder
import com.lensora.ai.editor.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportPlanBuilderTest {

    @Test
    fun buildPlan_1080pNineSixteen_calculatesCorrectResolutionAndItemDurations() {
        val clips = listOf(
            Clip("c1", "file://photo1.jpg", ClipType.PHOTO, 0L, 0L, 0L), // photo defaults to 3000ms
            Clip("c2", "file://video1.mp4", ClipType.VIDEO, 10000L, 2000L, 8000L), // 6000ms
        )
        val project = Project("p1", "Test Vlog", AspectRatio.NINE_SIXTEEN, clips)

        val plan1080 = ExportPlanBuilder.buildPlan(project, resolutionHeight = 1080)

        assertEquals(1080, plan1080.targetHeight)
        assertEquals(607, plan1080.targetWidth) // (1080 * 9) / 16 = 607
        assertEquals(2, plan1080.items.size)

        val item1 = plan1080.items[0]
        assertEquals("c1", item1.id)
        assertTrue(item1.isPhoto)
        assertEquals(3000L, item1.durationMs)

        val item2 = plan1080.items[1]
        assertEquals("c2", item2.id)
        assertFalse(item2.isPhoto)
        assertEquals(2000L, item2.startMs)
        assertEquals(8000L, item2.endMs)
        assertEquals(6000L, item2.durationMs)

        assertEquals(9000L, plan1080.totalDurationMs)
    }

    @Test
    fun buildPlan_720pSixteenNine_calculatesCorrectResolution() {
        val clips = listOf(Clip("c1", "file://v.mp4", ClipType.VIDEO, 5000L, 0L, 5000L))
        val project = Project("p2", "Landscape", AspectRatio.SIXTEEN_NINE, clips)

        val plan720 = ExportPlanBuilder.buildPlan(project, resolutionHeight = 720)

        assertEquals(720, plan720.targetHeight)
        assertEquals(1280, plan720.targetWidth) // (720 * 16) / 9 = 1280
        assertEquals(5000L, plan720.totalDurationMs)
    }

    @Test
    fun buildPlan_allAspectRatios_calculatesCorrectTargetDimensions() {
        val clip = listOf(Clip("c1", "file://v.mp4", ClipType.VIDEO, 5000L, 0L, 5000L))

        val pSquare = Project("p1", "Square", AspectRatio.ONE_ONE, clip)
        val planSquare = ExportPlanBuilder.buildPlan(pSquare, 1080)
        assertEquals(1080, planSquare.targetWidth)
        assertEquals(1080, planSquare.targetHeight)

        val pFourFive = Project("p2", "Portrait", AspectRatio.FOUR_FIVE, clip)
        val planFourFive = ExportPlanBuilder.buildPlan(pFourFive, 1080)
        assertEquals(864, planFourFive.targetWidth) // (1080 * 4) / 5 = 864
        assertEquals(1080, planFourFive.targetHeight)
    }

    @Test
    fun buildPlan_speedAdjustedClips_calculatesSpeedAdjustedTotalDuration() {
        val clips = listOf(
            Clip("c1", "file://v1.mp4", ClipType.VIDEO, 10000L, 0L, 10000L, speed = 2.0f), // 10000ms / 2.0 = 5000ms
            Clip("c2", "file://v2.mp4", ClipType.VIDEO, 6000L, 0L, 6000L, speed = 0.5f),   // 6000ms / 0.5 = 12000ms
        )
        val project = Project("p3", "Speed Test", AspectRatio.NINE_SIXTEEN, clips)

        val plan = ExportPlanBuilder.buildPlan(project, resolutionHeight = 1080)

        assertEquals(2, plan.items.size)
        assertEquals(5000L, plan.items[0].durationMs)
        assertEquals(12000L, plan.items[1].durationMs)
        assertEquals(17000L, plan.totalDurationMs)
    }

    @Test
    fun exportItemMapper_toMappingParams_mapsClipPropertiesCorrectly() {
        val item = ExportItem(
            id = "c1",
            sourceUri = "content://media/external/video/media/100",
            startMs = 1000L,
            endMs = 6000L,
            isPhoto = false,
            speed = 2.0f,
        )

        val params = ExportItemMapper.toMappingParams(item)

        assertEquals("c1", params.id)
        assertEquals("content://media/external/video/media/100", params.sourceUri)
        assertEquals(1000L, params.startMs)
        assertEquals(6000L, params.endMs)
        assertEquals(2500L, params.effectiveDurationMs)
        assertFalse(params.isPhoto)
        assertEquals(2.0f, params.speed)
    }

    @Test
    fun exportGuard_isStorageSufficient_validatesCorrectly() {
        assertFalse(ExportGuard.isStorageSufficient(1_500_000_000L)) // 1.5 GB
        assertTrue(ExportGuard.isStorageSufficient(2_000_000_000L))  // 2.0 GB
        assertTrue(ExportGuard.isStorageSufficient(5_000_000_000L))  // 5.0 GB
    }

    @Test
    fun exportGuard_isThermalSafe_validatesCorrectly() {
        assertTrue(ExportGuard.isThermalSafe(0)) // NONE
        assertTrue(ExportGuard.isThermalSafe(1)) // LIGHT
        assertTrue(ExportGuard.isThermalSafe(2)) // MODERATE
        assertFalse(ExportGuard.isThermalSafe(4)) // SEVERE
        assertFalse(ExportGuard.isThermalSafe(5)) // CRITICAL
    }
}
