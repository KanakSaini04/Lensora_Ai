package com.lensora.ai

import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.MusicTrack
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.ProjectRepository
import com.lensora.ai.editor.Template
import com.lensora.ai.editor.TemplateLoader
import com.lensora.ai.editor.TemplateSlot
import com.lensora.ai.editor.TransitionType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EditorFoundationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun testProjectJsonRoundTrip() {
        val clip = Clip(
            id = "clip_01",
            sourceUri = "content://media/external/images/media/101",
            type = ClipType.PHOTO,
            durationMs = 3000L,
            trimStartMs = 0L,
            trimEndMs = 3000L,
            speed = 1.0f,
            volume = 1.0f,
            vibePreset = "CINEMATIC",
            transitionIn = TransitionType.FADE,
        )

        val music = MusicTrack(
            uri = "content://media/external/audio/media/202",
            startMs = 500L,
            volume = 0.8f,
        )

        val originalProject = Project(
            id = "proj_001",
            name = "Test Travel Vlog",
            aspect = AspectRatio.NINE_SIXTEEN,
            clips = listOf(clip),
            music = music,
        )

        assertTrue(originalProject.validate())

        val encodedJson = json.encodeToString(Project.serializer(), originalProject)
        assertNotNull(encodedJson)

        val decodedProject = json.decodeFromString(Project.serializer(), encodedJson)
        assertEquals(originalProject.id, decodedProject.id)
        assertEquals(originalProject.name, decodedProject.name)
        assertEquals(originalProject.aspect, decodedProject.aspect)
        assertEquals(1, decodedProject.clips.size)
        assertEquals("clip_01", decodedProject.clips[0].id)
        assertNotNull(decodedProject.music)
        assertEquals(0.8f, decodedProject.music!!.volume, 0.01f)
    }

    @Test
    fun testTemplateJsonRoundTripAndSampleParsing() {
        val sampleJson = """
            {
              "id": "sample_vlog_01",
              "name": "Urban Vlog Beat 9:16",
              "aspect": "NINE_SIXTEEN",
              "musicAsset": "audio/vlog_beat.mp3",
              "slots": [
                {
                  "durationMs": 2500,
                  "vibePreset": "CINEMATIC",
                  "transitionIn": "FADE",
                  "text": "MORNING COFFEE"
                },
                {
                  "durationMs": 3000,
                  "vibePreset": "ROYAL_GOLD",
                  "transitionIn": "SLIDE",
                  "text": "CITY EXPLORATION"
                }
              ]
            }
        """.trimIndent()

        val parsedTemplate = TemplateLoader.parseTemplateJson(sampleJson)
        assertNotNull(parsedTemplate)
        assertEquals("sample_vlog_01", parsedTemplate!!.id)
        assertEquals(AspectRatio.NINE_SIXTEEN, parsedTemplate.aspect)
        assertEquals(2, parsedTemplate.slots.size)
        assertTrue(parsedTemplate.validate())
    }

    @Test
    fun testModelValidationRules() {
        val validClip = Clip(
            id = "c1",
            sourceUri = "uri1",
            type = ClipType.VIDEO,
            durationMs = 5000L,
            trimStartMs = 1000L,
            trimEndMs = 4000L,
            speed = 1.5f,
            volume = 0.5f,
        )
        assertTrue(validClip.validate())

        // Invalid: trimEndMs <= trimStartMs
        val invalidTrimClip = validClip.copy(trimStartMs = 3000L, trimEndMs = 2000L)
        assertFalse(invalidTrimClip.validate())

        // Invalid: speed out of 0.25..4.0
        val invalidSpeedClip = validClip.copy(speed = 5.0f)
        assertFalse(invalidSpeedClip.validate())

        // Invalid: volume out of 0..1
        val invalidVolumeClip = validClip.copy(volume = -0.5f)
        assertFalse(invalidVolumeClip.validate())

        // Invalid: durationMs <= 0
        val invalidDurationClip = validClip.copy(durationMs = 0L)
        assertFalse(invalidDurationClip.validate())

        val validMusic = MusicTrack(uri = "uri2", startMs = 0L, volume = 1.0f)
        assertTrue(validMusic.validate())

        val invalidMusic = validMusic.copy(volume = 1.5f)
        assertFalse(invalidMusic.validate())
    }

    @Test
    fun testProjectRepositorySaveLoadListDelete() = runBlocking {
        val tempDir = tempFolder.newFolder("projects")
        val repo = ProjectRepository(tempDir)

        val clip = Clip(
            id = "c1",
            sourceUri = "uri1",
            type = ClipType.PHOTO,
            durationMs = 2000L,
        )
        val project = Project(
            id = "proj_test_1",
            name = "My Summer Reel",
            aspect = AspectRatio.ONE_ONE,
            clips = listOf(clip),
        )

        // 1. Save Project Atomically
        val saved = repo.saveProject(project)
        assertTrue(saved)

        // Verify .tmp file is GONE after successful save
        val tmpFile = File(tempDir, "proj_test_1.json.tmp")
        assertFalse(tmpFile.exists())

        val targetFile = File(tempDir, "proj_test_1.json")
        assertTrue(targetFile.exists())

        // 2. Load Project
        val loaded = repo.loadProject("proj_test_1")
        assertNotNull(loaded)
        assertEquals("proj_test_1", loaded!!.id)
        assertEquals("My Summer Reel", loaded.name)

        // 3. List Projects
        val list = repo.listProjects()
        assertEquals(1, list.size)
        assertEquals("proj_test_1", list[0].id)

        // 4. Delete Project
        val deleted = repo.deleteProject("proj_test_1")
        assertTrue(deleted)
        assertFalse(targetFile.exists())
        assertEquals(0, repo.listProjects().size)
    }

    @Test
    fun testCorruptJsonHandlingDoesNotCrash() = runBlocking {
        val tempDir = tempFolder.newFolder("corrupt_projects")
        val repo = ProjectRepository(tempDir)

        // Create corrupt JSON file
        val corruptFile = File(tempDir, "corrupt_01.json")
        corruptFile.writeText("{ \"id\": \"corrupt_01\", \"invalid_field\": [NOT_VALID_JSON] }")

        // Load corrupt JSON must return null, NOT crash
        val loaded = repo.loadProject("corrupt_01")
        assertNull(loaded)

        // List projects must skip corrupt file and return empty list without crashing
        val list = repo.listProjects()
        assertTrue(list.isEmpty())
    }
}
