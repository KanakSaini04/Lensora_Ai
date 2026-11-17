package com.lensora.ai.editor

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository(
    private val baseDir: File,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "projects"))

    private val tag = "ProjectRepository"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    suspend fun saveProject(project: Project): Boolean = withContext(Dispatchers.IO) {
        if (!project.validate()) {
            Log.w(tag, "Failed to save project ${project.id}: Validation failed")
            return@withContext false
        }

        try {
            val tmpFile = File(baseDir, "${project.id}.json.tmp")
            val targetFile = File(baseDir, "${project.id}.json")

            val jsonString = json.encodeToString(Project.serializer(), project)
            tmpFile.writeText(jsonString)

            // Atomic rename
            var renamed = tmpFile.renameTo(targetFile)
            if (!renamed) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
                renamed = true
            }
            Log.i(tag, "Successfully saved project ${project.id} atomically to ${targetFile.name}")
            renamed
        } catch (e: Exception) {
            Log.e(tag, "Error saving project ${project.id}", e)
            false
        }
    }

    suspend fun loadProject(id: String): Project? = withContext(Dispatchers.IO) {
        val targetFile = File(baseDir, "$id.json")
        if (!targetFile.exists()) return@withContext null

        try {
            val content = targetFile.readText()
            val project = json.decodeFromString(Project.serializer(), content)
            if (project.validate()) {
                project
            } else {
                Log.w(tag, "Loaded project $id failed validation")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Corrupt or invalid project JSON for $id: ${e.message}")
            null
        }
    }

    suspend fun listProjects(): List<Project> = withContext(Dispatchers.IO) {
        val jsonFiles = baseDir.listFiles { _, name -> name.endsWith(".json") && !name.endsWith(".tmp") } ?: return@withContext emptyList()

        jsonFiles.mapNotNull { file ->
            try {
                val content = file.readText()
                val project = json.decodeFromString(Project.serializer(), content)
                if (project.validate()) project else null
            } catch (e: Exception) {
                Log.w(tag, "Skipping corrupt project file ${file.name}: ${e.message}")
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(baseDir, "$id.json")
            val tmpFile = File(baseDir, "$id.json.tmp")
            if (tmpFile.exists()) tmpFile.delete()

            val projectMediaDir = File(baseDir, id)
            if (projectMediaDir.exists()) {
                projectMediaDir.deleteRecursively()
                Log.i(tag, "Deleted media directory for project $id")
            }

            if (targetFile.exists()) {
                val deleted = targetFile.delete()
                Log.i(tag, "Deleted project JSON $id: $deleted")
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting project $id", e)
            false
        }
    }
}
