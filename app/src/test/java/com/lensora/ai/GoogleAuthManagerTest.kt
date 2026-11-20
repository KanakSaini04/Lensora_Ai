package com.lensora.ai

import com.lensora.ai.ai.CloudSyncWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthManagerTest {

    @Test
    fun wifiOnlyConstraint_verifiesUnmeteredNetworkRequirement() {
        val workRequest = CloudSyncWorker.createWifiOnlySyncRequest(
            photoUriStr = "content://media/external/images/media/100",
            fileName = "vault_photo.jpg",
            autoPurgeEnabled = true,
        )
        assertNotNull(workRequest)
        assertEquals(androidx.work.NetworkType.UNMETERED, workRequest.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun cloudSyncInputData_verifiesUriAndFileName() {
        val workRequest = CloudSyncWorker.createWifiOnlySyncRequest(
            photoUriStr = "content://media/external/images/media/200",
            fileName = "custom_cloud_master.jpg",
            autoPurgeEnabled = false,
        )
        val input = workRequest.workSpec.input
        assertEquals("content://media/external/images/media/200", input.getString("PHOTO_URI"))
        assertEquals("custom_cloud_master.jpg", input.getString("FILE_NAME"))
        assertFalse(input.getBoolean("AUTO_PURGE_ENABLED", true))
    }
}
