# Lensora_Ai Physical Device Audit Protocol
**Target Hardware**: Redmi Note 13 Pro 5G (Snapdragon 7s Gen 2, 8GB RAM, 200MP Main Camera, Android 14/15/16)

---

## Executive Overview
This document specifies the strict physical hardware audit protocol for validating **Lensora_Ai** (`com.lensora.ai`) on real device hardware. Tests evaluate thermal behavior, RAM memory shields, GPU Delegate acceleration, Camera2 HAL stability, Google Drive Vault operations, and overall System Diagnostics.

---

## 1. Physical Device Audit Matrix & Test Procedures

### Test 1: Rapid Capture & Thermal Stability (Stress Test)
- **Goal**: Verify continuous Camera2 photo capture without triggering `SEVERE` or `CRITICAL` thermal throttling.
- **Steps**:
  1. Open app, set Mode to **AI AUTO**.
  2. Capture 10 photos in rapid succession (1 capture every 2 seconds).
  3. Navigate to **Settings -> System Diagnostics**.
  4. Observe Battery Temperature and Thermal Status.
- **Pass Criteria**: Thermal status remains `NONE` or `LIGHT` ($< 42^\circ\text{C}$). 0 app crashes.

### Test 2: Real-Time LaMa Object Eraser (GPU Inpainting)
- **Goal**: Verify LaMa GPU Delegate initialization, 4-channel tensor streaming, and $512 \times 512$ tile Memory Shield.
- **Steps**:
  1. Go to **Settings -> Manage AI Engines**, tap **Download LaMa Model**.
  2. Confirm status reads `LaMa Dilated: Ready`.
  3. Navigate to **Studio**, tap **Eraser**, draw a brush mask over an object.
  4. Tap **Process Eraser**.
  5. Check Logcat (`adb logcat -s ObjectEraser`):
     - `ObjectEraser: GpuDelegate initialized for LaMa Inpainting model`
     - `ObjectEraser: Loaded LaMa-Dilated TFLite Model`
     - `ObjectEraser: LaMa-Dilated GPU Inpainting pass completed in <500ms`
- **Pass Criteria**: Masked object cleanly inpainted, RAM usage spike $< 100\text{MB}$, 0 OOM crashes.

### Test 3: Foreground Service AI Engine (HyperOS Protection)
- **Goal**: Verify foreground service lifecycle (`AiProcessorService.kt`) under HyperOS background task management.
- **Steps**:
  1. Capture a high-res photo in **PRO** mode.
  2. Immediately minimize the app (press Home button).
  3. Check notification bar for persistent notification: `"✨ AI: Perfecting your photo..."`.
  4. Wait 3 seconds, reopen app, open **Gallery**.
- **Pass Criteria**: Output image saved to MediaStore (`Pictures/Lensora_Ai`), service cleanly self-stops (`stopSelf()`).

### Test 4: Real Google Drive Cloud Vault & Auto-Purge
- **Goal**: Verify OAuth2 token persistence, Wi-Fi unmetered resumable upload, and SHA-256 local MediaStore purge.
- **Steps**:
  1. Open **Settings**, tap **Sign in to Google Drive**.
  2. Enable **Cloud Sync (Google Drive)** and **Auto-Purge Local Masters**.
  3. Connect to Wi-Fi network.
  4. Capture a photo in **PHOTO** mode.
  5. Check Logcat (`adb logcat -s CloudSyncWorker`):
     - `CloudSyncWorker: Upload verified (SHA-256 matched)`
     - `CloudSyncWorker: Auto-Purge deleted local master file`
- **Pass Criteria**: Photo uploaded to Google Drive Vault, local master purged cleanly.

### Test 5: Pro Mode HUD & RAW DNG Capture
- **Goal**: Verify manual ISO/Shutter speed sliders and uncompressed DNG capture.
- **Steps**:
  1. Set Mode to **PRO**.
  2. Adjust ISO slider to `800` and Shutter Speed to `1/250s`.
  3. Toggle **Save RAW (DNG)** switch ON.
  4. Tap Shutter button.
- **Pass Criteria**: DNG raw file saved to `Pictures/Lensora_Ai/RAW/` (`.dng` format, $> 15\text{MB}$).

### Test 6: 24fps Cinematic Video & Letterboxing
- **Goal**: Verify 24fps target range lock (`Range(24, 24)`) and 2.39:1 aspect ratio letterboxing bars.
- **Steps**:
  1. Set Mode to **CINEMATIC**.
  2. Verify 2.39:1 top and bottom letterboxing bars overlay on viewfinder.
  3. Tap red recording button, record 10 seconds of video.
  4. Tap stop recording button.
- **Pass Criteria**: Video saved to `Movies/Lensora_Ai/` (`.mp4` format, H264/AAC, 24fps frame rate).

---

## 2. System Diagnostics Threshold Standards

| Metric | Target Value | Warning Threshold | Critical Threshold |
| :--- | :--- | :--- | :--- |
| **RAM Usage** | $< 4.5\text{ GB} / 8\text{ GB}$ ($< 55\%$) | $> 6.0\text{ GB}$ ($> 75\%$) | $> 7.2\text{ GB}$ ($> 90\%$) |
| **Battery Temp** | $25^\circ\text{C} - 38^\circ\text{C}$ | $40^\circ\text{C} - 44^\circ\text{C}$ | $> 45^\circ\text{C}$ |
| **Thermal Status** | `NONE` | `LIGHT` / `MODERATE` | `SEVERE` / `CRITICAL` |
| **LaMa GPU Latency** | $< 450\text{ ms}$ | $450\text{ ms} - 800\text{ ms}$ | $> 1000\text{ ms}$ |

---

## 3. Physical Device Hardware Audit Checklist

- [x] **P1 Probe**: 12.5MP binned stream exposed cleanly via Camera2 HAL.
- [x] **P2 Probe**: RAW_SENSOR capability verified.
- [x] **P3 Probe**: 24fps fixed frame rate range verified.
- [x] **LaMa GPU Delegate**: TFLite inpainting verified with $512 \times 512$ tile Memory Shield.
- [x] **Cloud Vault**: Resumable upload & SHA-256 Auto-Purge verified.
- [x] **Logcat Crash Audit**: 0 Runtime Crashes (`AndroidRuntime:E`).
