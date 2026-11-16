# Lensora_Ai Comprehensive Architecture & Phase 23 Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents the completion of **Phase 23: Foreground Service AI Pipeline (HyperOS Kill Protection)**. On aggressive OEM Android distributions (such as Xiaomi HyperOS 3.0 and Android 16 background quota management), WorkManager background jobs are subject to JobScheduler quotas and risk silent termination or execution delays during heavy CPU/GPU computational tasks. To guarantee OS kill protection and uninterrupted execution, the L3 AI computational pipeline has been migrated from WorkManager to a dedicated **Foreground Service** (`AiProcessorService.kt`). Triggered immediately upon capture completion, the service promotes itself to foreground status with a persistent silent notification (`✨ AI: Perfecting your photo...`), executes the 512x512 tiled processing pass on `Dispatchers.IO`, enqueues cloud sync if enabled, and cleanly stops itself upon completion (`stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()`).

---

## 1. Architectural & Rule Compliance Setup

- **Foreground Service Migration**: L3 heavy computational tasks run inside `AiProcessorService.kt` extending `android.app.Service()`.
- **Manifest Permissions**:
  - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />`
  - `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`
  - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
- **Service Declaration**: `<service android:name=".service.AiProcessorService" android:exported="false" android:foregroundServiceType="dataSync" />`
- **Runtime Notification Permissions**: `MainActivity.kt` registers runtime permission launcher for `POST_NOTIFICATIONS` on Android 13+ (API 33+).
- **Service Lifecycle & Notification Channel**:
  - Creates Notification Channel `"ai_processing_channel"` with `NotificationManager.IMPORTANCE_LOW` (silent, non-intrusive).
  - Invokes `startForeground(1001, notification)` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC` on Android 10+.
  - Bound to `CoroutineScope(Dispatchers.IO + SupervisorJob())`.
  - Automatically calls `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf()` when the processing pass finishes or fails.

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 23 Service Lifecycle Audit Matrix

| Component | Target Action | Lifecycle Management | Notification Display | Result |
| :--- | :--- | :--- | :--- | :---: |
| `AiProcessorService.kt` | Foreground Promotion | `startForeground(1001, notification)` | `"✨ AI: Perfecting your photo..."` | `PASS` |
| `AiProcessorService.kt` | Tiled Processing Pass | `serviceScope.launch(Dispatchers.IO)` | Silent Low Importance Channel | `PASS` |
| `AiProcessorService.kt` | Service Cleanup | `stopForeground(STOP_FOREGROUND_REMOVE)` + `stopSelf()` | Notification Dismissed | `PASS` |
| `CameraController.kt` | Capture Flow Trigger | `ContextCompat.startForegroundService(context, serviceIntent)` | `PHOTO_URI`, `SCENE_TYPE`, `VIBE_PRESET` | `PASS` |
| `MainActivity.kt` | Runtime Permission | `registerForActivityResult(ActivityResultContracts.RequestPermission())` | `POST_NOTIFICATIONS` Prompt | `PASS` |

---

## 4. Verification & Logcat Crash Audit

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **ADB Permission Grants**: Granted `CAMERA`, `RECORD_AUDIO`, and `POST_NOTIFICATIONS` permissions programmatically.
- **Service Execution Verification**: Logcat logs confirm foreground service startup, tiling pass execution, and clean termination:
  - `CameraController: Started L3 AiProcessorService for content://media/external/images/media/...`
  - `AiProcessorService: Executing L3 512x512 Tiled Processing pass in Foreground Service`
  - `AiProcessorService: Service processing 4080 x 3060 px in 48 tiles (512 x 512 px each)`
  - `AiProcessorService: L3 Service 512x512 Tiled AI Processing finished in 920ms`
  - `AiProcessorService: Cleaning up AiProcessorService and dismissing notification.`
  - `AiProcessorService: AiProcessorService destroyed`
- **Logcat Crash Audit**: `adb logcat -d -s AndroidRuntime:E` - **0 Runtime Crashes (`AndroidRuntime:E`)**.
