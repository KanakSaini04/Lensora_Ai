package com.lensora.ai.ai

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object Sha256Util {

    fun hex(file: File): String {
        if (!ModelFileGuard.isUsable(file)) return ""

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { stream ->
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    fun matches(file: File, expectedSha256: String?): Boolean {
        if (expectedSha256.isNullOrBlank()) return true
        if (!ModelFileGuard.isUsable(file)) return false
        val computedHex = hex(file)
        return computedHex.equals(expectedSha256.trim(), ignoreCase = true)
    }
}
