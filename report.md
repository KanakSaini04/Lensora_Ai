# Phase 1, Step 2 Report: Hardware Capability Probes & Documentation

## Executive Summary

Phase 1, Step 2 has been successfully completed for **Lensora_Ai**. We built the **Probe Lab** to audit Camera2 hardware capability characteristics (200MP ultra-high resolution, RAW sensor support, 24fps target frame rates) without opening camera devices, ensuring 100% crash safety via try-catch blocks.

The app was deployed and verified live on physical hardware (**Xiaomi Redmi Note 13 Pro 5G**).

---

## 1. Documentation Setup
- **`agent.md`**: Placed in project root with mandatory AI Agent rules.
- **`master_prompt.md`**: Placed in project root with Lensora_Ai master architecture and L0-L4 constraints.

---

## 2. Hardware Capability Probes & Live Device Results (Xiaomi Redmi Note 13 Pro 5G)

1. **`CameraProbeP1.kt` (200MP Probe)**:
   - **Status**: `FAIL`
   - **Details**: Max resolution exposed via standard Camera2 characteristics is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics.
   - **Latency**: 113 ms

2. **`CameraProbeP2.kt` (RAW Probe)**:
   - **Status**: `PASS`
   - **Details**: `Camera2 REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on camera ID 0.
   - **Latency**: 6 ms

3. **`CameraProbeP3.kt` (24fps Probe)**:
   - **Status**: `PASS`
   - **Details**: Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`.
   - **Latency**: 5 ms

---

## 3. UI & Navigation Integration

1. **`ProbeLabScreen.kt`**:
   - Obsidian Royal UI (#0E0E0E Pitch Black, #131313 Surface, #2563EB Royal Blue).
   - Displays real-time status badges (`PASS` green, `FAIL` red, `UNSUPPORTED` amber, `ERROR` bright red).
   - Shows detailed audit logs and execution latency per probe.

2. **`SettingsScreen.kt` & `Navigation.kt`**:
   - Added `SettingsScreen` with "Launch Probe Lab" button.
   - Added `probe_lab` navigation route to `AppNavigation`.

---

# Phase 1, Step 3 Report: L0 Safe Core - Camera2 Preview & Capture Engine

## Executive Summary

Phase 1, Step 3 implements the **L0 Safe Core Camera2 Engine** providing edge-to-edge live viewfinder preview and atomic JPEG capture (12.5MP optimal) saved directly to `MediaStore` under `Pictures/Lensora_Ai`.

---

## 1. Camera2 Engine & L0 Atomic Capture Journal

1. **`CameraController.kt`**:
   - Pure Camera2 API wrapper managing background thread handler, camera open/close lifecycle, continuous auto-focus, and capture sessions.
   - **JPEG Orientation Calculation**: Fixed 90-degree photo rotation bug by calculating `JPEG_ORIENTATION = (sensorOrientation - deviceRotationDegrees + 360) % 360`.
   - **L0 Atomic Capture (.tmp rename)**:
     1. Captures JPEG bytes from `ImageReader`.
     2. Writes temporary file `cacheDir/LENSORA_TIMESTAMP.jpg.tmp`.
     3. Inserts MediaStore row with `IS_PENDING = 1` under `Pictures/Lensora_Ai`.
     4. Copies stream from `.tmp` file to MediaStore output stream.
     5. Updates row to `IS_PENDING = 0` and deletes temporary `.tmp` file.
   - **100% Try-Catch Safety**: Every Camera2 HAL interaction is safely wrapped in try-catch to prevent hardware/HAL crashes.

2. **`CameraPreview.kt`**:
   - `AndroidView` wrapping a `TextureView` for edge-to-edge hardware-accelerated preview rendering.

3. **`CameraScreen.kt` & UI Enhancements**:
   - Handles `CAMERA` runtime permission checks safely.
   - Displays black screen with "Grant Permission" button when permission is not granted.
   - Floating double-concentric white circle shutter button in Obsidian Royal styling.
   - **Gallery Thumbnail Preview**: Displays a rounded thumbnail in bottom left corner showing the last captured photo loaded directly from MediaStore.

---

## 2. Files Touched in Step 3 Follow-up

- `app/src/main/java/com/lensora/ai/camera/CameraController.kt` (MODIFIED - Calculated JPEG_ORIENTATION)
- `app/src/main/java/com/lensora/ai/ui/camera/CameraScreen.kt` (MODIFIED - Added gallery thumbnail preview)
- `report.md` (MODIFIED - Appended camera rotation & UI thumbnail report)

---

## 3. Git PR Workflow

Execute the following commands in terminal:

```bash
git add .
git commit -m "fix(camera): correct jpeg orientation and add gallery thumbnail"
git push -u origin fix/camera-rotation-ui
gh pr create --title "fix(camera): orientation and thumbnail" --body "Fixes rotated saves and adds UI thumbnail" --base dev --head fix/camera-rotation-ui
```
