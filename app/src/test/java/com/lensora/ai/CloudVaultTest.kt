package com.lensora.ai

import androidx.work.NetworkType
import com.lensora.ai.ai.CloudSyncWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class CloudVaultTest {

    @Test
    fun cloudSyncWorker_wifiOnlyConstraint_verifiesUnmeteredNetwork() {
        val workRequest = CloudSyncWorker.createWifiOnlySyncRequest(
            photoUriStr = "content://media/external/images/media/100",
            fileName = "test_photo.jpg",
            autoPurgeEnabled = true,
        )

        assertNotNull(workRequest)
        val constraints = workRequest.workSpec.constraints
        assertEquals(NetworkType.UNMETERED, constraints.requiredNetworkType)
    }

    @Test
    fun sha256Checksum_calculatesCorrectHexRepresentation() {
        val input = "Lensora_Ai Cloud Vault Integrity Test Payload".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input)
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }

        assertNotNull(hexString)
        assertEquals(64, hexString.length) // SHA-256 is 256 bits = 64 hex chars
    }

    @Test
    fun cloudSyncInputData_passesParametersCorrectly() {
        val workRequest = CloudSyncWorker.createWifiOnlySyncRequest(
            photoUriStr = "file:///sdcard/test.jpg",
            fileName = "custom_name.jpg",
            autoPurgeEnabled = false,
        )

        val inputData = workRequest.workSpec.input
        assertEquals("file:///sdcard/test.jpg", inputData.getString("PHOTO_URI"))
        assertEquals("custom_name.jpg", inputData.getString("FILE_NAME"))
        assertTrue(!inputData.getBoolean("AUTO_PURGE_ENABLED", true))
    }
}
