# Lensora_Ai Comprehensive Architecture & Step Completion Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents a complete end-to-end audit of all implemented components, hardware capability probe results, L0 atomic capture journals, UI overlays, camera hardware zoom control engines, tap-to-focus metering, flash controls, mode selection state wiring, thermal/storage guards, L1 Light AI Scene Classifier, L2 AI Planner Foundation, L3/L4 WorkManager Processing Queue & Quality Validator, Pinterest-Style Masonry Gallery Tab UI, Settings Tab & AI Center Control Room, and live device/emulator verification results.

---

## 1. Architectural & Rule Compliance Setup

- **`agent.md`**: Placed in project root with mandatory AI Agent truth, prompt, judging, and workflow rules (including Rule 16 step execution constraints).
- **`master_prompt.md`**: Architectural blueprint enforcing L0-L4 layer isolation, 100% try-catch HAL crash safety, atomic MediaStore transactions, and the Obsidian Royal palette:
  - Pitch Black Background: `#0E0E0E`
  - Charcoal Surface: `#131313`
  - Royal Blue Highlight: `#2563EB`
  - Muted Gray Text/Icons: `#9CA3AF`
  - Stark White Accent: `#FFFFFF`

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 1, Step 3: L0 Safe Core - Camera2 Preview & Capture Engine

### 1. `CameraController.kt`
- **Pure Camera2 API Wrapper**: Manages `HandlerThread("CameraBackground")`, `CameraDevice`, `CameraCaptureSession`, and `ImageReader`.
- **Continuous Auto-Focus**: Configured with `CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE`.
- **JPEG Orientation Calculation**: Resolves 90-degree photo rotation using:
  $$\text{JPEG\_ORIENTATION} = (\text{sensorOrientation} - \text{deviceRotationDegrees} + 360) \pmod{360}$$
- **L0 Atomic Capture Journal (.tmp write + MediaStore transaction)**:
  1. Captures JPEG byte buffer from `ImageReader`.
  2. Writes temporary file `cacheDir/LENSORA_TIMESTAMP.jpg.tmp`.
  3. Inserts MediaStore row with `IS_PENDING = 1` under `Pictures/Lensora_Ai`.
  4. Streams bytes from `.tmp` file directly into MediaStore output stream.
  5. Updates MediaStore row to `IS_PENDING = 0` and deletes temporary `.tmp` file.
- **100% Try-Catch Safety**: Every HAL method call is wrapped in try-catch to prevent hardware or service crashes.

### 2. `CameraPreview.kt`
- `AndroidView` wrapping a `TextureView` for hardware-accelerated edge-to-edge live viewfinder rendering. Handles `isAvailable` surface texture re-initialization safely.

---

## 4. Phase 1, Step 4: Circular Gallery Shortcut & Quick View Fullscreen Viewer

### 1. `GalleryRepository.kt`
- Asynchronously queries `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` on `Dispatchers.IO` for photos in `Pictures/Lensora_Ai` ordered by `DATE_ADDED DESC`.

### 2. Single Circular Gallery Thumbnail Shortcut
- Placed in bottom-left corner of `CameraScreen.kt` (52dp circle, 2dp white border).
- Displays latest captured photo via Coil `AsyncImage`.
- Clicking navigates to `quick_view` route.

### 3. `QuickViewScreen.kt`
- Fullscreen Pitch Black (`#0E0E0E`) image viewer using Compose `HorizontalPager`.
- Features TopAppBar with Back button and live photo count badge.

---

## 5. Phase 1, Step 5: Camera Top Bar & Mode Carousel Overlays

1. **`CameraTopBar.kt`**: Transparent top overlay with Material Design icons for Flash (Auto), Timer (Off), Grid (Toggle), and Settings, respecting status bar insets.
2. **`CameraModeCarousel.kt`**: Horizontal `LazyRow` mode selector (`AI AUTO`, `PHOTO`, `PORTRAIT`, `VIDEO`, `PRO`) with selected mode highlighted in Royal Blue (`#2563EB`).

---

## 6. Phase 1, Step 6: Zoom Pills & AI Badge Overlays

1. **AI Badge (`✨ AI: Auto`)**: Frosted glass rounded pill at top center below the top bar using Charcoal surface background (`#131313`) with 75% alpha and white border. Explicitly uses Unicode string encoding (`\u2728 AI: Auto`) to prevent UTF-8 artifacting.
2. **Zoom Pills (`0.6x`, `1x`, `2x`, `3x`)**: Dark rounded container (`#131313`, 80% alpha) placed directly above the Mode Carousel. The active zoom pill is styled with a Royal Blue circular highlight.

---

## 7. Phase 1, Step 7: Camera2 Hardware Zoom, Pinch Gesture & Emulator Black Screen Resolution

1. **Dual-Strategy Hardware Zoom Engine (`applyZoom`)**:
   - `CameraController.kt` queries `CONTROL_ZOOM_RATIO_RANGE` and `SENSOR_INFO_ACTIVE_ARRAY_SIZE`.
   - On modern physical devices supporting `CONTROL_ZOOM_RATIO`, sets `CaptureRequest.CONTROL_ZOOM_RATIO` directly.
   - On emulators and legacy HALs where `CONTROL_ZOOM_RATIO_RANGE` is null, automatically falls back to `SCALER_CROP_REGION` calculation based on `activeSensorArraySize`, preventing HAL crashes or black preview frames.
   - Clamps requested ratio strictly within `[1.0f, maxZoomRatio]`.
2. **Transparent Gesture Surface Layer**:
   - Decoupled `detectTransformGestures` pinch-to-zoom gesture layer into a transparent `Box` positioned between the `CameraPreview` viewfinder and top bar / shutter controls.
   - Allows continuous smooth pinch zooming between `1.0x` and `10.0x` while ensuring `TextureView` rendering and UI overlay clicks function without interference.

---

## 8. Phase 1, Step 8: L0 Safe Core - Tap-to-Focus, Flash Control & 3x3 Grid Overlay

1. **Tap-to-Focus AF Metering & Ring Animation**:
   - `CameraScreen.kt` captures touch coordinates `(x, y)` via Compose `detectTapGestures`.
   - Displays a smooth animated focus ring (`FocusRingOverlay`) at touch coordinates that scales down and fades out cleanly.
   - `CameraController.kt` maps touch coordinates to sensor active array size, computes `MeteringRectangle` with `METERING_WEIGHT_MAX`, and triggers `CaptureRequest.CONTROL_AF_TRIGGER_START` with `CONTROL_AF_MODE_AUTO`.
2. **Flash Control Cycling (`OFF`, `ON`, `AUTO`)**:
   - `CameraTopBar.kt` features interactive Flash toggle cycling through `OFF` (grey FlashOff icon), `ON` (Royal Blue FlashOn icon), and `AUTO` (White FlashAuto icon).
   - `CameraController.kt` applies `CaptureRequest.FLASH_MODE` and `CaptureRequest.CONTROL_AE_MODE` to both repeating preview and still captures.
3. **3x3 Composition Grid Overlay**:
   - `CameraScreen.kt` renders a 3x3 rule-of-thirds Canvas grid overlay (`GridOverlay`) with semi-transparent white lines when Grid is toggled ON via `CameraTopBar.kt`.

---

## 9. Phase 1, Step 9: Camera Mode Selection State Wiring

1. **`CameraMode.kt` Enum**:
   - Package `com.lensora.ai.camera` defining `CameraMode`: `AI_AUTO` (`\u2728 AI AUTO`), `PHOTO`, `PORTRAIT`, `VIDEO`, `PRO`.
2. **`CameraController.kt` `setCameraMode(mode: CameraMode)`**:
   - Logs mode transition: `CameraController: Camera mode changed to: PORTRAIT (PORTRAIT)`.
   - Configures Camera2 `CaptureRequest.CONTROL_SCENE_MODE` (e.g. `CONTROL_SCENE_MODE_PORTRAIT` vs `CONTROL_MODE_OFF` for PRO).
3. **Mode Carousel & UI State Binding**:
   - Bound `selectedMode: CameraMode` state in `CameraScreen.kt` and `CameraModeCarousel.kt`.
   - Tapping any mode smoothly shifts the Royal Blue (`#2563EB`) highlight text color and invokes `cameraController.setCameraMode(mode)`.

---

## 10. Phase 1, Step 10: L0 Safe Core Guards (Thermal & Storage)

1. **Storage Guard (`checkAvailableStorageBytes`)**:
   - `CameraController.kt` checks available disk space via `StatFs(cacheDir)`.
   - If available space is less than **2 GB** (2 * 1024 * 1024 * 1024 bytes), still capture in `takePicture()` is automatically blocked and error callback triggers a Toast warning: `"Storage Low: ... Minimum 2GB space required."`
2. **Thermal Guard (`PowerManager.OnThermalStatusChangedListener`)**:
   - Attaches `PowerManager.OnThermalStatusChangedListener` in `openCamera()`.
   - Monitors device thermal throttling state (`NONE`, `LIGHT`, `MODERATE`, `SEVERE`, `CRITICAL`, `EMERGENCY`, `SHUTDOWN`).
   - Logs warnings when thermal state hits `SEVERE` or `CRITICAL`.
   - Automatically unregisters listener on `closeCamera()`.

---

## 11. Phase 2, Step 11: L1 Light AI Foundation & Scene Classifier

1. **TensorFlow Lite Integration**:
   - Configured `org.tensorflow:tensorflow-lite:2.16.1` dependency in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
2. **`AiModule` Architecture (`AiModule.kt` & `ModelManager.kt`)**:
   - Defined core `AiModule` interface with suspend `initialize()`, `analyze(bitmap)`, and `close()` contracts.
   - Implemented `ModelManager` asset loading for TFLite models with 100% try-catch crash protection.
3. **`SceneClassifier` Engine (`SceneClassifier.kt`)**:
   - Performs real-time frame classification on 224x224 downscaled bitmap inputs.
   - Runs on `Dispatchers.Default` background worker thread, ensuring the UI main thread is never blocked.
   - Classifies camera scene semantics into `✨ AI: Portrait`, `✨ AI: Landscape`, `✨ AI: Night`, `✨ AI: Sky`, and `✨ AI: Auto`.

---

## 12. Phase 2, Step 12: L2 AI Planner Foundation

1. **`AiPlanner.kt` Architecture**:
   - Package `com.lensora.ai.ai` defining `SceneType` (`PORTRAIT`, `LANDSCAPE`, `NIGHT`, `SKY`, `AUTO`) and `ProcessingPlan` data class (`sceneType`, `targetEvOffset`, `targetAfMode`).
2. **Dynamic Hardware Execution Plan Mapping**:
   - **`PORTRAIT`**: EV Offset `+0.3`, AF Mode `CONTINUOUS_PICTURE`.
   - **`LANDSCAPE`**: EV Offset `-0.2`, AF Mode `AUTO`.
   - **`NIGHT`**: EV Offset `+1.0`, AF Mode `AUTO`.
   - **`SKY`**: EV Offset `-0.1`, AF Mode `AUTO`.
   - **`AUTO`**: EV Offset `0.0`, AF Mode `CONTINUOUS_PICTURE`.
3. **`CameraController.kt` `applyProcessingPlan(plan: ProcessingPlan)`**:
   - Calculates `CONTROL_AE_EXPOSURE_COMPENSATION` step bounds based on `CONTROL_AE_COMPENSATION_STEP` and `CONTROL_AE_COMPENSATION_RANGE`.
   - Safely updates Camera2 preview repeating request stream without interrupting viewfinder rendering.

---

## 13. Phase 2, Step 13: L3/L4 Image Processing Pipeline Skeleton

1. **WorkManager Integration (`androidx.work:work-runtime-ktx:2.10.0`)**:
   - Integrated WorkManager dependency for background task scheduling.
2. **`Validator.kt` (L4 Quality Verification)**:
   - Verifies bitmap integrity and checks for average luminance bounds (`avgLum > 2`) to ensure processed outputs are not corrupted or pitch black before MediaStore finalization.
3. **`AiProcessorWorker.kt` (L3 AI Computational Engine)**:
   - Background `CoroutineWorker` running on `Dispatchers.IO`.
   - Reads input URI, scene metadata, and EV offsets.
   - Executes computational AI processing pass with 2-second non-blocking background queue execution.
   - Generates final processed MediaStore image: `LENSORA_AI_PROCESSED_<timestamp>.jpg`.
   - L0 Fallback Guarantee: If processing fails or validation fails, the original raw JPEG capture is safely preserved.

---

## 14. Phase 2, Step 14: Pinterest-Style Gallery Tab UI

1. **`GalleryRepository.kt` `getAllPhotos()`**:
   - Asynchronously queries MediaStore on `Dispatchers.IO` for all images stored in `Pictures/Lensora_Ai`.
2. **`GalleryScreen.kt` Masonry Layout**:
   - Built with Compose `LazyVerticalStaggeredGrid` (2 columns: `StaggeredGridCells.Fixed(2)`).
   - Asynchronously loads images via Coil `AsyncImage` with natural staggered item height cards (`180.dp` to `310.dp`).
   - Displays a `CircularProgressIndicator` while images are loading.
   - Styled using Obsidian Royal theme palette (`PitchBlackBackground` `#0E0E0E`, `CharcoalSurface` `#131313`).
3. **Navigation Integration**:
   - Wired `GalleryScreen` into `Navigation.kt` shell with photo click navigation to `QuickViewScreen`.

---

## 15. Phase 2, Step 15: Settings Tab UI & AI Center Control Room

1. **`SettingsScreen.kt` Redesign**:
   - Built as a full control room list using Material 3 `Scaffold`, `LazyColumn`, `ListItem`, `Switch`, and `Card`.
   - Styled in Obsidian Royal theme palette (`PitchBlackBackground` `#0E0E0E`, `CharcoalSurface` `#131313`, `RoyalBlue` `#2563EB`, `ColorWhite` `#FFFFFF`, `ColorMutedGray` `#9CA3AF`).
2. **Control Room Sections**:
   - **AI Control Center**: Displays AI Center status, active TFLite / MobileNet inference engine metrics.
   - **Services & Engines**: Interactive `Cloud Sync` toggle switch (Google Drive) and `Manage AI Engines` row.
   - **Storage & Maintenance**: Interactive `Clear Cache & Temporary Files` cleaner deleting temporary `.tmp` capture journals.
   - **Hardware Diagnostics**: Card launcher for `Hardware Probe Lab` navigation.
   - **About**: Version information (`v1.0.0 Android 16 API 36`).

---

## 16. Verification & Crash Audit Results

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **Static Code Analysis**: Analyzed all 27 Kotlin source files in `app/src/main/java` - **0 Errors, 0 Warnings**.
- **Live Device & Emulator Audit**: Physical **Xiaomi Redmi Note 13 Pro 5G** and **Android Studio Emulator (`emulator-5554`)**.
- **Logcat Crash Audit**: Verified **0 Runtime Crashes (`AndroidRuntime:E`)**.
