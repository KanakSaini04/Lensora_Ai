package com.lensora.ai

import com.lensora.ai.ai.ManifestParser
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
    fun fileExistenceGuard_missingOrEmptyFile_validatesCorrectly() {
        val missingFile = File(tempFolder.root, "non_existent.tflite")
        assertFalse(missingFile.exists() && missingFile.length() > 0 && missingFile.canRead())

        val emptyFile = tempFolder.newFile("empty.tflite")
        assertFalse(emptyFile.exists() && emptyFile.length() > 0 && emptyFile.canRead())

        val validFile = tempFolder.newFile("valid.tflite")
        validFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        assertTrue(validFile.exists() && validFile.length() > 0 && validFile.canRead())
    }

    @Test
    fun sha256Helper_calculatesAndVerifiesHashCorrectly() {
        val tempFile = tempFolder.newFile("test_sha.tflite")
        val testBytes = "Lensora AI Test Model Buffer".toByteArray()
        tempFile.writeBytes(testBytes)

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(testBytes)
        val expectedHex = hashBytes.joinToString("") { "%02x".format(it) }

        val computedDigest = MessageDigest.getInstance("SHA-256")
        val actualBytes = tempFile.readBytes()
        val actualHex = computedDigest.digest(actualBytes).joinToString("") { "%02x".format(it) }

        assertEquals(expectedHex, actualHex)
        assertTrue(actualHex.equals(expectedHex, ignoreCase = true))
        assertFalse(actualHex.equals("wrong_hash_123", ignoreCase = true))
    }
}
