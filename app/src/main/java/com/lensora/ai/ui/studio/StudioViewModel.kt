package com.lensora.ai.ui.studio

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StudioViewModel : ViewModel() {

    private val _strokeHistory = StrokeHistory(maxUndoSteps = 25)
    private val _strokesState = MutableStateFlow<List<StudioStroke>>(emptyList())
    val strokesState: StateFlow<List<StudioStroke>> = _strokesState.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun addStroke(stroke: StudioStroke) {
        _strokeHistory.addStroke(stroke)
        updateState()
    }

    fun undo(): Boolean {
        val result = _strokeHistory.undo()
        updateState()
        return result
    }

    fun redo(): Boolean {
        val result = _strokeHistory.redo()
        updateState()
        return result
    }

    fun clearMaskAndHistory() {
        _strokeHistory.clear()
        updateState()
    }

    private fun updateState() {
        _strokesState.value = _strokeHistory.strokes
        _canUndo.value = _strokeHistory.canUndo
        _canRedo.value = _strokeHistory.canRedo
    }
}
