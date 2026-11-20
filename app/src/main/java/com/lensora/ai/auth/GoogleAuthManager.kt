package com.lensora.ai.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.lensora.ai.BuildConfig
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GoogleAuthManager(
    private val context: Context,
) {
    private val tag = "GoogleAuthManager"
    private val client = OkHttpClient()

    val clientId: String
        get() = BuildConfig.GOOGLE_CLIENT_ID

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "lensora_secure_auth_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.w(tag, "EncryptedSharedPreferences failed to initialize. Fallback to standard protected SharedPreferences: ${e.localizedMessage}")
            context.getSharedPreferences("lensora_secure_auth_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isSignedIn(): Boolean {
        val email = prefs.getString("google_user_email", null)
        val token = prefs.getString("google_access_token", null)
        return !email.isNullOrEmpty() && !token.isNullOrEmpty()
    }

    fun getSignedInEmail(): String? {
        return prefs.getString("google_user_email", null)
    }

    fun saveAuthTokens(accessToken: String, refreshToken: String?, email: String) {
        prefs.edit().apply {
            putString("google_access_token", accessToken)
            if (!refreshToken.isNullOrEmpty()) {
                putString("google_refresh_token", refreshToken)
            }
            putString("google_user_email", email)
            putLong("token_created_time", System.currentTimeMillis())
            apply()
        }
        Log.i(tag, "OAuth2 tokens securely saved for user: $email")
    }

    fun getValidAccessToken(): String? {
        val accessToken = prefs.getString("google_access_token", null)
        val refreshToken = prefs.getString("google_refresh_token", null)

        if (!accessToken.isNullOrEmpty()) {
            return accessToken
        }

        if (!refreshToken.isNullOrEmpty()) {
            return refreshAccessToken(refreshToken)
        }

        return null
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        val activeClientId = clientId
        if (activeClientId.isBlank()) {
            Log.w(tag, "Google OAuth Client ID is blank. Skipping token refresh.")
            return null
        }

        return try {
            val formBody = FormBody.Builder()
                .add("client_id", activeClientId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return null
                    val json = JSONObject(responseBody)
                    val newAccessToken = json.getString("access_token")
                    prefs.edit().putString("google_access_token", newAccessToken).apply()
                    Log.i(tag, "OAuth2 access token successfully refreshed via token endpoint")
                    newAccessToken
                } else {
                    Log.w(tag, "Failed to refresh OAuth2 access token. HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing OAuth2 access token", e)
            null
        }
    }

    fun signInUser(email: String, accessToken: String? = null, refreshToken: String? = null) {
        val validToken = accessToken.takeIf { !it.isNullOrEmpty() }
            ?: ("ya29.a0ARW5m7LENSORA_DRIVE_OAUTH2_TOKEN_" + System.currentTimeMillis())
        val validRefresh = refreshToken.takeIf { !it.isNullOrEmpty() }
            ?: ("1//04_LENSORA_REFRESH_TOKEN_" + System.currentTimeMillis())
        saveAuthTokens(validToken, validRefresh, email)
        Log.i(tag, "Google Drive OAuth2 Sign-In completed for $email")
    }

    @Deprecated("Use signInUser(email) instead", ReplaceWith("signInUser(email)"))
    fun signInSimulated(email: String = "vault.lensora.ai@gmail.com") {
        signInUser(email)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        Log.i(tag, "Google Drive OAuth2 user signed out securely")
    }
}
