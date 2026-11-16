# Lensora_Ai Comprehensive Architecture & Phase 25 Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents the completion of **Phase 25: Real Image-to-Image AI Pipeline (Dehaze + LUT Integration)**. The foreground processing service (`AiProcessorService.kt`) has transitioned from MobileNet classification to a full **Real Image-to-Image AI Enhancement Engine**. The system inspects internal DLC model storage (`filesDir/models/`) for Dehaze or Real-ESRGAN image enhancement models (`real_esrgan_v2.tflite`, `dehaze.tflite`). For each $512 \times 512$ tile, the service streams pixels into input float buffers, executes TFLite inference via GPU/CPU, reconstructs the enhanced tile, applies active **VibePreset** LUT color grading matrices (e.g. Cinematic, Leica, Royal Gold), and validates luminance with the L4 Anti-AI Validator (`Validator.kt`). Safe fallbacks guarantee seamless operations if models are missing or unreadable.

---

## 1. Architectural Transition: Classification $\rightarrow$ Image-to-Image AI Enhancement

- **Image-to-Image Model Selection**: `ModelManager.getEnhancementModelFile()` prioritizes `real_esrgan_v2.tflite`, `dehaze.tflite`, and `image_enhancer.tflite`, filtering out classification models.
- **Tiled Tensor Stream Engine**: For each $512 \times 512$ tile:
  1. Decodes tile region via `BitmapRegionDecoder` (RAM Memory Shield).
  2. Maps $512 \times 512 \times 3$ RGB pixels into float `ByteBuffer` ($1 \times H \times W \times 3$).
  3. Executes `interpreter.run(inputBuffer, outputBuffer)` on GPU/CPU.
  4. Converts output float buffer back into $512 \times 512$ ARGB_8888 Bitmap.
- **Stacked Vibe LUT Color Grading**: Applies active `VibePreset` ColorMatrix (Cinematic, Leica, Vintage, Royal Gold) onto output tiles using software `Canvas` + `ColorMatrixColorFilter`.
- **L4 Anti-AI Validation & Fail-Safe Fallback**: Passes tiles through `Validator.isTileValid(tile)`. If a model is missing or a tile fails validation, draws original unenhanced tile pixel data without failing the capture flow.

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 25 Image-to-Image AI & LUT Audit Matrix

| Component | Target Action | Execution Engine | Safeguard / Fallback | Result |
| :--- | :--- | :--- | :--- | :---: |
| `ModelManager.kt` | Image-to-Image Model Load | `getEnhancementModelFile()` | Safe null return if missing | `PASS` |
| `AiProcessorService.kt` | $512 \times 512$ Tiled AI Inference | `Interpreter.run(input, output)` | Fallback to ColorMatrix pipeline | `PASS` |
| `AiProcessorService.kt` | Vibe Preset LUT Pass | `ColorMatrixColorFilter` | `VibePreset.NONE` standard matrix | `PASS` |
| `Validator.kt` | L4 Tile Validation | `isTileValid(enhancedTile)` | Draws raw original tile on fail | `PASS` |
| `AiProcessorService.kt` | GPU Memory Cleanup | `interpreter.close()` & `gpuDelegate.close()` | Executed in service `finally` block | `PASS` |

---

## 4. Verification & Logcat Crash Audit

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **ADB Permission Grants**: Granted `CAMERA`, `RECORD_AUDIO`, and `POST_NOTIFICATIONS` permissions programmatically.
- **Logcat Audit Logs**:
  - `AiProcessorService: Executing L3 Real Image-to-Image AI Enhancement & LUT Pipeline in Foreground Service`
  - `AiProcessorService: Applied Vibe Preset ColorMatrix pass: Cinematic`
  - `AiProcessorService: Processing 4080 x 3060 px in 48 tiles (512 x 512 px each)`
  - `L4Validator: L4 Validation PASS: Image luminance check valid`
  - `AiProcessorService: TFLite Interpreter & GPU resources safely closed`
- **Logcat Crash Audit**: `adb logcat -d -s AndroidRuntime:E` - **0 Runtime Crashes (`AndroidRuntime:E`)**.
