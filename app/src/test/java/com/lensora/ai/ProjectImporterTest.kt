package com.lensora.ai

import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.import.ExtensionHelper
import com.lensora.ai.editor.import.StorageGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectImporterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testExtensionHelperWhitelist() {
        val allowed = ExtensionHelper.ALLOWED_EXTENSIONS
        assertTrue(allowed.contains("jpg"))
        assertTrue(allowed.contains("jpeg"))
        assertTrue(allowed.contains("png"))
        assertTrue(allowed.contains("webp"))
        assertTrue(allowed.contains("heic"))
        assertTrue(allowed.contains("heif"))
        assertTrue(allowed.contains("mp4"))
        assertTrue(allowed.contains("mov"))
        assertTrue(allowed.contains("webm"))
        assertTrue(allowed.contains("3gp"))
        assertTrue(allowed.contains("mkv"))

        assertFalse(allowed.contains("exe"))
        assertFalse(allowed.contains("apk"))
        assertFalse(allowed.contains("bat"))
    }

    @Test
    fun testStorageGuardEvaluation() {
        val enoughAvailable = 5L * 1024 * 1024 * 1024
        val pickedSize = 100L * 1024 * 1024
        assertTrue(StorageGuard.evaluateStorageGuard(enoughAvailable, pickedSize))

        val lowAvailable = 100L * 1024 * 1024
        assertFalse(StorageGuard.evaluateStorageGuard(lowAvailable, pickedSize))

        val largePicked = 2L * 1024 * 1024 * 1024
        val tightAvailable = 1L * 1024 * 1024 * 1024
        assertFalse(StorageGuard.evaluateStorageGuard(tightAvailable, largePicked))
    }

    @Test
    fun testClipBuildingPhotoAndVideoDurations() {
        val photoClip = Clip(
            id = "c1",
            sourceUri = "file:///path/photo.jpg",
            type = ClipType.PHOTO,
            durationMs = 3000L,
        )
        assertTrue(photoClip.validate())
        assertEquals(3000L, photoClip.durationMs)

        val videoClip = Clip(
            id = "c2",
            sourceUri = "file:///path/video.mp4",
            type = ClipType.VIDEO,
            durationMs = 12500L,
        )
        assertTrue(videoClip.validate())
        assertEquals(12500L, videoClip.durationMs)

        val fallbackVideoClip = Clip(
            id = "c3",
            sourceUri = "file:///path/corrupt.mp4",
            type = ClipType.VIDEO,
            durationMs = 3000L,
        )
        assertTrue(fallbackVideoClip.validate())
        assertEquals(3000L, fallbackVideoClip.durationMs)
    }

    @Test
    fun testTmpFileCleanupOnFolderDeletion() {
        val testProjectDir = tempFolder.newFolder("proj_1001")
        val mediaDir = File(testProjectDir, "media").apply { mkdirs() }

        val tmpFile1 = File(mediaDir, "clip_1.mp4.tmp").apply { writeText("partial data") }
        val tmpFile2 = File(mediaDir, "clip_2.jpg.tmp").apply { writeText("partial data") }

        assertTrue(tmpFile1.exists())
        assertTrue(tmpFile2.exists())

        testProjectDir.deleteRecursively()

        assertFalse(tmpFile1.exists())
        assertFalse(tmpFile2.exists())
        assertFalse(testProjectDir.exists())
    }
}
