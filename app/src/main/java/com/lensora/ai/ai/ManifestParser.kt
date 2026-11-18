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
            val entriesMap = mutableMapOf<String, ModelEntry>()
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return emptyList()
            }

            val body = trimmed.substring(1, trimmed.length - 1)
            val blocks = splitTopLevelBlocks(body)

            for ((key, content) in blocks) {
                if (key == "schema_version") continue
                val trimmedContent = content.trim()
                if (!trimmedContent.startsWith("{")) continue

                val inner = trimmedContent.substring(1, trimmedContent.length - 1)
                if (inner.contains('{') || inner.contains('[')) {
                    continue
                }

                val fileName = extractJsonValue(content, "file_name")
                    ?: extractJsonValue(content, "model_name")
                    ?: "$key.tflite"

                val url = extractJsonValue(content, "url")
                val sha256 = extractJsonValue(content, "sha256")

                entriesMap[key] = ModelEntry(
                    key = key,
                    fileName = fileName,
                    url = url,
                    sha256 = sha256,
                )
            }
            entriesMap.values.toList()
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
            val keyEnd = findClosingQuote(body, keyStart)
            if (keyEnd == -1) break

            val key = unescapeString(body.substring(keyStart + 1, keyEnd))
            val colonIdx = body.indexOf(':', keyEnd + 1)
            if (colonIdx == -1) break

            var valueStart = colonIdx + 1
            while (valueStart < len && body[valueStart].isWhitespace()) {
                valueStart++
            }

            if (valueStart < len && body[valueStart] == '{') {
                var depth = 1
                var valueEnd = valueStart + 1
                var inString = false
                var escaped = false

                while (valueEnd < len && depth > 0) {
                    val c = body[valueEnd]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '"') {
                        inString = !inString
                    } else if (!inString) {
                        if (c == '{') depth++
                        if (c == '}') depth--
                    }
                    valueEnd++
                }
                val content = body.substring(valueStart, valueEnd)
                list.add(Pair(key, content))
                i = valueEnd
            } else {
                var valueEnd = valueStart
                var inString = false
                var escaped = false

                while (valueEnd < len) {
                    val c = body[valueEnd]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '"') {
                        inString = !inString
                    } else if (!inString && (c == ',' || c == '}')) {
                        break
                    }
                    valueEnd++
                }
                val content = body.substring(valueStart, valueEnd)
                list.add(Pair(key, content))
                i = valueEnd + 1
            }
        }
        return list
    }

    private fun findClosingQuote(str: String, startQuoteIdx: Int): Int {
        var i = startQuoteIdx + 1
        val len = str.length
        var escaped = false

        while (i < len) {
            val c = str[i]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                return i
            }
            i++
        }
        return -1
    }

    private fun extractJsonValue(jsonBlock: String, key: String): String? {
        val keyPattern = Regex("\"$key\"\\s*:\\s*")
        val match = keyPattern.find(jsonBlock) ?: return null
        var idx = match.range.last + 1
        val len = jsonBlock.length

        while (idx < len && jsonBlock[idx].isWhitespace()) {
            idx++
        }
        if (idx >= len) return null

        if (jsonBlock.startsWith("null", idx, ignoreCase = true)) {
            return null
        }

        if (jsonBlock[idx] == '"') {
            val closingIdx = findClosingQuote(jsonBlock, idx)
            if (closingIdx == -1) return null
            val rawQuoted = jsonBlock.substring(idx + 1, closingIdx)
            val result = unescapeString(rawQuoted)
            return if (result.equals("null", ignoreCase = true)) null else result
        }

        var end = idx
        while (end < len && jsonBlock[end] != ',' && jsonBlock[end] != '}' && !jsonBlock[end].isWhitespace()) {
            end++
        }
        val unquoted = jsonBlock.substring(idx, end).trim()
        return if (unquoted.isEmpty() || unquoted.equals("null", ignoreCase = true)) null else unquoted
    }

    private fun unescapeString(str: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = str.length
        var escaped = false

        while (i < len) {
            val c = str[i]
            if (escaped) {
                when (c) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(c)
                }
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}
