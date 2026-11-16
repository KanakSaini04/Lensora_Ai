# Comprehensive Test Plan & Quality Matrix - Lensora_Ai

**Application Name**: Lensora_Ai  
**Package Name**: `com.lensora.ai`  
**Test Coverage Target**: 100% Core Architectural Layer Verification (L0 - L4)  
**Execution Devices**: Physical Xiaomi Redmi Note 13 Pro 5G & Android Studio Emulator (`emulator-5554`)  

---

## 1. Test Strategy Overview

Lensora_Ai quality assurance combines static code inspection, automated Gradle build checks, hardware capability diagnostics via Probe Lab, end-to-end Golden Path workflow verification, boundary condition testing for safety guards (Thermal & Storage), and Logcat crash auditing.

---

## 2. Test Execution Matrix

| Test Suite ID | Test Case Title | Target Module / Component | Verification Methodology | Target Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **TP-01** | Clean Build Verification | Root Gradle & Hilt DI | Run `./gradlew assembleDebug` | `BUILD SUCCESSFUL` with 0 compilation errors. |
| **TP-02** | Installation & Direct Boot | `MainActivity` / Manifest | Run `./gradlew installDebug` & reboot emulator | App launches without Direct Boot `SharedPreferences` exception. |
| **TP-03** | Probe Lab P1 (200MP Probe) | `ProbeLabScreen.kt` / Probe Engine | Execute Probe P1 in Settings | Correctly identifies max standard Camera2 stream map resolution (12.5MP) & logs binning behavior. |
| **TP-04** | Probe Lab P2 (RAW Sensor) | `ProbeLabScreen.kt` / Hardware Map | Execute Probe P2 in Settings | `REQUEST_AVAILABLE_CAPABILITIES_RAW` reports `PASS`. |
| **TP-05** | Probe Lab P3 (24fps Target) | `ProbeLabScreen.kt` / AE Ranges | Execute Probe P3 in Settings | Confirms target 24fps AE range availability (`[24, 24]`). |
| **TP-06** | L0 Atomic Capture Journal | `CameraController.kt` | Capture photo in PHOTO mode | `.tmp` journal created in `cacheDir`, streamed to MediaStore, and deleted cleanly. |
| **TP-07** | Storage Guard Threshold | `CameraController.kt` | Simulate storage space < 2GB | Capture blocked and Toast warning displayed: `"Storage Low... Minimum 2GB space required."` |
| **TP-08** | Thermal Guard Monitoring | `CameraController.kt` | Monitor thermal status callback | Registers `PowerManager.OnThermalStatusChangedListener` and logs throttling warnings. |
| **TP-09** | L1 AI Scene Classifier | `SceneClassifier.kt` | Live preview sampling at 2 FPS | Dynamically updates AI Badge text (e.g. `✨ AI: Portrait`, `✨ AI: Landscape`). |
| **TP-10** | L2 AI Execution Planner | `AiPlanner.kt` | Scene detection loop | Maps `SceneType` to exposure compensation offsets (`targetEvOffset`) & AF mode. |
| **TP-11** | L3 AI Processing Worker | `AiProcessorWorker.kt` | Photo capture completion | WorkManager enqueues L3 worker, applies ColorMatrix / Vibe LUT, and saves processed photo to MediaStore. |
| **TP-12** | L4 Quality Validation | `Validator.kt` | Pre-finalization pass | Validates bitmap integrity & average luminance bounds (`avgLum > 2`). |
| **TP-13** | Pinterest Staggered Gallery | `GalleryScreen.kt` | Navigate to Gallery tab | Staggered 2-column masonry grid displays MediaStore images with Coil `AsyncImage`. |
| **TP-14** | Studio Real-Time Editing | `StudioScreen.kt` / `ImageEditor.kt` | Drag Exposure & Contrast sliders | 60fps GPU preview updates without bitmap reallocation or OOM crashes. |
| **TP-15** | Studio Export Pipeline | `ImageEditor.kt` | Tap Export in Studio | Software `Canvas` renders full-res image with Vibe LUT & timestamp, saving to MediaStore. |
| **TP-16** | AI Object Eraser (LaMa) | `ObjectEraser.kt` / `StudioScreen` | Draw brush mask & tap Process | Mask strokes rendered, loading overlay shown, and inpainted bitmap updated. |
| **TP-17** | GitHub DLC Downloader | `ModelDownloaderWorker.kt` | Tap Download MobileNet in Settings | Manifest parsed, ZIP downloaded, extracted to `filesDir/models/`, SHA-256 verified. |
| **TP-18** | Pro Mode Manual Exposure | `ProModeControls.kt` | Drag ISO & Shutter sliders | Camera2 HAL applies `SENSOR_SENSITIVITY` and `SENSOR_EXPOSURE_TIME`. |
| **TP-19** | Video & Cinematic 24fps | `VideoRecorder.kt` / `CameraScreen` | Record in VIDEO & CINEMATIC modes | MP4 file saved to `Movies/Lensora_Ai`; Cinematic mode renders 12% top/bottom black bars (76% center viewfinder). |
| **TP-20** | Astro Mode Hardware Wiring | `CameraController.kt` | Select ASTRO from MORE menu | Camera2 HAL locks focus to infinity (`0.0f`) & sets 30s exposure; preview renders raw clear TextureView. |
| **TP-21** | Logcat Crash Audit | Android System Runtime | `adb logcat -d -s AndroidRuntime:E` | Confirms **0 Runtime Crashes (`AndroidRuntime:E`)**. |

---

## 3. Automated Command Execution Protocol

```powershell
# 1. Clean Build & Deploy
./gradlew installDebug

# 2. Programmatic Permission Grant
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell "pm grant com.lensora.ai android.permission.CAMERA"
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell "pm grant com.lensora.ai android.permission.RECORD_AUDIO"

# 3. Launch App
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell "am force-stop com.lensora.ai"
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell "am start -n com.lensora.ai/.MainActivity"

# 4. Crash Audit
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 logcat -d -s AndroidRuntime:E
```
