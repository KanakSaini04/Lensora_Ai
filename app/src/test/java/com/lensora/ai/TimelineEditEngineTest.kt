package com.lensora.ai

import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.TimelineEditEngine
import com.lensora.ai.editor.TimelineUndoRedoStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineEditEngineTest {

    @Test
    fun trim_validBounds_updatesStartAndEndMs() {
        val clip = Clip("c1", "uri1", ClipType.VIDEO, 10000L, 0L, 10000L)
        val trimmed = TimelineEditEngine.trim(clip, 2000L, 8000L)

        assertEquals(2000L, trimmed.trimStartMs)
        assertEquals(8000L, trimmed.trimEndMs)
    }

    @Test
    fun trim_zeroLengthOrInvalidBounds_returnsOriginalClipUnchanged() {
        val clip = Clip("c1", "uri1", ClipType.VIDEO, 10000L, 0L, 10000L)

        val invalid1 = TimelineEditEngine.trim(clip, 5000L, 5000L)
        assertEquals(clip, invalid1)

        val invalid2 = TimelineEditEngine.trim(clip, 8000L, 2000L)
        assertEquals(clip, invalid2)
    }

    @Test
    fun split_validTimestamp_returnsTwoClipsWithCorrectSpan() {
        val clip = Clip("c1", "uri1", ClipType.VIDEO, 10000L, 0L, 10000L)
        val (clipA, clipB) = TimelineEditEngine.split(clip, 4000L)

        assertEquals("c1_split_a", clipA.id)
        assertEquals(0L, clipA.trimStartMs)
        assertEquals(4000L, clipA.trimEndMs)

        assertEquals("c1_split_b", clipB.id)
        assertEquals(4000L, clipB.trimStartMs)
        assertEquals(10000L, clipB.trimEndMs)
    }

    @Test
    fun split_atEdges_returnsOriginalClipPairUnchanged() {
        val clip = Clip("c1", "uri1", ClipType.VIDEO, 10000L, 0L, 10000L)

        val (clipA, clipB) = TimelineEditEngine.split(clip, 5L)
        assertEquals(clip, clipA)
        assertEquals(clip, clipB)

        val (clipC, clipD) = TimelineEditEngine.split(clip, 9995L)
        assertEquals(clip, clipC)
        assertEquals(clip, clipD)
    }

    @Test
    fun reorder_validIndices_movesClipToTargetIndex() {
        val clips = listOf(
            Clip("c1", "uri1", ClipType.PHOTO, 3000L),
            Clip("c2", "uri2", ClipType.VIDEO, 5000L),
            Clip("c3", "uri3", ClipType.PHOTO, 3000L),
        )

        val reordered = TimelineEditEngine.reorder(clips, 0, 2)
        assertEquals("c2", reordered[0].id)
        assertEquals("c3", reordered[1].id)
        assertEquals("c1", reordered[2].id)
    }

    @Test
    fun reorder_outOfBoundsIndices_returnsOriginalListUnchanged() {
        val clips = listOf(
            Clip("c1", "uri1", ClipType.PHOTO, 3000L),
            Clip("c2", "uri2", ClipType.VIDEO, 5000L),
        )

        val result1 = TimelineEditEngine.reorder(clips, -1, 1)
        assertEquals(clips, result1)

        val result2 = TimelineEditEngine.reorder(clips, 0, 10)
        assertEquals(clips, result2)
    }

    @Test
    fun delete_presentAndMissingId_handlesBothCases() {
        val clips = listOf(
            Clip("c1", "uri1", ClipType.PHOTO, 3000L),
            Clip("c2", "uri2", ClipType.VIDEO, 5000L),
        )

        val deleted = TimelineEditEngine.delete(clips, "c1")
        assertEquals(1, deleted.size)
        assertEquals("c2", deleted[0].id)

        val missing = TimelineEditEngine.delete(clips, "non_existent")
        assertEquals(2, missing.size)
        assertEquals(clips, missing)
    }

    @Test
    fun totalDurationMs_calculatesCorrectTotalDuration() {
        val clips = listOf(
            Clip("c1", "uri1", ClipType.PHOTO, 0L, 0L, 0L), // photo defaults to 3000ms
            Clip("c2", "uri2", ClipType.VIDEO, 10000L, 2000L, 8000L, speed = 2.0f), // 6000ms / 2.0 = 3000ms
        )
        val project = Project("p1", "Test", AspectRatio.NINE_SIXTEEN, clips)

        val total = TimelineEditEngine.totalDurationMs(project)
        assertEquals(6000L, total)
    }

    @Test
    fun undoRedoStack_pushUndoRedo_managesStateHistoryCorrectly() {
        val stack = TimelineUndoRedoStack(maxHistory = 50)

        val state1 = listOf(Clip("c1", "u1", ClipType.PHOTO, 3000L))
        val state2 = listOf(Clip("c1", "u1", ClipType.PHOTO, 3000L), Clip("c2", "u2", ClipType.VIDEO, 5000L))

        assertFalse(stack.canUndo())
        assertFalse(stack.canRedo())

        stack.pushState(state1)
        assertTrue(stack.canUndo())
        assertFalse(stack.canRedo())

        val undo1 = stack.undo(state2)
        assertEquals(state1, undo1)
        assertFalse(stack.canUndo())
        assertTrue(stack.canRedo())

        val redo1 = stack.redo(state1)
        assertEquals(state2, redo1)
        assertTrue(stack.canUndo())
        assertFalse(stack.canRedo())
    }

    @Test
    fun selectionSafety_afterDeletionAndUndo_fallbackToValidRemainingClip() {
        val clips = listOf(
            Clip("c1", "uri1", ClipType.PHOTO, 3000L),
            Clip("c2", "uri2", ClipType.VIDEO, 5000L),
        )
        val deleted = TimelineEditEngine.delete(clips, "c1")
        val fallbackId = if (deleted.any { it.id == "c1" }) "c1" else deleted.firstOrNull()?.id
        assertEquals("c2", fallbackId)
    }

    @Test
    fun setSpeed_coercesAndUpdatesClipSpeedAndEffectiveDuration() {
        val clip = Clip("c1", "uri1", ClipType.VIDEO, 10000L, 0L, 10000L, speed = 1.0f)
        assertEquals(10000L, TimelineEditEngine.effectiveDurationMs(clip))

        val halfSpeed = TimelineEditEngine.setSpeed(clip, 0.5f)
        assertEquals(0.5f, halfSpeed.speed, 0.01f)
        assertEquals(20000L, TimelineEditEngine.effectiveDurationMs(halfSpeed))

        val doubleSpeed = TimelineEditEngine.setSpeed(clip, 2.0f)
        assertEquals(2.0f, doubleSpeed.speed, 0.01f)
        assertEquals(5000L, TimelineEditEngine.effectiveDurationMs(doubleSpeed))

        val overflowSpeed = TimelineEditEngine.setSpeed(clip, 10.0f)
        assertEquals(4.0f, overflowSpeed.speed, 0.01f) // Coerced to max 4.0
        assertEquals(2500L, TimelineEditEngine.effectiveDurationMs(overflowSpeed))
    }
}
