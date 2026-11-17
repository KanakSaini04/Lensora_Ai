package com.lensora.ai

import com.lensora.ai.editor.AspectRatio
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testOrphanSweepLogic() = runBlocking {
        val projectsDir = tempFolder.newFolder("projects")
        val repo = ProjectRepository(projectsDir)

        val fixedNow = 1_700_000_000_000L
        val maxAgeMs = 30 * 60 * 1000L // 30 minutes

        // 1. Valid project folder WITH matching JSON -> MUST BE KEPT
        val validFolder = File(projectsDir, "proj_valid").apply { mkdirs() }
        validFolder.setLastModified(fixedNow - (40 * 60 * 1000L)) // Old (40 min ago)
        val validProject = Project(
            id = "proj_valid",
            name = "Valid Project",
            aspect = AspectRatio.NINE_SIXTEEN,
            clips = listOf(Clip("c1", "uri", ClipType.PHOTO, 3000L)),
        )
        repo.saveProject(validProject)

        // 2. Old orphan folder WITHOUT matching JSON -> MUST BE DELETED
        val oldOrphanFolder = File(projectsDir, "proj_orphan_old").apply { mkdirs() }
        File(oldOrphanFolder, "stale.mp4").writeText("data")
        oldOrphanFolder.setLastModified(fixedNow - (40 * 60 * 1000L)) // 40 min ago

        // 3. Fresh orphan folder WITHOUT matching JSON -> MUST BE KEPT
        val freshOrphanFolder = File(projectsDir, "proj_orphan_fresh").apply { mkdirs() }
        File(freshOrphanFolder, "fresh.mp4").writeText("data")
        freshOrphanFolder.setLastModified(fixedNow - (10 * 60 * 1000L)) // 10 min ago

        // 4. Old .tmp file -> MUST BE DELETED
        val oldTmpFile = File(projectsDir, "stale_data.tmp").apply { writeText("tmp") }
        oldTmpFile.setLastModified(fixedNow - (40 * 60 * 1000L)) // 40 min ago

        // 5. Fresh .tmp file -> MUST BE KEPT
        val freshTmpFile = File(projectsDir, "fresh_data.tmp").apply { writeText("tmp") }
        freshTmpFile.setLastModified(fixedNow - (5 * 60 * 1000L)) // 5 min ago

        // Run sweep with fixed clock
        val deletedCount = repo.sweepOrphanProjects(clockMillis = { fixedNow }, maxAgeMs = maxAgeMs)

        assertTrue(deletedCount >= 2)
        assertTrue(validFolder.exists())
        assertTrue(File(projectsDir, "proj_valid.json").exists())
        assertFalse(oldOrphanFolder.exists())
        assertTrue(freshOrphanFolder.exists())
        assertFalse(oldTmpFile.exists())
        assertTrue(freshTmpFile.exists())
    }
}
