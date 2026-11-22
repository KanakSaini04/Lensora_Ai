package com.lensora.ai.ai

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object SceneLabelMap {
    private val landscapeCategories = setOf(
        "badlands", "bamboo_forest", "beach", "botanical_garden", "butte",
        "canyon", "cliff", "coast", "corn_field", "creek", "crevasse",
        "desert/sand", "desert/vegetation", "desert_road",
        "field/cultivated", "field/wild", "field_road", "forest/broadleaf", "forest_path", "forest_road", "formal_garden",
        "glacier", "grotto",
        "hayfield", "hot_spring",
        "iceberg", "islet",
        "japanese_garden",
        "lagoon", "lake/natural", "lawn",
        "marsh", "mountain", "mountain_path", "mountain_snowy",
        "ocean", "orchard",
        "park", "pasture", "pond",
        "rainforest", "rice_paddy", "river", "rock_arch",
        "ski_resort", "ski_slope", "snowfield", "swamp", "swimming_hole",
        "topiary_garden", "tree_farm", "tundra",
        "underwater/ocean_deep",
        "valley", "vegetable_garden", "vineyard", "volcano",
        "waterfall", "watering_hole", "wave", "wheat_field",
        "zen_garden",
    )

    private val skyCategories = setOf(
        "sky",
    )

    private var cachedLabels: List<String>? = null

    fun loadLabels(context: Context): List<String> {
        cachedLabels?.let { return it }

        val labels = mutableListOf<String>()
        try {
            context.assets.open("categories_places365.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.isNotEmpty()) {
                                val rawCategory = parts[0]
                                // Format is /a/airfield -> airfield
                                val categoryName = rawCategory.substringAfter("/", "").substringAfter("/", rawCategory)
                                labels.add(categoryName)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Safe fallback if assets file fails
        }
        if (labels.isNotEmpty()) {
            cachedLabels = labels
        }
        return labels
    }

    fun parseCategoryLine(line: String): String {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split("\\s+".toRegex())
        val rawCategory = parts[0]
        return rawCategory.substringAfter("/", "").substringAfter("/", rawCategory)
    }

    fun mapCategoryToAppLabel(categoryName: String, confidence: Float): String {
        if (confidence < 0.30f) {
            return "✨ AI: Auto"
        }
        val cleanName = categoryName.lowercase()
        return when {
            skyCategories.any { cleanName == it || cleanName.endsWith("/$it") } -> "✨ AI: Sky"
            landscapeCategories.any { cleanName == it || cleanName.endsWith("/$it") } -> "✨ AI: Landscape"
            else -> "✨ AI: Auto"
        }
    }

    fun mapIndexToAppLabel(labels: List<String>, topIndex: Int, confidence: Float): String {
        if (topIndex !in labels.indices) return "✨ AI: Auto"
        val categoryName = labels[topIndex]
        return mapCategoryToAppLabel(categoryName, confidence)
    }
}
