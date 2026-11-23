# 📸 Lensora_Ai — The Native Computational Camera

> **AI-First • 100% Offline • Zero CameraX • Pure Camera2 L0-L4 Architecture**

---

## 📌 Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an enterprise-grade, native Android computational camera application built on **Android 16 (API 36)** using **Jetpack Compose**, **Material 3 (Obsidian Royal Theme)**, **Hilt**, and a custom **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

It delivers high-end computational photography entirely **100% offline** on device hardware—featuring real-time AI preview scene classification, LaMa GPU inpainting object erasing, $512 \times 512$ tiled neural image enhancement, 24fps cinematic recording with letterboxing, 3D LUT color science grading, real-time system diagnostics, and Google Drive Cloud Vault backup with SHA-256 local master auto-purge.

---

## 🎯 Target Hardware & Physical Constraints

The application is tuned and hardened specifically for the **Redmi Note 13 Pro 5G** (and modern Snapdragon 7/8 Series devices):

| Parameter | Device Specification | Architectural Impact & Strategy |
| :--- | :--- | :--- |
| **SoC** | Qualcomm Snapdragon 7s Gen 2 | Multi-threaded TFLite execution on CPU (4 threads with XNNPACK) for Real-ESRGAN super-resolution, and `GpuDelegate` acceleration for LaMa inpainting. |
| **RAM** | 8 GB LPDDR4X | **Memory Shield Engine**: $512 \times 512$ px tiled region processing (`BitmapRegionDecoder`) preventing Out-Of-Memory (OOM) crashes on 12.5MP images. |
| **OS** | Android 16 / Xiaomi HyperOS 3.0 | **HyperOS Kill Protection**: `AiProcessorService` runs as a persistent Foreground Service (`FOREGROUND_SERVICE_DATA_SYNC`). |
| **Camera Sensor** | 200 MP ISOCELL HP3 (Main ID 0) | **Probe Audit Finding**: 200MP raw stream map is unexposed to 3rd-party apps via standard Camera2 characteristics. Binned to **12.5 MP (4080x3060)** + Real-ESRGAN AI upscale. |
| **Preview Buffer** | 1080p `SurfaceTexture` | Preview default buffer set to standard 1080p (`1920x1080`) to eliminate aspect ratio stretching while preserving 12.5MP capture on `ImageReader`. |

---

## 🏗️ L0–L4 Computational Camera Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│ L0 SAFE CORE HAL (Camera2, 100% Try-Catch Safety, Storage/Thermal Guards) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Preview Frames (224x224, 2 FPS / 500 ms)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ L1 LIGHT AI DIRECTOR (Places365 ResNet-18 Scene Classification)         │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Scene Labels & Subject Bounding Box
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ L2 AI PLANNER & TUNING (Rule-Based Hardware Orchestration)              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Still JPEG Bytes
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ L3 HEAVY AI PIPELINE (512x512 Tiled Memory Shield, LaMa GPU, Service)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Enhanced Tile Bitmaps
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ L4 ANTI-AI VALIDATOR (Luminance Shift Bounds Check: 2 < avgLum < 253) │
└────────────────────────────────────────────────────────────────────────┘
```

1. **L0 Safe Core**:
   - Pure Camera2 API implementation (`CameraDevice`, `CameraCaptureSession`).
   - Atomic `.tmp` capture journal (`cacheDir/LENSORA_TIMESTAMP.jpg.tmp` $\rightarrow$ MediaStore `Pictures/Lensora_Ai` $\rightarrow$ `IS_PENDING = 0`).
   - Thermal Guard (`PowerManager.OnThermalStatusChangedListener`) and 2GB Storage Guard (`StatFs`).
2. **L1 Light AI Director**:
   - 2 FPS / 500 ms preview frame sampling loop (`delay(500)`) on `Dispatchers.Default`.
   - `SceneClassifier.kt` initializes `ModelManager` loading `scene_classifier.tflite` (Places365 ResNet-18 FP16) executing NCHW Float32 tensor inference for Places365 365-class scene detection ("✨ AI: Landscape", "✨ AI: Sky", "✨ AI: Auto"), with brightness heuristic fallback for "✨ AI: Night" and subject bounding box heuristic for "✨ AI: Portrait".
   - `CoachingHintHelper.coachingHintFor(box)` computes real normalized subject bounding boxes and returns composition coaching prompts (*"✨ Step Closer"*, *"✨ Move Right"*, *"✨ Perfect Composition"*) only when a valid subject box is calculated (hiding the UI pill completely when `box == null`).
3. **L2 AI Planner**:
   - Rule-based orchestrator converting classified scenes into processing plans.
4. **L3 Heavy AI Pipeline**:
   - **Memory Shield Engine**: Processes high-res images in $512 \times 512$ tile regions using `BitmapRegionDecoder`, executing TFLite inference and immediately calling `tileBitmap.recycle()`.
   - **Foreground Service**: `AiProcessorService.kt` runs heavy processing passes under HyperOS background task protection with persistent notification (`✨ AI: Perfecting your photo...`).
5. **L4 Anti-AI Validator**:
   - Inspects generated tile luminance bounds ($2 < \text{avgLum} < 253$). Rejects corrupt or pitch-black/white tile artifacts, falling back to raw pixel data.

---

## 📱 The 5 Core Tabs (Obsidian Royal UI)

The UI is built with the **Obsidian Royal** color palette:
- `Background`: Pitch Black (`#0E0E0E`)
- `Surface`: Dark Charcoal (`#131313`)
- `Primary Accent`: Royal Blue (`#2563EB`)
- `Highlight`: Cyan (`#00E5FF`)
- `Typography`: Clean Inter sans-serif across all Material 3 styles

| Tab | Key Functionality & Highlights |
| :--- | :--- |
| 📷 **Camera** | Pure edge-to-edge TextureView viewfinder, translucent glass status pills, Multi-Ring Tactile Shutter Button ($82\text{dp}$ outer ring, $70\text{dp}$ core, spring scale `0.88f`), Zoom Pills (`0.6x`, `1x`, `2x`, `3x`), Pro HUD (Zebras, Focus Peaking, ISO, Shutter), 2.39:1 Cinematic Letterboxing. |
| 🎨 **Studio** | Real-time Exposure (-2.0 to +2.0 EV) & Contrast (-50 to +50) GPU processing, **LaMa GPU Object Eraser** with interactive brush mask Canvas, VN-Style Vintage Timestamp overlay, 512x512 tiled high-res export. |
| 🖼️ **Gallery** | Pinterest-style Masonry Grid layout displaying local photos, single-tap Quick View swipable viewer with haptic feedback. |
| 🎞️ **Vibes** | 3D LUT Color Science presets (Cinematic Teal & Orange, Leica B&W, Royal Gold, Vintage Film), AGSL film grain shaders, VN aesthetic template cards. |
| ⚙️ **Settings** | **System Diagnostics Dashboard** (2s live polling loop for RAM, Battery Temp, Thermal state, AI engines), GitHub DLC Model Downloader with progress tracking, Google Drive OAuth2 Cloud Vault settings. |

---

## 🎥 The 27 Camera Modes

| Category | Camera Mode | Status | Behavior & Tuning Specification |
| :--- | :--- | :---: | :--- |
| **Core Modes** | **✨ AI AUTO** | `WORKING (Step 62c)` | Continuous auto-focus, live AI scene detection (2 FPS / 500 ms loop). |
| | **📷 PHOTO** | `WORKING (Step 37)` | Standard 12.5MP still capture with atomic MediaStore `.tmp` journal. |
| | **👤 PORTRAIT** | `WORKING (Step 37)` | Universal skin tone illumination EV `+0.3` boost + continuous auto-focus. |
| | **📹 VIDEO** | `WORKING (Step 60)` | `TEMPLATE_RECORD` session with MediaRecorder surface binding. |
| | **🎬 CINEMATIC** | `WORKING (Step 37)` | 2.39:1 pure black letterboxing + validated safe FPS target range (`Range(24, 24)` / `Range(15, 30)`). |
| | **🎛️ PRO** | `WORKING (Step 37)` | Manual exposure controls (ISO 100–3200, Shutter 1/1000s–1s), RAW DNG toggle, Zebras, Focus Peaking. |
| **Pro Tools** | **✨ ASTRO** | `IMPLEMENTED - NOT VERIFIED` | Infinity focus lock (`0.0f`) + 30s long exposure. |
| | **🔍 PRO MACRO** | `IMPLEMENTED - NOT VERIFIED` | Minimum focus distance lock (`LENS_INFO_MINIMUM_FOCUS_DISTANCE`). |
| | **📷 HIGH-RES** | `IMPLEMENTED - VERIFIED` | 12.5MP capture + Real-ESRGAN 2x net AI super-resolution pass (4x model tile scale, GPU delegate with CPU 4-thread fallback, 8px tile overlap with 112x112 core tiles, verified on emulator-5554 with peak RAM 445 MB PSS). |
| | **🌙 NIGHT VISION** | `IMPLEMENTED - NOT VERIFIED` | Exposure compensation +1.0 EV boost + multi-frame noise reduction (needs dehaze.tflite, not in DLC manifest; see docs/MODEL_AUDIT.md). |
| | **📄 SCAN** | `IMPLEMENTED - NOT VERIFIED` | Document edge detection & multi-page PDF generation with ZXing `ErrorCorrectionLevel.H` QR codes. |
| | **📑 DOCUMENT** | `IMPLEMENTED - NOT VERIFIED` | High contrast black-and-white text extraction. |
| **Brand Aesthetics** | **📷 LEICA** | `VIBE-ONLY` | Leica Monochrome B&W 3D LUT color grading. |
| | **🔵 ZEISS** | `VIBE-ONLY` | ZEISS T* anti-reflex high-contrast color tuning. |
| | **🟠 HASSELBLAD** | `VIBE-ONLY` | Hasselblad Natural Color Solution (HNCS) golden palette. |
| | **✨ XMAGE** | `VIBE-ONLY` | XMAGE Vivid saturated color science. |
| | **🎬 HARCOURT** | `VIBE-ONLY` | Studio Harcourt Paris dramatic portrait black & white. |
| **Utility Modes** | **🌐 PANORAMA** | `SOON` | 360 sweep capture pipeline. |
| | **🍔 FOOD** | `SOON` | Warm color temperature boost & macro center focus. |
| | **⏱️ SLOW-MOTION** | `SOON` | High frame rate recording pass. |
| | **⏳ TIME-LAPSE** | `SOON` | Interval frame capture pass. |
| | **📱 DUAL-VIDEO** | `SOON` | Simultaneous front and rear camera recording session. |
| | **✨ BEAUTY** | `SOON` | Real-time skin smoothing ColorMatrix pass. |
| | **🌌 DEEP SKY** | `SOON` | Multi-frame astrophotography stacking pass. |
| | **🔬 ULTRA MACRO** | `SOON` | Digital crop + sharpening macro pass. |
| | **🏷️ WATERMARK** | `SOON` | VN-style vintage timestamp watermark overlay. |
| | **🎞️ RAW DNG** | `SOON` | `DngCreator` uncompressed RAW sensor capture (`Pictures/Lensora_Ai/RAW`). |

---

## 🛠️ Automated Review Workflow

Before submitting review packages or pull requests, run the review preparation script to package all git metadata, test results, build logs, and validated screenshots into `Claude_review.zip`:

```powershell
.\scripts\prepare_claude_review.ps1 -Step "Step 63" -Base dev
```

---

## 🤖 Neural AI Models (100% Offline Inference)

LaMa Dilated object inpainting executes via `GpuDelegate()`. Real-ESRGAN v2 super-resolution engine executes via multi-threaded CPU (`setNumThreads(4)` with XNNPACK) yielding 2x net resolution scale (4x model tile scale):

| AI Model | File Name / Location | Neural Network Architecture & Function |
| :--- | :--- | :--- |
| **Scene Classifier** | `scene_classifier.tflite` | Places365 ResNet-18 FP16 224x224 NCHW preview classifier ($< 50\text{ms}$ latency). |
| **Object Eraser** | `lama_dilated.tflite` | Two-input float tensors ($1 \times 512 \times 512 \times 3$ RGB + $1 \times 512 \times 512 \times 1$ Mask) LaMa-Dilated neural inpainting engine ($512 \times 512$ tiles). |
| **AI Super-Res** | `real_esrgan_v2.tflite` | Real-ESRGAN v2 2x net super-resolution model (4x tile scale: 128x128 -> 512x512 tiles, 50% pre-downscale pass targeting ~50MP output). |
| **AI Dehaze** | `dehaze.tflite` | Atmospheric haze removal neural pass ($512 \times 512$ tiles). |

---

## ☁️ Real Google Drive Cloud Vault

- **OAuth2 Authentication**: Secure token management storing access/refresh tokens in `EncryptedSharedPreferences` backed by `MasterKeys.AES256_GCM_SPEC`.
- **Resumable REST API v3**: Uploads processed photos via Google Drive REST API v3 (`uploadType=resumable`).
- **Wi-Fi Only WorkManager**: `CloudSyncWorker` runs under `NetworkType.UNMETERED` constraints.
- **SHA-256 Auto-Purge**: Computes streaming SHA-256 checksum during byte transfer. Verifies checksum against cloud response before deleting local master file from MediaStore (`ContentResolver.delete()`).

---

## 🛡️ System Guards & HyperOS Protection

- **Storage Guard**: Checks available disk space via `StatFs`. Blocks camera capture if available space is $< 2\text{GB}$.
- **Thermal Guard**: `PowerManager.OnThermalStatusChangedListener` monitors thermal throttling states (`THERMAL_STATUS_SEVERE`).
- **HyperOS Protection**: `AiProcessorService` runs as a Foreground Service with persistent notification (`"✨ AI: Perfecting your photo..."`) resisting Xiaomi HyperOS task kills.

---

## ⚠️ Known Limitations

- **Android Emulator Camera2 HAL**: Preview frame buffers on Android Emulator instances render virtual test scenes / color bars. Physical hardware camera validation requires physical Android device execution.
- **Redmi Note 13 Pro 5G Sensor Binning**: 200MP raw streams are binned to 12.5MP (`4080x3060`) output maps by standard Camera2 HAL characteristics. High-res capture uses 12.5MP capture + Real-ESRGAN v2 4x tile super-resolution pass (50% pre-downscale pass targeting ~50MP output).
- **TFLite Neural Inference**: `SceneClassifier.kt` executes NCHW Float32 tensor array inference on `places_fp16.tflite` (`scene_classifier.tflite`) for Places365 365-class scene categorizations, falling back to color-brightness heuristics when uninstalled.
- **Video Editor Non-Destructive Export**: Non-destructive timeline project imports use Media3 1.11.1 stream ingestion; multi-track composition rendering is currently under active integration.

---

## 🛠️ Building & Installation

### Prerequisites
- **Android Studio**: NOT VERIFIED (Environment patch variable unexposed)
- **JDK**: Java 17
- **Android SDK**: Compile SDK 36, Min SDK 33, Target SDK 35
- **Unit Test Suite**: 21 passed unit tests (`21/21 pass`)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/KanakSaini04/Lensora_Ai.git
cd Lensora_Ai

# Run unit tests (96/96 pass across 20 test classes)
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Install on connected device/emulator via ADB
./gradlew installDebug
```

---

## 📄 License & Credits
Developed by **Kanak Saini**. Built with Jetpack Compose, Camera2 API, TensorFlow Lite, and Hilt.
