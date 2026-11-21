package com.lensora.ai

import android.app.Application
import android.content.SharedPreferences
import com.lensora.ai.ai.CloudSyncWorker
import com.lensora.ai.auth.GoogleAuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSharedPreferences : SharedPreferences {
    private val map = HashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { key?.let { map[it] = value } }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { key?.let { map[it] = values } }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { key?.let { map[it] = value } }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { key?.let { map[it] = value } }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { key?.let { map[it] = value } }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { key?.let { map[it] = value } }
        override fun remove(key: String?): SharedPreferences.Editor = apply { map.remove(key) }
        override fun clear(): SharedPreferences.Editor = apply { map.clear() }
        override fun commit(): Boolean = true
        override fun apply() {}
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

class GoogleAuthManagerTest {

    @Test
    fun googleSignIn_withKanakSainiEmail_savesAccountAndTokenCorrectly() {
        val fakeContext = Application()
        val fakePrefs = FakeSharedPreferences()
        val authManager = GoogleAuthManager(fakeContext, fakePrefs)

        // Initially signed out
        assertFalse(authManager.isSignedIn())
        assertNull(authManager.getSignedInEmail())
        assertNull(authManager.getValidAccessToken())

        // Perform Sign-In with target user email
        val userEmail = "kanaksaini07330@gmail.com"
        authManager.signInUser(userEmail)

        // Assert signed in state and email match
        assertTrue(authManager.isSignedIn())
        assertEquals("kanaksaini07330@gmail.com", authManager.getSignedInEmail())
        val token = authManager.getValidAccessToken()
        assertNotNull(token)
        assertTrue(token!!.startsWith("ya29.a0ARW5m7LENSORA_DRIVE_OAUTH2_TOKEN_"))

        // Perform Sign-Out
        authManager.signOut()
        assertFalse(authManager.isSignedIn())
        assertNull(authManager.getSignedInEmail())
        assertNull(authManager.getValidAccessToken())
    }

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
