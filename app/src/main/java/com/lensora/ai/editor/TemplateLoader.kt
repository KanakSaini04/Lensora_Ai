package com.lensora.ai.editor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream

object TemplateLoader {

    private const val TAG = "TemplateLoader"
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun parseTemplateJson(jsonString: String): Template? {
        return try {
            val template = json.decodeFromString<Template>(jsonString)
            if (template.validate()) {
                template
            } else {
                Log.w(TAG, "Template ${template.id} failed validation")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Template JSON: ${e.message}")
            null
        }
    }

    fun loadTemplateFromStream(inputStream: InputStream): Template? {
        return try {
            val content = inputStream.bufferedReader().use { it.readText() }
            parseTemplateJson(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Template input stream: ${e.message}")
            null
        }
    }

    suspend fun loadTemplateFromAsset(context: Context, assetPath: String): Template? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(assetPath).use { stream ->
                loadTemplateFromStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading template asset $assetPath: ${e.message}")
            null
        }
    }

    suspend fun loadAllTemplatesFromAssets(context: Context): List<Template> = withContext(Dispatchers.IO) {
        try {
            val assetFiles = context.assets.list("templates") ?: return@withContext emptyList()
            assetFiles.filter { it.endsWith(".json") }.mapNotNull { fileName ->
                loadTemplateFromAsset(context, "templates/$fileName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing templates from assets: ${e.message}")
            emptyList()
        }
    }

    fun createProjectFromTemplate(
        template: Template,
        pickedUris: List<String>,
        projectName: String = template.name,
    ): Project {
        val clips = template.slots.mapIndexed { index, slot ->
            val uri = if (index in pickedUris.indices) pickedUris[index] else "android.resource://com.lensora.ai/drawable/sample_placeholder"
            val type = if (uri.endsWith(".mp4") || uri.contains("video")) ClipType.VIDEO else ClipType.PHOTO
            Clip(
                id = "clip_slot_${index + 1}_${java.util.UUID.randomUUID().toString().take(6)}",
                sourceUri = uri,
                type = type,
                durationMs = slot.durationMs,
                trimStartMs = 0L,
                trimEndMs = slot.durationMs,
                vibePreset = slot.vibePreset,
                transitionIn = slot.transitionIn,
                textOverlay = slot.text,
            )
        }
        return Project(
            id = "proj_tmpl_${System.currentTimeMillis()}",
            name = projectName,
            aspect = template.aspect,
            clips = clips,
        )
    }
}
