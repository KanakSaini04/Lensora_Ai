package com.lensora.ai.editor

object TimelineEditEngine {

    const val DEFAULT_PHOTO_DURATION_MS = 3000L

    fun effectiveDurationMs(clip: Clip): Long {
        val duration = if (clip.durationMs <= 0L && clip.type == ClipType.PHOTO) DEFAULT_PHOTO_DURATION_MS else clip.durationMs
        val trimStart = clip.trimStartMs.coerceIn(0L, duration)
        val trimEnd = if (clip.trimEndMs <= 0L && clip.type == ClipType.PHOTO) duration else clip.trimEndMs.coerceIn(trimStart, duration)
        val rawSpan = (trimEnd - trimStart).coerceAtLeast(0L)
        val speed = if (clip.speed > 0f) clip.speed else 1.0f
        return (rawSpan / speed).toLong()
    }

    fun totalDurationMs(clips: List<Clip>): Long {
        return clips.sumOf { effectiveDurationMs(it) }
    }

    fun totalDurationMs(project: Project): Long {
        return totalDurationMs(project.clips)
    }

    fun trim(clip: Clip, inMs: Long, outMs: Long): Clip {
        val totalMs = if (clip.durationMs <= 0L && clip.type == ClipType.PHOTO) DEFAULT_PHOTO_DURATION_MS else clip.durationMs
        val safeIn = inMs.coerceIn(0L, totalMs)
        val safeOut = outMs.coerceIn(safeIn, totalMs)

        if (safeOut <= safeIn) {
            return clip
        }

        return clip.copy(
            durationMs = if (clip.durationMs <= 0L) totalMs else clip.durationMs,
            trimStartMs = safeIn,
            trimEndMs = safeOut,
        )
    }

    fun split(clip: Clip, atMs: Long): Pair<Clip, Clip> {
        val start = clip.trimStartMs
        val end = clip.trimEndMs

        if (atMs <= start + 10L || atMs >= end - 10L) {
            return Pair(clip, clip)
        }

        val clipA = clip.copy(
            id = "${clip.id}_split_a",
            trimStartMs = start,
            trimEndMs = atMs,
        )

        val clipB = clip.copy(
            id = "${clip.id}_split_b",
            trimStartMs = atMs,
            trimEndMs = end,
        )

        return Pair(clipA, clipB)
    }

    fun reorder(clips: List<Clip>, fromIndex: Int, toIndex: Int): List<Clip> {
        if (fromIndex !in clips.indices || toIndex !in clips.indices || fromIndex == toIndex) {
            return clips
        }

        val mutable = clips.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.toList()
    }

    fun delete(clips: List<Clip>, clipId: String): List<Clip> {
        val filtered = clips.filterNot { it.id == clipId }
        return if (filtered.size == clips.size) clips else filtered
    }
}

class TimelineUndoRedoStack(private val maxHistory: Int = 50) {

    private val undoStack = ArrayDeque<List<Clip>>()
    private val redoStack = ArrayDeque<List<Clip>>()

    fun pushState(clips: List<Clip>) {
        undoStack.addLast(clips)
        if (undoStack.size > maxHistory) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(currentClips: List<Clip>): List<Clip>? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeLast()
        redoStack.addLast(currentClips)
        return previous
    }

    fun redo(currentClips: List<Clip>): List<Clip>? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeLast()
        undoStack.addLast(currentClips)
        return next
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
