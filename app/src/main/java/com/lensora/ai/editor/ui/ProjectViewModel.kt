package com.lensora.ai.editor.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.ProjectRepository
import com.lensora.ai.editor.import.ImportState
import com.lensora.ai.editor.import.ProjectImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val importState: ImportState = ImportState.Idle,
    val selectedProject: Project? = null,
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

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = repository.listProjects()
            _uiState.update { it.copy(projects = list, isLoading = false) }
        }
    }

    fun startImport(
        pickedUris: List<Uri>,
        projectName: String,
        aspectRatio: AspectRatio = AspectRatio.NINE_SIXTEEN,
    ) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            importer.importMediaFlow(pickedUris, projectName, aspectRatio).collect { state ->
                _uiState.update { it.copy(importState = state) }
                if (state is ImportState.Success) {
                    loadProjects()
                    _uiState.update { it.copy(selectedProject = state.project) }
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
        _uiState.update { it.copy(selectedProject = project) }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_uiState.value.selectedProject?.id == projectId) {
                _uiState.update { it.copy(selectedProject = null) }
            }
            loadProjects()
        }
    }
}
