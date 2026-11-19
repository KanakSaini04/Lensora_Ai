package com.lensora.ai.editor.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ExportWorker
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.ProjectRepository
import com.lensora.ai.editor.TimelineEditEngine
import com.lensora.ai.editor.TimelineUndoRedoStack
import com.lensora.ai.editor.import.ImportState
import com.lensora.ai.editor.import.ProjectImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val importState: ImportState = ImportState.Idle,
    val selectedProject: Project? = null,
    val selectedClipId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportWorkId: UUID? = null,
    val exportError: String? = null,
)

@HiltViewModel
class ProjectViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProjectRepository = ProjectRepository(context),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    private val importer = ProjectImporter(context, repository)
    private var importJob: Job? = null
    private val undoRedoStack = TimelineUndoRedoStack()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.sweepOrphanProjects()
            loadProjectsInternal()
        }
    }

    fun loadProjects() {
        viewModelScope.launch(Dispatchers.IO) {
            loadProjectsInternal()
        }
    }

    private suspend fun loadProjectsInternal() {
        _uiState.update { it.copy(isLoading = true) }
        val list = repository.listProjects()
        _uiState.update { it.copy(projects = list, isLoading = false) }
    }

    fun startImport(
        pickedUris: List<Uri>,
        projectName: String,
        aspectRatio: AspectRatio = AspectRatio.NINE_SIXTEEN,
    ) {
        importJob?.cancel()
        importJob = viewModelScope.launch(Dispatchers.IO) {
            importer.importMediaFlow(pickedUris, projectName, aspectRatio).collect { state ->
                _uiState.update { it.copy(importState = state) }
                if (state is ImportState.Success) {
                    loadProjectsInternal()
                    selectProject(state.project)
                }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _uiState.update { it.copy(importState = ImportState.Cancelled) }
    }

    fun dismissImportState() {
        _uiState.update { it.copy(importState = ImportState.Idle) }
    }

    fun selectProject(project: Project?) {
        undoRedoStack.clear()
        val firstClipId = project?.clips?.firstOrNull()?.id
        _uiState.update {
            it.copy(
                selectedProject = project,
                selectedClipId = firstClipId,
                canUndo = false,
                canRedo = false,
                isExporting = false,
                exportProgress = 0,
                exportWorkId = null,
                exportError = null,
            )
        }
    }

    fun selectClip(clipId: String?) {
        _uiState.update { it.copy(selectedClipId = clipId) }
    }

    private fun applyClipsChange(newClips: List<Clip>) {
        val currentProj = _uiState.value.selectedProject ?: return
        undoRedoStack.pushState(currentProj.clips)

        val updatedProj = currentProj.copy(
            clips = newClips,
            updatedAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                selectedProject = updatedProj,
                canUndo = undoRedoStack.canUndo(),
                canRedo = undoRedoStack.canRedo(),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(updatedProj)
            loadProjectsInternal()
        }
    }

    fun trimClip(clipId: String, inMs: Long, outMs: Long) {
        val proj = _uiState.value.selectedProject ?: return
        val target = proj.clips.firstOrNull { it.id == clipId } ?: return
        val trimmed = TimelineEditEngine.trim(target, inMs, outMs)
        val updatedClips = proj.clips.map { if (it.id == clipId) trimmed else it }
        applyClipsChange(updatedClips)
    }

    fun splitClip(clipId: String, atMs: Long) {
        val proj = _uiState.value.selectedProject ?: return
        val idx = proj.clips.indexOfFirst { it.id == clipId }
        if (idx == -1) return

        val target = proj.clips[idx]
        val (clipA, clipB) = TimelineEditEngine.split(target, atMs)

        if (clipA == target && clipB == target) return

        val mutable = proj.clips.toMutableList()
        mutable.removeAt(idx)
        mutable.add(idx, clipB)
        mutable.add(idx, clipA)

        applyClipsChange(mutable)
        selectClip(clipA.id)
    }

    fun reorderClip(fromIndex: Int, toIndex: Int) {
        val proj = _uiState.value.selectedProject ?: return
        val reordered = TimelineEditEngine.reorder(proj.clips, fromIndex, toIndex)
        if (reordered != proj.clips) {
            applyClipsChange(reordered)
        }
    }

    fun deleteClip(clipId: String) {
        val proj = _uiState.value.selectedProject ?: return
        val remaining = TimelineEditEngine.delete(proj.clips, clipId)
        if (remaining != proj.clips) {
            applyClipsChange(remaining)
            val nextSelectedId = remaining.firstOrNull()?.id
            selectClip(nextSelectedId)
        }
    }

    fun undoEdit() {
        val proj = _uiState.value.selectedProject ?: return
        val previousClips = undoRedoStack.undo(proj.clips) ?: return

        val updatedProj = proj.copy(
            clips = previousClips,
            updatedAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                selectedProject = updatedProj,
                canUndo = undoRedoStack.canUndo(),
                canRedo = undoRedoStack.canRedo(),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(updatedProj)
            loadProjectsInternal()
        }
    }

    fun redoEdit() {
        val proj = _uiState.value.selectedProject ?: return
        val nextClips = undoRedoStack.redo(proj.clips) ?: return

        val updatedProj = proj.copy(
            clips = nextClips,
            updatedAt = System.currentTimeMillis(),
        )

        _uiState.update {
            it.copy(
                selectedProject = updatedProj,
                canUndo = undoRedoStack.canUndo(),
                canRedo = undoRedoStack.canRedo(),
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(updatedProj)
            loadProjectsInternal()
        }
    }

    fun startExport(projectId: String, resolutionHeight: Int = 1080) {
        val workManager = WorkManager.getInstance(context)
        val exportRequest = OneTimeWorkRequestBuilder<ExportWorker>()
            .setInputData(
                workDataOf(
                    "PROJECT_ID" to projectId,
                    "RESOLUTION_HEIGHT" to resolutionHeight,
                )
            )
            .build()

        _uiState.update {
            it.copy(
                isExporting = true,
                exportProgress = 0,
                exportWorkId = exportRequest.id,
                exportError = null,
            )
        }

        workManager.enqueue(exportRequest)

        viewModelScope.launch(Dispatchers.IO) {
            workManager.getWorkInfoByIdFlow(exportRequest.id).collect { info ->
                if (info != null) {
                    val progress = info.progress.getInt("PROGRESS", 0)
                    val isRunning = info.state == WorkInfo.State.RUNNING

                    _uiState.update {
                        it.copy(
                            exportProgress = progress,
                            isExporting = isRunning,
                            exportError = if (info.state == WorkInfo.State.FAILED) {
                                info.outputData.getString("ERROR") ?: "Export failed"
                            } else null,
                        )
                    }
                }
            }
        }
    }

    fun cancelExport() {
        val workId = _uiState.value.exportWorkId
        if (workId != null) {
            WorkManager.getInstance(context).cancelWorkById(workId)
        }
        _uiState.update {
            it.copy(
                isExporting = false,
                exportProgress = 0,
                exportWorkId = null,
            )
        }
    }

    fun dismissExportError() {
        _uiState.update { it.copy(exportError = null) }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(projectId)
            if (_uiState.value.selectedProject?.id == projectId) {
                _uiState.update { it.copy(selectedProject = null) }
            }
            loadProjectsInternal()
        }
    }
}
