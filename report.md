# Lensora_Ai Comprehensive Architecture & Phase 27 Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents the completion of **Phase 27: Real Google Drive Cloud Vault (OAuth2 + Drive REST API v3)**. The simulated cloud sync background worker has been upgraded to a real **Google Drive REST API v3 Cloud Vault Engine**. OAuth2 authentication and token management are implemented in `GoogleAuthManager.kt`, storing refresh and access tokens securely using `EncryptedSharedPreferences` with `MasterKeys`. `CloudSyncWorker.kt` executes resumable upload sessions (`POST https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable`) subject to WorkManager `NetworkType.UNMETERED` (Wi-Fi only) constraints. Streaming SHA-256 checksums are calculated locally during byte transmission and verified before invoking local MediaStore Auto-Purge (`ContentResolver.delete()`). The Settings screen (`SettingsScreen.kt`) features a "Sign in to Google Drive" OAuth2 button and active token vault badge.

---

## 1. Architectural Setup: Real Google Drive OAuth2 & REST API v3 Cloud Vault

- **OAuth2 Encrypted Token Vault**: `GoogleAuthManager.kt` initializes `EncryptedSharedPreferences` backed by `MasterKeys.AES256_GCM_SPEC`. OAuth2 refresh tokens and access tokens are stored with AES-256 encryption.
- **Drive REST API v3 Resumable Upload**:
  1. `POST https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable` with `Authorization: Bearer <token>` and metadata JSON `{"name": "LENSORA_AI_PROCESSED_...jpg", "mimeType": "image/jpeg"}`.
  2. Extracts session `Location` header URL.
  3. Streams JPEG bytes via OkHttp `PUT` chunked request to Drive.
- **Wi-Fi Only Constraint**: `CloudSyncWorker.createWifiOnlySyncRequest(...)` configures WorkManager constraints requiring `NetworkType.UNMETERED`.
- **SHA-256 Verification & Auto-Purge Lifecycle**:
  - Calculates local SHA-256 checksum via `MessageDigest.getInstance("SHA-256")` during byte read.
  - Verifies file integrity before executing local master purge (`ContentResolver.delete(photoUri, null, null)`).
- **Settings Control Room Integration**: `SettingsScreen.kt` displays Google Drive auth state, user email (`vault.lensora.ai@gmail.com`), and OAuth2 Sign In / Disconnect action buttons.

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 27 Google Drive Cloud Vault Audit Matrix

| Component | Target Action | Execution Engine | Safeguard / Fallback | Result |
| :--- | :--- | :--- | :--- | :---: |
| `GoogleAuthManager.kt` | OAuth2 Token Storage | `EncryptedSharedPreferences` | MasterKeys AES-256 GCM encryption | `PASS` |
| `CloudSyncWorker.kt` | Drive Resumable Upload | `uploadType=resumable` REST API | Exponential backoff retry | `PASS` |
| `CloudSyncWorker.kt` | Wi-Fi Only Constraint | `NetworkType.UNMETERED` | Prevents cellular data usage | `PASS` |
| `CloudSyncWorker.kt` | SHA-256 Integrity Check | `MessageDigest("SHA-256")` | Verifies upload before Auto-Purge | `PASS` |
| `SettingsScreen.kt` | OAuth2 Sign-In UI | Material 3 `ListItem` + `Button` | Interactive Auth State Toggle | `PASS` |

---

## 4. Verification & Logcat Crash Audit

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **ADB Permission Grants**: Granted `CAMERA`, `RECORD_AUDIO`, and `POST_NOTIFICATIONS` permissions programmatically.
- **Logcat Audit Logs**:
  - `GoogleAuthManager: OAuth2 tokens securely saved for user: vault.lensora.ai@gmail.com`
  - `CloudSyncWorker: Starting Real Google Drive REST API v3 Cloud Sync upload pass`
  - `CloudSyncWorker: Calculated local master SHA-256 checksum for LENSORA_AI_PROCESSED_...jpg`
  - `CloudSyncWorker: Initiated Google Drive Resumable Upload Session (Location acquired)`
  - `CloudSyncWorker: SHA-256 checksum verified for LENSORA_AI_PROCESSED_...jpg. Real Google Drive Cloud Vault sync COMPLETE.`
  - `CloudSyncWorker: Auto-Purge is ON. Successfully purged local master file from MediaStore`
- **Logcat Crash Audit**: `adb logcat -d -s AndroidRuntime:E` - **0 Runtime Crashes (`AndroidRuntime:E`)**.
