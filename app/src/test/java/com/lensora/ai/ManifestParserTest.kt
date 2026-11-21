package com.lensora.ai

import com.lensora.ai.ai.ManifestParser
import com.lensora.ai.ai.ModelFileGuard
import com.lensora.ai.ai.Sha256Util
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class ManifestParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun modelFileGuard_isUsable_validatesMissingEmptyAndValidFiles() {
        val missingFile = File(tempFolder.root, "non_existent.tflite")
        assertFalse(ModelFileGuard.isUsable(missingFile))

        val emptyFile = tempFolder.newFile("empty.tflite")
        assertFalse(ModelFileGuard.isUsable(emptyFile))

        val validFile = tempFolder.newFile("valid.tflite")
        validFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertTrue(ModelFileGuard.isUsable(validFile))
    }

    @Test
    fun sha256Util_hexAndMatches_verifiesCorrectly() {
        val tempFile = tempFolder.newFile("test_sha.tflite")
        val testBytes = "Lensora AI Test Model Buffer 2026".toByteArray()
        tempFile.writeBytes(testBytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(testBytes)
        val expectedHex = hashBytes.joinToString("") { "%02x".format(it) }

        val actualHex = Sha256Util.hex(tempFile)
        assertEquals(expectedHex, actualHex)

        // Case-insensitive match check
        assertTrue(Sha256Util.matches(tempFile, expectedHex.uppercase()))
        assertTrue(Sha256Util.matches(tempFile, expectedHex.lowercase()))

        // Null/blank expected SHA-256 means unverified (returns true)
        assertTrue(Sha256Util.matches(tempFile, null))
        assertTrue(Sha256Util.matches(tempFile, ""))

        // Mismatch check
        assertFalse(Sha256Util.matches(tempFile, "badhash1234567890"))
    }

    @Test
    fun parseManifest_urlWithQueryParams_parsesFullUrl() {
        val json = """
            {
              "schema_version": 2,
              "model_key": {
                "file_name": "model.tflite",
                "url": "https://example.com/model.zip?a=1&b=2"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("https://example.com/model.zip?a=1&b=2", entry.url)
    }

    @Test
    fun parseManifest_escapedQuotesInString_unescapesQuotesCorrectly() {
        val json = """
            {
              "schema_version": 2,
              "model_key": {
                "file_name": "model_\"v1\".tflite",
                "url": "https://example.com/path/\"model\".zip"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("model_\"v1\".tflite", entry.fileName)
        assertEquals("https://example.com/path/\"model\".zip", entry.url)
    }

    @Test
    fun parseManifest_extraUnknownKeysAndNestedObjects_ignoresExtraContentCleanly() {
        val json = """
            {
              "schema_version": 2,
              "unknown_primitive": "ignored_value",
              "nested_unknown_object": {
                "foo": "bar",
                "nested_array": [1, 2, 3]
              },
              "scene_classifier": {
                "file_name": "scene.tflite",
                "url": null,
                "extra_key": "ignored_extra"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("scene_classifier", entry.key)
        assertEquals("scene.tflite", entry.fileName)
        assertNull(entry.url)
    }

    @Test
    fun parseManifest_duplicateKeys_handlesDuplicatesCleanly() {
        val json = """
            {
              "schema_version": 2,
              "scene_classifier": {
                "file_name": "old_scene.tflite"
              },
              "scene_classifier": {
                "file_name": "new_scene.tflite",
                "url": "https://example.com/new.zip"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("scene_classifier", entry.key)
        assertEquals("new_scene.tflite", entry.fileName)
        assertEquals("https://example.com/new.zip", entry.url)
    }

    @Test
    fun parseManifest_validV2Schema_parsesAllEntries() {
        val json = """
            {
              "schema_version": 2,
              "scene_classifier": {
                "file_name": "scene_classifier.tflite",
                "url": null,
                "sha256": null
              },
              "lama_dilated": {
                "file_name": "lama_dilated.tflite",
                "url": "https://example.com/lama.zip",
                "sha256": "abc123sha"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(2, entries.size)
        val scene = entries.first { it.key == "scene_classifier" }
        assertEquals("scene_classifier.tflite", scene.fileName)
        assertNull(scene.url)
        assertNull(scene.sha256)

        val lama = entries.first { it.key == "lama_dilated" }
        assertEquals("lama_dilated.tflite", lama.fileName)
        assertEquals("https://example.com/lama.zip", lama.url)
        assertEquals("abc123sha", lama.sha256)
    }

    @Test
    fun parseManifest_missingKeys_usesFallbacksWithoutCrashing() {
        val json = """
            {
              "schema_version": 2,
              "custom_model": {
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val custom = entries.first()
        assertEquals("custom_model", custom.key)
        assertEquals("custom_model.tflite", custom.fileName)
        assertNull(custom.url)
        assertNull(custom.sha256)
    }

    @Test
    fun parseManifest_malformedJson_returnsEmptyListWithoutCrashing() {
        val malformedJson = "{ invalid json content ... "

        val entries = ManifestParser.parseManifest(malformedJson)

        assertTrue(entries.isEmpty())
    }

    @Test
    fun parseManifest_nullUrl_parsesUrlAsNull() {
        val json = """
            {
              "model_key": {
                "file_name": "test.tflite",
                "url": "null"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        assertNull(entries.first().url)
    }

    @Test
    fun parseManifest_lamaDilatedRealManifestEntry_parsesLamaManifestCorrectly() {
        val json = """
            {
              "schema_version": 2,
              "lama_dilated": {
                "file_name": "lama_dilated.tflite",
                "url": "https://github.com/KanakSaini04/Lensora_Ai/releases/download/models-v1/lama_dilated.tflite",
                "sha256": "a2ac8ed174be1233e4e518c8ef57fb943ea97162696dcd2d428c056280efe583"
              }
            }
        """.trimIndent()

        val entries = ManifestParser.parseManifest(json)

        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("lama_dilated", entry.key)
        assertEquals("lama_dilated.tflite", entry.fileName)
        assertEquals("https://github.com/KanakSaini04/Lensora_Ai/releases/download/models-v1/lama_dilated.tflite", entry.url)
        assertEquals("a2ac8ed174be1233e4e518c8ef57fb943ea97162696dcd2d428c056280efe583", entry.sha256)
    }
}
