package com.lensora.ai

import com.lensora.ai.ai.SceneLabelMap
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SceneLabelMapTest {

    @Test
    fun parseCategoryLine_extractsCleanCategoryName() {
        assertEquals("airfield", SceneLabelMap.parseCategoryLine("/a/airfield 0"))
        assertEquals("bamboo_forest", SceneLabelMap.parseCategoryLine("/b/bamboo_forest 36"))
        assertEquals("zen_garden", SceneLabelMap.parseCategoryLine("/z/zen_garden 364"))
    }

    @Test
    fun mapCategoryToAppLabel_landscapeCategory_returnsLandscapeLabel() {
        val label = SceneLabelMap.mapCategoryToAppLabel("mountain", 0.85f)
        assertEquals("✨ AI: Landscape", label)
    }

    @Test
    fun mapCategoryToAppLabel_skyCategory_returnsSkyLabel() {
        val label = SceneLabelMap.mapCategoryToAppLabel("sky", 0.90f)
        assertEquals("✨ AI: Sky", label)
    }

    @Test
    fun mapCategoryToAppLabel_skyscraper_mapsToAutoLabel() {
        val label = SceneLabelMap.mapCategoryToAppLabel("skyscraper", 0.90f)
        assertEquals("✨ AI: Auto", label)
    }

    @Test
    fun mapCategoryToAppLabel_lowConfidence_returnsAutoLabel() {
        val label = SceneLabelMap.mapCategoryToAppLabel("mountain", 0.25f) // Below 0.30 threshold
        assertEquals("✨ AI: Auto", label)
    }

    @Test
    fun mapCategoryToAppLabel_unmappedClass_returnsAutoLabel() {
        val label = SceneLabelMap.mapCategoryToAppLabel("bookstore", 0.88f)
        assertEquals("✨ AI: Auto", label)
    }

    @Test
    fun mapIndexToAppLabel_realPlaces365FileIndices_verifyPositiveAndNegativeMappings() {
        val assetFile = File("src/main/assets/categories_places365.txt").takeIf { it.exists() }
            ?: File("app/src/main/assets/categories_places365.txt")
        val lines = assetFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }

        assertEquals(365, lines.size)

        val categories = lines.map { SceneLabelMap.parseCategoryLine(it) }

        assertEquals("bar", categories[39])
        assertEquals("beach", categories[48])
        assertEquals("living_room", categories[215])
        assertEquals("mountain", categories[232])
        assertEquals("sky", categories[306])
        assertEquals("skyscraper", categories[307])

        assertEquals("✨ AI: Auto", SceneLabelMap.mapIndexToAppLabel(categories, 39, 0.85f))
        assertEquals("✨ AI: Landscape", SceneLabelMap.mapIndexToAppLabel(categories, 48, 0.85f))
        assertEquals("✨ AI: Auto", SceneLabelMap.mapIndexToAppLabel(categories, 215, 0.85f))
        assertEquals("✨ AI: Landscape", SceneLabelMap.mapIndexToAppLabel(categories, 232, 0.85f))
        assertEquals("✨ AI: Sky", SceneLabelMap.mapIndexToAppLabel(categories, 306, 0.85f))
        assertEquals("✨ AI: Auto", SceneLabelMap.mapIndexToAppLabel(categories, 307, 0.85f))
    }
}
