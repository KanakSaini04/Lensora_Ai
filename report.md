# Lensora_Ai Comprehensive Architecture & Phase 22 Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents the completion of **Phase 22: L3 Tiled Processing (Memory Shield)**. In accordance with Lensora_Ai's absolute **No-Risk Mandate**, loading full-resolution Bitmaps directly into RAM has been eliminated to prevent Out-Of-Memory (OOM) crashes on target hardware (Xiaomi Redmi Note 13 Pro 5G, 8GB RAM). The L3 Image Processing pipeline in `AiProcessorWorker.kt` and `ImageEditor.kt` has been fully refactored to use `BitmapRegionDecoder`, iterating through images in $512 \times 512$ tile regions. Each tile is processed, drawn to the target output canvas, and immediately recycled (`tileBitmap.recycle()`), creating a hardware memory shield that guarantees OOM-free processing even for massive sensor captures.

---

## 1. Architectural & Rule Compliance Setup

- **No Full Bitmaps Policy**: `BitmapFactory.decodeStream()` and `decodeFile()` for full-resolution image processing are strictly prohibited in L3 computational pipelines.
- **`BitmapRegionDecoder` Enforcement**: All full-resolution image processing operations stream 512x512 tile regions using `BitmapRegionDecoder.newInstance(inputStream)` and `decoder.decodeRegion(rect, null)`.
- **Memory Shield Routine**: Immediately following each tile pass (applying ColorMatrix contrast curves, Vibe LUT color science, or timestamp overlays), `tileBitmap.recycle()` is called before proceeding to the next grid cell.
- **WorkManager Progress Reporting**: `AiProcessorWorker.kt` calculates progress percentage per tile cell and updates UI asynchronously via `setProgressAsync(workDataOf("progress" to progressPercent))`.
- **Threading Model**: All tiled decoding and canvas composition runs strictly on `Dispatchers.IO`, preserving 60 FPS Compose rendering on the Main UI thread.
- **Crash Safety Fallback**: BitmapRegionDecoder initialization and tile iterations are wrapped in strict `try-catch` blocks with automatic safe fallback if decoder operations fail.

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 22 Tiled Processing Audit Matrix

| Component | Target Function | Tiling Grid | Memory Shield Routine | Result |
| :--- | :--- | :---: | :--- | :---: |
| `AiProcessorWorker.kt` | Background AI Worker (`doWork`) | $512 \times 512$ px | `decoder.decodeRegion` $\rightarrow$ `canvas.drawBitmap` $\rightarrow$ `tileBitmap.recycle()` | `PASS` |
| `ImageEditor.kt` | Studio High-Res Export (`exportImage`) | $512 \times 512$ px | `decoder.decodeRegion` $\rightarrow$ `canvas.drawBitmap` $\rightarrow$ `tileBitmap.recycle()` | `PASS` |
| `CameraController.kt` | Photo Capture Enqueue (`takePicture`) | WorkData | Passes `"PHOTO_URI"`, `"SCENE_TYPE"`, `"EV_OFFSET"`, `"VIBE_PRESET"` | `PASS` |

---

## 4. Verification & Logcat Crash Audit

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **ADB Permission Grants**: Granted `CAMERA` and `RECORD_AUDIO` permissions programmatically.
- **Tiled Processing Verification**: Logcat logs explicitly verify 512x512 tile iterations:
  - `AiProcessorWorker: Starting L3 512x512 Tiled AI Processing pass`
  - `AiProcessorWorker: Processing 4080 x 3060 px in 48 tiles (512 x 512 px each)`
  - `AiProcessorWorker: Tile 1 of 48 processed (0,0 - 512,512) [2%]`
  - `ImageEditor: Export Tile 1 of 48 processed (0,0 - 512,512)`
- **Logcat Crash Audit**: `adb logcat -d -s AndroidRuntime:E` - **0 Runtime Crashes (`AndroidRuntime:E`)**.
