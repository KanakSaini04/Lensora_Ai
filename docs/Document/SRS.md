# Software Requirements Specification (SRS) - Lensora_Ai

**Application Name**: Lensora_Ai  
**Package Name**: `com.lensora.ai`  
**Target Platform**: Android 16 (API Level 36) / Minimum SDK 33 (Android 13)  
**Architecture Pattern**: Layered L0-L4 Safe Core Architectural Model  
**UI Framework**: Jetpack Compose with Material 3 Obsidian Royal Theme  

---

## 1. Introduction

### 1.1 Purpose
This Software Requirements Specification (SRS) defines the functional and non-functional requirements for **Lensora_Ai**, an AI-first computational camera application built natively for modern Android devices. Lensora_Ai operates on a zero-CameraX policy, utilizing pure Camera2 API for precision hardware control alongside a 5-tier (L0-L4) computational engine.

### 1.2 Scope
Lensora_Ai delivers real-time AI scene classification, hardware capability probing, 27 specialized camera modes, uncompressed RAW (DNG) capture, 24fps Cinematic video recording, real-time 3D LUT AGSL color grading, Studio non-destructive photo editing, AI Object Eraser (LaMa inpainting), Pinterest-style staggered gallery, Google Drive cloud backup, and manifest-driven TFLite AI model downloading.

---

## 2. Architectural Layers & Safety Mandates

### 2.1 Layer Isolation Model (L0 - L4)
- **L0 Safe HAL Engine**: Raw Camera2 API wrapper managing hardware capture requests, background threads, and atomic `.tmp` MediaStore file transactions. Guaranteed 100% crash safety via try-catch wrapping on all HAL invocations.
- **L1 Light AI Foundation**: Real-time 2 FPS preview frame analysis using TensorFlow Lite (`SceneClassifier.kt`) running on `Dispatchers.Default`.
- **L2 AI Planner Engine**: Dynamic execution planner (`AiPlanner.kt`) converting AI scene labels into exposure compensation offsets (`targetEvOffset`) and auto-focus modes (`targetAfMode`).
- **L3 AI Computational Engine**: Background WorkManager CoroutineWorker queue (`AiProcessorWorker.kt`) executing TFLite inference (MobileNet / Real-ESRGAN) or stacked ColorMatrix color grading.
- **L4 Quality Verification**: Pre-finalization validation engine (`Validator.kt`) verifying bitmap integrity and average luminance bounds (`avgLum > 2`).

### 2.2 System Safety & Thermal Guards
- **Storage Guard**: Evaluates `StatFs` available space prior to capture. Automatically blocks photo/video capture if available internal storage is under **2 GB**, preventing disk full corruption.
- **Thermal Guard**: Monitors `PowerManager.OnThermalStatusChangedListener` events. Throttles computational worker threads during `SEVERE` or `CRITICAL` thermal states.
- **Direct Boot Compatibility**: `MainActivity` operates with `android:directBootAware="true"` and safe `createDeviceProtectedStorageContext()` fallback for credential-encrypted `SharedPreferences`.

---

## 3. Functional Requirements

### 3.1 Camera Capture & Preview Engine
- **FR-01 (Camera2 Preview)**: Edge-to-edge hardware-accelerated preview rendering via Compose `TextureView`.
- **FR-02 (Hardware Zoom)**: Pinch-to-zoom gesture (1.0x to 10.0x) backed by Camera2 `CONTROL_ZOOM_RATIO` with automatic fallback to `SCALER_CROP_REGION` for legacy devices and emulators.
- **FR-03 (Tap-to-Focus)**: Touch coordinate mapping to sensor active array bounds triggering `CONTROL_AF_TRIGGER_START` metering with animated focus ring.
- **FR-04 (Flash Controls)**: Interactive Flash toggle cycling `OFF`, `ON`, and `AUTO` applying `FLASH_MODE` and `CONTROL_AE_MODE`.
- **FR-05 (Grid & Timers)**: 3x3 Canvas rule-of-thirds grid overlay and shutter countdown timer (`OFF`, `3s`, `10s`).
- **FR-06 (Camera Flip)**: Switch between `LENS_FACING_BACK` and `LENS_FACING_FRONT` with horizontal preview mirroring.

### 3.2 Computational Modes (23 Active Modes + 4 Feature Controls, 10 Planned)
- **Active Core Modes (6)**: `AI AUTO`, `PHOTO`, `PORTRAIT`, `VIDEO`, `PRO`, `CINEMATIC`.
- **Active Pro Tools (2 Modes)**: `Astro` (infinity focus lock & 30s manual exposure), `Pro Macro` (minimum focus distance).
- **Active Signature Brands (5 Modes)**: `Leica` (B&W grading), `ZEISS` (T* anti-reflex), `Hasselblad` (natural color solution), `XMAGE` (vivid color science), `Harcourt` (portrait black & white).
- **Active Feature Controls (4 Controls)**: `RAW DNG Capture`, `Flash Controls` (OFF/ON/AUTO), `Hardware Zoom` (0.6x to 3x+), `Tap-to-Focus`.
- **Planned Modes (10 "SOON" Modes)**: `Panorama`, `Tele-Cine`, `Time-Lapse`, `3D Spatial`, `360 Sphere`, `Stop Motion`, `Sony G`, `Samsung`, `Apple`, `Pixel` (displayed in MoreModesSheet with "SOON" badge, pending hardware sensors/future updates).

### 3.3 Pro Mode HUD & Overlays
- **FR-07 (Manual Exposure Sliders)**: Manual ISO slider (100 to 3200) and Shutter Speed slider (1/1000s to 1/30s) applying `SENSOR_SENSITIVITY` and `SENSOR_EXPOSURE_TIME`.
- **FR-08 (RAW DNG Capture)**: Serializes uncompressed RAW `.dng` files using `DngCreator` to `Pictures/Lensora_Ai/RAW/` with safe fallback to JPEG.
- **FR-09 (AGSL GPU Overlays)**: Zebras highlight (>0.85 luminance red diagonal stripes) and Focus Peaking (neon green edge detection).

### 3.4 Video Recording Engine
- **FR-10 (Video Recorder)**: MP4 format, H264 video encoder (10 Mbps), AAC audio encoder saving to `Movies/Lensora_Ai`.
- **FR-11 (Cinematic 24fps)**: Locked 24 FPS target AE range (`[24, 24]`) with top 12% and bottom 12% pure black 2.39:1 letterboxing bars overlay.

### 3.5 Gallery, Studio & Vibes Tabs
- **FR-12 (Pinterest Gallery)**: Staggered masonry grid (`LazyVerticalStaggeredGrid`) displaying MediaStore captured photos.
- **FR-13 (Studio Editing)**: Real-time 60fps Compose `ColorMatrix` preview driven by Exposure (-2.0 to +2.0 EV) and Contrast (-50 to +50) sliders with stacked Vibe LUT color science.
- **FR-14 (AI Object Eraser)**: Touch brush mask Canvas overlay with LaMa AI inpainting algorithm pass.
- **FR-15 (Studio Export)**: Software `Canvas` rendering pass baking stacked Vibe LUTs and optional glowing Cyan (`#00E5FF`) timestamp overlay onto full-res Bitmaps.
- **FR-16 (Vibes Tab)**: Segmented tab views (`LUTs`, `Presets`, `Templates`) displaying 3D CUBE badges, tone curves, and story templates.

### 3.6 Settings, Cloud Sync & DLC Model Downloader
- **FR-17 (Control Room Settings)**: Persists user preferences (`grid_enabled`, `flash_mode`, `camera_mode`, `raw_dng_enabled`, `cloud_sync_enabled`, `auto_purge_enabled`).
- **FR-18 (Cloud Sync & Auto-Purge)**: Background WorkManager CoroutineWorker streaming photo to Google Drive with SHA-256 verification and automatic MediaStore master deletion when Auto-Purge is enabled.
- **FR-19 (GitHub DLC Downloader)**: Manifest-driven streaming ZIP downloader extracting `.tflite` model files to internal storage with SHA-256 validation.
- **FR-20 (Hardware Probe Lab)**: Non-disruptive hardware capability diagnostic lab testing P1 (200MP binned to 12.5MP), P2 (RAW_SENSOR), and P3 (24fps Cinema).

---

## 4. Non-Functional Requirements

### 4.1 Performance & Memory Hygiene
- **UI Frame Rate**: Constant 60 FPS Compose rendering on main thread; background classification capped at 2 FPS on `Dispatchers.Default`.
- **Memory Management**: Explicit `Bitmap.recycle()` calls on all intermediate bitmap instances; zero OOM memory leaks.
- **Storage Footprint**: APK size under 25 MB with on-demand DLC model downloads.

### 4.2 UI/UX Palette Constraints
- Pitch Black Background: `#0E0E0E`
- Charcoal Surface: `#131313`
- Royal Blue Highlight: `#2563EB`
- Accent Cyan: `#00E5FF`
- Muted Gray Text: `#9CA3AF`
- White Accent: `#FFFFFF`
