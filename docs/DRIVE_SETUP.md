# Google Drive OAuth2 Setup Guide for Lensora_Ai

This document details the configuration required to enable real Google Drive Cloud Vault backups for **Lensora_Ai**.

---

## 1. Project Identifiers
- **App Package Name**: `com.lensora.ai`
- **Google Drive API Scope**: `https://www.googleapis.com/auth/drive.file` (Per-file access permission for files created by Lensora_Ai)

---

## 2. Obtain Keystore SHA-1 Fingerprints

Run the Gradle `signingReport` task in PowerShell / Command Prompt:

```powershell
.\gradlew.bat signingReport
```

Example output snippet:
```text
Variant: debug
Config: debug
Store: C:\Users\<username>\.android\debug.keystore
Alias: AndroidDebugKey
SHA1: AA:BB:CC:DD:EE:FF:11:22:33:44:55:66:77:88:99:00:11:22:33:44
```

---

## 3. Google Cloud Console Configuration

### Step A: Create a Cloud Project
1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Click **Select a Project** -> **New Project**.
3. Set **Project Name** to `Lensora-Ai-Vault` and click **Create**.

### Step B: Enable Google Drive API
1. In the left sidebar, navigate to **APIs & Services** -> **Library**.
2. Search for **Google Drive API**.
3. Select **Google Drive API** and click **Enable**.

### Step C: Configure OAuth Consent Screen
1. Navigate to **APIs & Services** -> **OAuth consent screen**.
2. Select **External** user type and click **Create**.
3. Fill in basic app information:
   - **App name**: Lensora Ai
   - **User support email**: Your developer email address
   - **Developer contact information**: Your developer email address
4. Under **Scopes**, click **Add or Remove Scopes**:
   - Add `https://www.googleapis.com/auth/drive.file`
5. Under **Test Users**, add your personal Google Account email address (required while in Testing publishing status).

### Step D: Create OAuth 2.0 Credentials
1. Navigate to **APIs & Services** -> **Credentials**.
2. Click **Create Credentials** -> **OAuth client ID**.
3. Select **Application type**: `Android`.
4. Fill in:
   - **Name**: Lensora Android Client (Debug)
   - **Package name**: `com.lensora.ai`
   - **SHA-1 certificate fingerprint**: Paste the SHA-1 output from `.\gradlew.bat signingReport`
5. Click **Create**.

---

## 4. Code Security & Data Isolation Guarantees
- Tokens are stored exclusively on-device in `EncryptedSharedPreferences` (`lensora_secure_auth_prefs`).
- Network requests for cloud sync are constrained to `NetworkType.UNMETERED` (Wi-Fi Only).
- Local files are purged ONLY after receiving an HTTP 200/201 response and verifying the uploaded payload SHA-256 hash.
