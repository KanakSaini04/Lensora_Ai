package com.lensora.ai.ai

data class ModelEntry(
    val key: String,
    val fileName: String,
    val url: String? = null,
    val sha256: String? = null,
)

object ManifestParser {

    fun parseManifest(jsonString: String): List<ModelEntry> {
        if (jsonString.isBlank()) return emptyList()

        return try {
            val entries = mutableListOf<ModelEntry>()
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return emptyList()
            }

            val body = trimmed.substring(1, trimmed.length - 1)
            val blocks = splitTopLevelBlocks(body)

            for ((key, content) in blocks) {
                if (key == "schema_version") continue

                val fileName = extractJsonValue(content, "file_name")
                    ?: extractJsonValue(content, "model_name")
                    ?: "$key.tflite"

                val url = extractJsonValue(content, "url")
                val sha256 = extractJsonValue(content, "sha256")

                entries.add(
                    ModelEntry(
                        key = key,
                        fileName = fileName,
                        url = url,
                        sha256 = sha256,
                    ),
                )
            }
            entries
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun splitTopLevelBlocks(body: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        var i = 0
        val len = body.length

        while (i < len) {
            val keyStart = body.indexOf('"', i)
            if (keyStart == -1) break
            val keyEnd = body.indexOf('"', keyStart + 1)
            if (keyEnd == -1) break

            val key = body.substring(keyStart + 1, keyEnd)
            val colonIdx = body.indexOf(':', keyEnd + 1)
            if (colonIdx == -1) break

            var valueStart = colonIdx + 1
            while (valueStart < len && body[valueStart].isWhitespace()) {
                valueStart++
            }

            if (valueStart < len && body[valueStart] == '{') {
                var depth = 1
                var valueEnd = valueStart + 1
                while (valueEnd < len && depth > 0) {
                    if (body[valueEnd] == '{') depth++
                    if (body[valueEnd] == '}') depth--
                    valueEnd++
                }
                val content = body.substring(valueStart, valueEnd)
                list.add(Pair(key, content))
                i = valueEnd
            } else {
                var valueEnd = body.indexOf(',', valueStart)
                if (valueEnd == -1) valueEnd = len
                val content = body.substring(valueStart, valueEnd)
                list.add(Pair(key, content))
                i = valueEnd + 1
            }
        }
        return list
    }

    private fun extractJsonValue(jsonBlock: String, key: String): String? {
        val keyPattern = "\"$key\"\\s*:\\s*"
        val regex = Regex(keyPattern + "(?:\"([^\"]*)\"|(null)|([^,\\}\\s]+))")
        val match = regex.find(jsonBlock) ?: return null

        val (quotedVal, nullVal, unquotedVal) = match.destructured
        if (nullVal.isNotEmpty() && nullVal.equals("null", ignoreCase = true)) {
            return null
        }
        if (quotedVal.isNotEmpty()) {
            return if (quotedVal.equals("null", ignoreCase = true)) null else quotedVal
        }
        if (unquotedVal.isNotEmpty()) {
            return if (unquotedVal.equals("null", ignoreCase = true)) null else unquotedVal
        }
        return null
    }
}
