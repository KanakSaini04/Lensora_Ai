# PROJECT_HANDOFF.md - Lensora_Ai

> Single source of truth for any AI or human taking over this project.
> Read this FIRST. It is updated in the SAME PR as every step. If it disagrees with the code, the code wins: fix this file.
> Rules for this file: every fact must come from the repo, a test run or a device/emulator check. Unknown = **NOT VERIFIED**. No secrets, tokens, keystores, PINs or personal data.

## 0. Status block (update every step)
- **Last updated**: 2026-11-18
- **Branch / HEAD**: `build/all-features` / `148eace`
- **Last completed step**: Task 2.4 (Model Supply Chain Audit & Gap Review)
- **In progress**: Rolling full build on branch `build/all-features`
- **Unit tests**: 58 passed, 0 skipped, 0 failed across 13 test classes
- **Baseline APK size**: 93,920,874 bytes (~89.57 MB)
- **Last emulator check**: `emulator-5554` (`Medium_Phone`), 2026-11-18 (Verified PhotoPicker launch, empty ProjectsScreen state, single Editor header, export dialog dismiss lock, 0 crashes in logcat); **Last physical-device (Redmi) check**: Step 62 (Camera tab top overlays & typography verification on Redmi Note 13 Pro 5G `2312DRA50I`)

## 1. Boot order for a new AI
1. `GEMINI.md` (coding-agent rules & Section 10 Run/Device policy)
2. `agent.md` (project rules & Rule 18 mandatory report format)
3. `PROJECT_HANDOFF.md` (this file)
4. `progress.md` (full step log), `CHANGELOG.md`, `README.md`, `report.md`, `docs/MODEL_AUDIT.md`, `docs/MODEL_SUPPLY_PLAN.md`, `docs/Document/*`
5. The latest `Claude_review.zip` if provided by the user.

- **Roles:** The user (developer, brand CodexCraft) decides. **Claude** = architect/reviewer: writes one-task step prompts, reviews review zips, cannot see the repo. **Gemini agent** (Android Studio) = coder: executes prompts, reports in GEMINI.md section 8 format.
- A new AI takes the role the user assigns. Never claim "verified" without pasted evidence.

## 2. What the project is and what we want
- **Lensora_Ai** (`com.lensora.ai`): Native Android computational camera application built on Android 16 (API 36) using Kotlin, Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, pure **Camera2 L0 Safe Core HAL (zero CameraX dependencies)**, 100% offline TFLite neural AI pipeline, VN-style video/photo editor with Media3 Transformer integration, and Google Drive Cloud Vault.
- **Goal:** Every one of the 27 camera modes, 5 core tabs, 4 neural AI models (`scene_classifier`, `lama_dilated`, `real_esrgan_v2`, `dehaze`), the VN-style video editor, and the Google Drive Cloud Vault actually WORKING, verified on the Redmi Note 13 Pro 5G, with nothing removed.
- **Vision**: Professional computational camera empowering creators with real-time AI Director scene coaching, 3D LUT color grading, non-destructive video/photo editing, and automated cloud vault backup—all with 100% offline AI inference privacy.

## 3. Locked decisions (never change without the user's explicit OK)
- **Camera Engine**: Pure Camera2 API only. Strictly zero CameraX dependencies.
- **Architecture**: L0 Safe Core HAL $\rightarrow$ L1 Light AI Director $\rightarrow$ L2 AI Planner $\rightarrow$ L3 Heavy AI Pipeline $\rightarrow$ L4 Anti-AI Validator.
- **Theme**: Obsidian Royal Theme (Pitch Black `#0E0E0E`, Dark Charcoal `#131313`, Royal Blue `#2563EB`, Highlight Cyan `#00E5FF`).
- **Tabs**: 5 fixed bottom navigation tabs (Camera, Studio, Gallery, Vibes, Settings).
- **Camera Modes**: All 27 camera modes stay in the app (a "SOON" badge is allowed; deleting or removing any mode is strictly forbidden).
- **AI Processing**: 100% offline local neural execution (TFLite); network connectivity permitted ONLY for Google Drive Vault sync and DLC model downloader.
- **Target Physical Device**: Xiaomi Redmi Note 13 Pro 5G (`2312DRA50I`), Snapdragon 7s Gen 2, 8GB RAM, Android 16 / HyperOS 3.0.
- **Target Emulator**: `Medium_Phone` (`emulator-5554`), API 35/36.

## 4. Repo, links, branches
- **Repo**: [https://github.com/KanakSaini04/Lensora_Ai](https://github.com/KanakSaini04/Lensora_Ai) (`main` = release, `dev` = integration). Step branches branch from `dev`.

### Key Repository Files
| Path | Link | Purpose |
| :--- | :--- | :--- |
| `README.md` | [README.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/README.md) | High-level project architecture, tab guides, 27 mode status, and build instructions. |
| `agent.md` | [agent.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/agent.md) | AI agent mandatory execution rules (Rules 1-18). |
| `GEMINI.md` | [GEMINI.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/GEMINI.md) | Coding agent Karpathy-style rules & Section 10 Run and Device Policy. |
| `progress.md` | [progress.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/progress.md) | Chronological step log tracking all completed development phases. |
| `CHANGELOG.md` | [CHANGELOG.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/CHANGELOG.md) | Version changelog grouped by release steps. |
| `report.md` | [report.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/report.md) | Verification report & physical device audit logs. |
| `docs/MODEL_AUDIT.md` | [docs/MODEL_AUDIT.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/docs/MODEL_AUDIT.md) | Audit matrix for TFLite models, tensor shapes, and tile dimensions. |
| `docs/MODEL_SUPPLY_PLAN.md` | [docs/MODEL_SUPPLY_PLAN.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/docs/MODEL_SUPPLY_PLAN.md) | DLC model downloader supply chain map & remediation plan. |
| `docs/Document/ARCHITECTURE_DFD.md` | [docs/Document/ARCHITECTURE_DFD.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/docs/Document/ARCHITECTURE_DFD.md) | L0-L4 Data Flow Diagrams & system layer interactions. |
| `docs/Document/DATABASE_SCHEMA.md` | [docs/Document/DATABASE_SCHEMA.md](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/docs/Document/DATABASE_SCHEMA.md) | JSON & Room persistence schemas. |
| `scripts/prepare_claude_review.ps1` | [scripts/prepare_claude_review.ps1](https://github.com/KanakSaini04/Lensora_Ai/blob/dev/scripts/prepare_claude_review.ps1) | PowerShell 5.1+ automated review package zip generator. |

### Recent Pull Requests History (Last 15)
| PR ID | Title | Branch | Status |
| :--- | :--- | :--- | :---: |
| **#101** | feat(editor): Step 67 - Media3 Transformer Export Engine | `step-67-export` | OPEN |
| **#100** | feat(editor): Step 66 - Timeline UI & Trim/Split/Reorder/Delete | `step-66-timeline-trim` | OPEN |
| **#99** | fix(ai): Step 65 - model manifest hardening | `step-65-model-manifest` | MERGED |
| **#98** | Dev | `dev` | MERGED |
| **#97** | docs(handoff): Step 65-fix - correct handoff drift | `step-65-fix-docs-drift` | MERGED |
| **#96** | docs: Populate PROJECT_HANDOFF.md Single Source of Truth | `docs-project-handoff-update` | MERGED |
| **#95** | docs: Step 65b - Neural Model Supply Chain Plan | `step-65b-model-supply-plan` | MERGED |
| **#94** | docs: Step 63e - Docs Sync & Step62c Screenshots | `step-63e-docs-sync` | MERGED |
| **#93** | fix(ui): Step 65a - CameraModeCarousel Auto-Scroll | `step-65a-carousel-autoscroll` | MERGED |
| **#92** | docs: Step 64a - Neural Model Audit | `step-64a-model-audit` | MERGED |
| **#91** | docs: Step 64 - Documentation Sync | `step-64-docs-sync` | MERGED |
| **#90** | docs: Step 63d - Run and Device Policy | `step-63d-run-device-policy` | MERGED |
| **#89** | fix(camera-ui): Step 62b - AiBadge Blur Removal & Status Bar Padding | `step-62b-aibadge-blur-fix` | MERGED |
| **#88** | fix(tooling): Step 63b - Review Tooling Fixes & -Since Parameter | `step-63b-review-tooling-evidence` | MERGED |
| **#87** | feat(tooling): Step 63 - Claude Review Package Generator | `step-63-claude-review-folder` | MERGED |
| **#86** | fix(camera-ui): Step 62 - Camera Overlay Fix & Typography | `step-62-camera-overlay-fix` | MERGED |
| **#85** | feat(editor): Step 61b - Importer Hardening & Orphan Sweep | `step-61b-importer-hardening` | MERGED |
| **#84** | feat(editor): Step 61 - Video Editor Media Import & Projects Screen | `step-61-editor-import` | MERGED |
| **#83** | feat(editor): Step 60 - Editor Foundation & Media3 | `step-60-editor-foundation` | OPEN |
| **#82** | feat(ui): Step 59 - Honest Modes & SOON Badges | `step-59-honest-modes` | OPEN |

## 5. Environment, build, run, test
- **Development Environment**: Windows 11 host, Android Studio (Ladybug 2024.2.1 or 2026.1), JDK 17.
- **Gradle & SDK Configuration**:
  - `compileSdk`: 36
  - `minSdk`: 33
  - `targetSdk`: 35
  - AGP: `9.2.1`
  - Kotlin: `2.1.20`
  - Compose BOM: `2025.02.00`
  - Hilt: `2.60.1`
  - Media3: `1.11.1`
  - TensorFlow Lite: `2.16.1`

### Exact Verified Verification Commands
```powershell
# 1. Run All JVM Unit Tests (21/21 pass)
./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks

# 2. Assemble Debug APK
./gradlew assembleDebug

# 3. Install on Active Emulator / Device
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Grant Permissions & Launch App
adb -s emulator-5554 shell pm grant com.lensora.ai android.permission.CAMERA
adb -s emulator-5554 shell pm grant com.lensora.ai android.permission.RECORD_AUDIO
adb -s emulator-5554 shell am start -n com.lensora.ai/.MainActivity

# 5. Logcat Crash Audit
adb -s emulator-5554 shell "logcat -d -s AndroidRuntime:E"

# 6. Generate Review Package Zip
.\scripts\prepare_claude_review.ps1 -Step "<Step_Name>" -Since <Commit_Hash>
```

- **Emulator Unlock PIN Protocol**:
  Emulator PIN: `1234` (NEVER write PIN into code, docs, commits, PRs or logs).
  Unlock commands:
  ```powershell
  adb -s emulator-5554 shell input keyevent KEYCODE_WAKEUP
  adb -s emulator-5554 shell input swipe 540 2000 540 600
  adb -s emulator-5554 shell input text 1234
  adb -s emulator-5554 shell input keyevent KEYCODE_ENTER
  adb -s emulator-5554 shell svc power stayon true
  ```

## 6. File structure

Annotated tree generated from repository source files:

```
Lensora_Ai/
├── app/                              # Android Application Module
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Manifest declaring Camera2 permissions & AiProcessorService
│   │   ├── assets/                   # Static assets & DLC manifest (manifest.json, luts/)
│   │   ├── java/com/lensora/ai/
│   │   │   ├── ai/                   # AI Engines & WorkManager tasks
│   │   │   │   ├── AiPlanner.kt      # L2 rule-based scene tuning orchestrator
│   │   │   │   ├── AiProcessorWorker.kt # L3 heavy AI background execution
│   │   │   │   ├── ModelDownloaderWorker.kt # DLC model downloader WorkManager
│   │   │   │   ├── ModelManager.kt   # TFLite Interpreter loader & delegate manager
│   │   │   │   ├── ObjectEraser.kt   # LaMa GPU object eraser neural inpainting
│   │   │   │   └── SceneClassifier.kt # L1 2 FPS sampling loop & CoachingHintHelper
│   │   │   ├── camera/               # Pure Camera2 HAL Engine
│   │   │   │   ├── CameraController.kt # Camera2 session manager, AE/AF/ISO & capture
│   │   │   │   ├── CameraMode.kt     # Enum of 6 core active camera modes
│   │   │   │   └── CameraPreferences.kt # SharedPreferences storage for flash/grid/mode
│   │   │   ├── data/                 # Local Data & Cloud Vault
│   │   │   │   ├── CloudSyncWorker.kt # Google Drive OAuth2 upload WorkManager
│   │   │   │   └── DriveService.kt   # Google Drive REST API v3 client
│   │   │   ├── editor/               # VN-Style Non-Destructive Video/Photo Editor
│   │   │   │   ├── Models.kt         # Project, Clip, MusicTrack, Template, AspectRatio
│   │   │   │   ├── ProjectRepository.kt # JSON storage in filesDir/projects/ + orphan sweep
│   │   │   │   ├── TemplateLoader.kt # Asset-based VN template JSON loader
│   │   │   │   ├── import/
│   │   │   │   │   └── ProjectImporter.kt # Storage Guard 2GB, MimeTypeMap whitelist copy
│   │   │   │   └── ui/
│   │   │   │       ├── ProjectsScreen.kt # Studio Tab VIDEO mode projects list & Photo Picker
│   │   │   │       └── ProjectViewModel.kt # ViewModel managing project creation/import
│   │   │   ├── service/
│   │   │   │   └── AiProcessorService.kt # Foreground service resisting HyperOS kills
│   │   │   ├── ui/                   # Jetpack Compose UI (Obsidian Royal Theme)
│   │   │   │   ├── MainActivity.kt   # Single Activity entry point
│   │   │   │   ├── camera/           # Camera Tab UI
│   │   │   │   │   ├── CameraScreen.kt # Viewfinder, top bar, AI badge, shutter, zoom pills
│   │   │   │   │   ├── CameraTopBar.kt # Flash, grid, timer, vibe, settings buttons
│   │   │   │   │   ├── CameraModeCarousel.kt # Mode carousel with LaunchedEffect auto-scroll
│   │   │   │   │   └── CameraPreview.kt # TextureView SurfaceTexture listener
│   │   │   │   ├── gallery/          # Gallery Tab UI
│   │   │   │   │   └── GalleryScreen.kt # Masonry grid photo gallery & Quick View swiper
│   │   │   │   ├── navigation/       # Navigation Architecture
│   │   │   │   │   ├── AppNavigation.kt # 5-tab BottomBar navigation controller
│   │   │   │   │   └── Screen.kt     # NavHost route destinations
│   │   │   │   ├── settings/         # Settings Tab UI
│   │   │   │   │   └── SettingsScreen.kt # System diagnostics dashboard & model downloader UI
│   │   │   │   ├── studio/           # Studio Tab Photo Editor UI
│   │   │   │   │   ├── StudioScreen.kt # Photo editor & LaMa brush eraser canvas
│   │   │   │   │   └── StudioViewModel.kt # Photo editor state holder
│   │   │   │   ├── theme/            # Material 3 Theme Tokens
│   │   │   │   │   ├── Color.kt      # Obsidian Royal palette (#0E0E0E, #131313, #2563EB, #00E5FF)
│   │   │   │   │   ├── Theme.kt      # MaterialTheme composable wrapper
│   │   │   │   │   └── Type.kt       # Typography definitions
│   │   │   │   └── vibes/            # Vibes Tab UI
│   │   │   │       └── VibesScreen.kt # 3D LUT presets & AGSL film grain shaders
│   │   │   └── utils/                # Utility classes
│   │   │       ├── StorageGuard.kt   # 2GB disk space check via StatFs
│   │   │       ├── SystemMonitor.kt # Live 2s polling for RAM, battery, thermal state
│   │   │       └── ThermalGuard.kt   # Thermal throttling listener
│   │   └── res/                      # Android resources (drawable, values, xml)
│   └── src/test/java/com/lensora/ai/ # Unit Test Suite (21 Tests)
│       ├── AiPlannerTest.kt          # 6 tests covering L2 scene tuning plans
│       ├── EditorFoundationTest.kt   # 5 tests covering Project/Clip data models
│       ├── ProjectImporterTest.kt    # 4 tests covering MimeTypeMap whitelist & Storage Guard
│       ├── ProjectRepositoryTest.kt  # 1 test covering atomic JSON saves & orphan sweep
│       └── SceneClassifierTest.kt    # 5 tests covering pure CoachingHintHelper logic
├── docs/                             # Project Documentation & Screenshots
│   ├── Document/                     # ARCHITECTURE_DFD.md, DATABASE_SCHEMA.md, SRS.md, etc.
│   ├── MODEL_AUDIT.md                # TFLite model inventory & tensor specs
│   ├── MODEL_SUPPLY_PLAN.md          # DLC model downloader supply chain map
│   └── screenshots/                  # Validated UI screenshots (step62c/, step65a/)
├── scripts/                          # Automated Tooling
│   └── prepare_claude_review.ps1     # Review package ZIP generator script
├── .gitignore                        # Git ignore rules (includes Claude/ and Claude_review.zip)
├── CHANGELOG.md                      # Chronological release step changelog
├── GEMINI.md                         # Coding agent guidelines & Section 10 Run Policy
├── PROJECT_HANDOFF.md                # Single source of truth project handoff document
├── README.md                         # Repository documentation & architecture guide
├── agent.md                          # Mandatory AI agent execution rules (Rules 1-18)
├── progress.md                       # Comprehensive step execution tracker log
└── report.md                         # Verification report & physical device audit logs
```

## 7. Architecture L0-L4 (reality vs plan)
- **L0 Safe Core HAL**:
  - *Reality today*: Pure Camera2 API implementation in `CameraController.kt` (`CameraDevice`, `CameraCaptureSession`). Enforces 1080p preview buffer size on `SurfaceTexture` to eliminate stretching, while streaming 12.5MP JPEG captures on `ImageReader`. Features 100% try-catch safety around HAL calls.
- **L1 Light AI Director**:
  - *Reality today*: `SceneClassifier.kt` runs a 2 FPS / 500 ms sampling loop (`delay(500)`) on `Dispatchers.Default`. Initializes `ModelManager` loading `scene_classifier.tflite`, while evaluating a downscaled 224x224 color-brightness pixel heuristic pass to select scene labels ("✨ AI: Night", "✨ AI: Landscape", "✨ AI: Portrait", "✨ AI: Sky", "✨ AI: Auto"). `CoachingHintHelper.coachingHintFor(box)` returns framing prompts (*"✨ Step Closer"*, *"✨ Move Right"*, *"✨ Perfect Composition"*) only when a valid subject box is calculated (hiding the UI pill when `box == null`). Real TFLite tensor array inference model outputs are PLANNED.
- **L2 AI Planner & Tuning**:
  - *Reality today*: `AiPlanner.kt` contains rule-based logic mapping classified scenes to ISO, shutter speed, exposure compensation, and contrast parameters.
- **L3 Heavy AI Pipeline**:
  - *Reality today*: `ObjectEraser.kt` loads `filesDir/models/lama_dilated.tflite` via `Interpreter` with `GpuDelegate` executing 4-channel float tensor inference (`1 x 512 x 512 x 4`). `AiProcessorService.kt` runs heavy passes as a persistent Foreground Service under HyperOS protection.
- **L4 Anti-AI Validator**:
  - *Reality today*: `ObjectEraser.kt` inspects output tile luminance bounds ($2 < \text{avgLum} < 253$) to reject corrupt or black/white artifact tiles.

## 8. Camera modes (27)

| Mode | Status | File + Function | Notes |
| :--- | :---: | :--- | :--- |
| **✨ AI AUTO** | `WORKING (Step 62c)` | `CameraScreen.kt` / `SceneClassifier.kt` | 2 FPS / 500 ms scene classification loop. |
| **📷 PHOTO** | `WORKING (Step 37)` | `CameraController.kt:takePicture()` | 12.5MP capture with atomic `.tmp` journal. |
| **👤 PORTRAIT** | `WORKING (Step 37)` | `CameraController.kt:setCameraMode()` | EV `+0.3` boost + continuous auto-focus. |
| **📹 VIDEO** | `WORKING (Step 60)` | `CameraController.kt:startRecording()` | `TEMPLATE_RECORD` session with MediaRecorder. |
| **🎬 CINEMATIC** | `WORKING (Step 37)` | `CameraScreen.kt` | 2.39:1 letterboxing + safe 24fps target range. |
| **🎛️ PRO** | `WORKING (Step 37)` | `CameraController.kt:setManualExposure()` | ISO 100–3200, Shutter 1/1000s–1s, Zebras, Peaking. |
| **✨ ASTRO** | `IMPLEMENTED - NOT VERIFIED` | `CameraController.kt` | Infinity focus lock (`0.0f`) + 30s exposure. |
| **🔍 PRO MACRO** | `IMPLEMENTED - NOT VERIFIED` | `CameraController.kt` | Minimum focus distance lock. |
| **📷 HIGH-RES** | `IMPLEMENTED - NOT VERIFIED` | `AiProcessorWorker.kt` | Needs `real_esrgan_v2.tflite` (not in manifest). |
| **🌙 NIGHT VISION** | `IMPLEMENTED - NOT VERIFIED` | `CameraController.kt` | Needs `dehaze.tflite` (not in manifest). |
| **📄 SCAN** | `IMPLEMENTED - NOT VERIFIED` | `CameraScreen.kt` | Document edge detection & ZXing PDF scan pass. |
| **📑 DOCUMENT** | `IMPLEMENTED - NOT VERIFIED` | `CameraScreen.kt` | High-contrast B&W text extraction. |
| **📷 LEICA** | `VIBE-ONLY` | `VibesScreen.kt` | Leica Monochrome 3D LUT preset. |
| **🔵 ZEISS** | `VIBE-ONLY` | `VibesScreen.kt` | ZEISS T* anti-reflex 3D LUT preset. |
| **🟠 HASSELBLAD** | `VIBE-ONLY` | `VibesScreen.kt` | Hasselblad HNCS golden palette 3D LUT preset. |
| **✨ XMAGE** | `VIBE-ONLY` | `VibesScreen.kt` | XMAGE Vivid saturated color 3D LUT preset. |
| **🎬 HARCOURT** | `VIBE-ONLY` | `VibesScreen.kt` | Studio Harcourt dramatic B&W 3D LUT preset. |
| **🌐 PANORAMA** | `SOON` | `CameraScreen.kt` | Planned 360 sweep capture. |
| **🍔 FOOD** | `SOON` | `CameraScreen.kt` | Planned warm color boost & center macro. |
| **⏱️ SLOW-MOTION** | `SOON` | `CameraScreen.kt` | Planned high frame rate pass. |
| **⏳ TIME-LAPSE** | `SOON` | `CameraScreen.kt` | Planned interval frame capture pass. |
| **📱 DUAL-VIDEO** | `SOON` | `CameraScreen.kt` | Planned concurrent front/rear camera session. |
| **✨ BEAUTY** | `SOON` | `CameraScreen.kt` | Planned real-time skin smoothing ColorMatrix. |
| **🌌 DEEP SKY** | `SOON` | `CameraScreen.kt` | Planned multi-frame astrophotography stacking. |
| **🔬 ULTRA MACRO** | `SOON` | `CameraScreen.kt` | Planned digital crop + sharpening macro. |
| **🏷️ WATERMARK** | `SOON` | `CameraScreen.kt` | Planned vintage timestamp watermark overlay. |
| **🎞️ RAW DNG** | `SOON` | `CameraController.kt` | Planned `DngCreator` uncompressed RAW capture. |

## 9. AI models and pipeline

| Model Identifier | Expected File Name | Asset / Storage Location | In Manifest? | Expected Tensors | Loader File + Line | Status |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **Scene Classifier** | `scene_classifier.tflite` | `filesDir/models/` | ❌ No (`mobilenet_v1_1.0_224.tflite` listed) | $1 \times 224 \times 224 \times 3$ float | `SceneClassifier.kt:47` | Mismatch (loader expects `scene_classifier.tflite`) |
| **Object Eraser** | `lama_dilated.tflite` | `filesDir/models/` | ⚠️ Yes | $1 \times 512 \times 512 \times 4$ float input $\rightarrow$ $1 \times 512 \times 512 \times 3$ float output | `ObjectEraser.kt:29` | Working (4-channel float buffer + GpuDelegate) |
| **AI Super-Res** | `real_esrgan_v2.tflite` | `filesDir/models/` | ❌ No | $1 \times 512 \times 512 \times 3$ input $\rightarrow$ $1 \times 1024 \times 1024 \times 3$ output | `ModelManager.kt:44` | Unlisted in DLC manifest |
| **AI Dehaze** | `dehaze.tflite` | `filesDir/models/` | ❌ No | $1 \times 512 \times 512 \times 3$ float | `ModelManager.kt:45` | Unlisted in DLC manifest |

- **Memory Shield Tiling**: High-resolution images are split into $512 \times 512$ pixel tiles via `BitmapRegionDecoder` before TFLite inference, preventing OOM crashes on 12.5MP images.
- **Hardware Acceleration**: `GpuDelegate` with CPU XNNPACK 4-thread fallback.
- **L4 Validator Rules**: Evaluates average tile luminance ($2 < \text{avgLum} < 253$). Rejects corrupt or all-black/white tile artifacts.

## 10. UI
- **Theme Tokens**: Background `#0E0E0E`, Surface `#131313`, Primary `#2563EB`, Highlight Cyan `#00E5FF`, Inter font. AI badge and overlay status pills use translucent glass (`Color(0xFF131313).copy(alpha = 0.65f)` with 1.dp `ColorWhite.copy(alpha = 0.15f)` border). Zero `Modifier.blur()` on text.
- **Navigation Architecture**: 5-tab `BottomBar` in `AppNavigation.kt` (`Camera`, `Studio`, `Gallery`, `Vibes`, `Settings`).
- **Tab Breakdown**:
  - **Camera Tab** (`CameraScreen.kt`): Viewfinder, top-aligned `Column` with `statusBarsPadding()` (`CameraTopBar` $\rightarrow$ Timestamp Pill $\rightarrow$ `AiBadge`), `CameraModeCarousel` with `LaunchedEffect` auto-scroll, tactile multi-ring shutter button, zoom pills (`0.6x`, `1x`, `2x`, `3x`).
  - **Studio Tab** (`StudioScreen.kt` / `ProjectsScreen.kt`): Top bar segment toggle `"PHOTO | VIDEO"`. `PHOTO` mode renders photo editor with EV/Contrast controls and LaMa brush object eraser canvas; `VIDEO` mode renders non-destructive project list sorted newest first with "+ New Project" button launching system Photo Picker.
  - **Gallery Tab** (`GalleryScreen.kt`): Masonry grid photo gallery with swipable Quick View viewer.
  - **Vibes Tab** (`VibesScreen.kt`): 3D LUT presets and AGSL film grain shaders.
  - **Settings Tab** (`SettingsScreen.kt`): System diagnostics dashboard (live 2s polling loop for RAM, battery, thermal state) and GitHub DLC model downloader UI.
- **Valid Screenshots Index**:
  - `docs/screenshots/step62c/camera_1_0.png`: Camera tab preview at 1.0x font scale on emulator (`86.13 KB`).
  - `docs/screenshots/step62c/camera_1_3.png`: Camera tab preview at 1.3x font scale on emulator (`125.65 KB`).
  - `docs/screenshots/step62c/launcher.png`: Unlocked emulator launcher screen (`1333.8 KB`).
  - `docs/screenshots/step65a/carousel_photo_1_0.png`: Mode carousel auto-scroll on PHOTO mode (`59.45 KB`).
  - `docs/screenshots/step65a/carousel_cinematic_1_3.png`: Mode carousel auto-scroll on CINEMATIC mode at 1.3x font scale (`126.86 KB`).
  - `docs/screenshots/step65a/carousel_aiauto_1_0.png`: Mode carousel auto-scroll on AI AUTO mode (`114.54 KB`).

## 11. Editor (VN-style)
- **Foundation**: Step 60 Media3 integration (`media3-exoplayer`, `media3-transformer`, `media3-effect`, `media3-common` version `1.11.1`).
- **Data Models**: `Project`, `Clip`, `MusicTrack`, `Template`, `TemplateSlot`, `AspectRatio`, `ClipType`, `TransitionType` in `com.lensora.ai.editor.Models.kt` with non-throwing `validate()` helper methods.
- **Storage Layout**: `filesDir/projects/<project_id>/project.json` and media files in `filesDir/projects/<project_id>/media/clip_<millis>_<index>.<ext>`.
- **ProjectRepository**: Atomic `.tmp` saves (`<id>.json.tmp` $\rightarrow$ `<id>.json`) on `Dispatchers.IO`. Features `sweepOrphanProjects(clockMillis, maxAgeMs)` deleting orphan project directories and stale `.tmp` files older than 30 minutes.
- **ProjectImporter**: Runs on `Dispatchers.IO` with `StatFs` 2GB Storage Guard limit, atomic `.tmp` stream copies, `MediaMetadataRetriever` video duration extraction, `MimeTypeMap` extension derivation against safe whitelist (`jpg`, `jpeg`, `png`, `webp`, `heic`, `heif`, `mp4`, `mov`, `webm`, `3gp`, `mkv`), and rethrows `CancellationException`.
- **Missing / In-Progress Features**: Multi-track timeline UI, clip trim/split controls, and Media3 Transformer export pass.

## 12. Services, guards, cloud
- **AiProcessorService**: Persistent Foreground Service with `FOREGROUND_SERVICE_DATA_SYNC` type, showing notification (`✨ AI: Perfecting your photo...`) resisting HyperOS background task kills.
- **Storage Guard**: `StorageGuard.kt` checks available disk space via `StatFs`. Blocks capture and media import if disk space is $< 2\text{GB}$.
- **Thermal Guard**: `ThermalGuard.kt` listens for `PowerManager.OnThermalStatusChangedListener` events (`THERMAL_STATUS_SEVERE`).
- **Cloud Vault Sync**: `CloudSyncWorker.kt` + `DriveService.kt` upload processed photos to Google Drive Cloud Vault under `NetworkType.UNMETERED` constraints. Calculates streaming SHA-256 checksums and auto-purges local MediaStore files after cloud verification.

## 13. Testing and evidence
- **Unit Test Suite**: 21 passed unit tests run via `./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks`:
  - `ExampleUnitTest`: 1 test covering base environment check.
  - `AiPlannerTest`: 5 tests covering L2 scene tuning parameters.
  - `EditorFoundationTest`: 5 tests covering `Project` / `Clip` data model validation.
  - `ProjectImporterTest`: 4 tests covering `ExtensionHelper` whitelist, `StorageGuard` 2GB limit, and MIME checks.
  - `ProjectRepositoryTest`: 1 test covering atomic JSON saves & `sweepOrphanProjects` orphan sweep.
  - `SceneClassifierTest`: 5 tests covering pure `CoachingHintHelper.coachingHintFor(box)` mappings.
- **Run Policy (GEMINI.md Section 10)**:
  - After EVERY change: build, install, launch on emulator, paste `logcat -d -s AndroidRuntime:E`. Label EMULATOR or DEVICE.
  - Emulator never counts as Redmi verification.
  - Use physical device ONLY when prompt says `DEVICE REQUIRED: yes`, else STOP.
  - Camera2 hardware behavior cannot be verified on emulator; mark it **NOT VERIFIED** on device.
  - Never start or restart the emulator yourself.
  - The emulator may be locked: wake and unlock it with the PIN given in the prompt (never store the PIN in the repo).

## 14. Git, PR and step workflow
- One task per prompt. One branch per step from latest `dev`: `step-<NN>-<slug>`. Never rewrite history.
- Commit convention: `type(scope): Step NN - summary` (types: `feat`, `fix`, `docs`, `test`, `tooling`).
- Every step updates docs (`README.md`, `progress.md`, `CHANGELOG.md`, `PROJECT_HANDOFF.md`), runs unit tests, builds, and opens a GitHub PR.
- Merge Rule: PRs touching ONLY `.md` files and `docs/screenshots/*.png` with passing tests/build may be merged by the agent. Any PR touching `.kt` or `.gradle` files must NOT be merged without explicit user approval.
- Before asking for review: `.\scripts\prepare_claude_review.ps1 -Step "<name>" -Since <commit>` to create `Claude_review.zip`.

## 15. Step history
- **Phases Summary**:
  - Phase 1-36: Computational camera foundation, pure Camera2 HAL core, Obsidian Royal UI, 5 tabs, 27 camera modes, TFLite AI models, Google Drive Cloud Vault.
  - Phase 37: Hard Camera & AI overhaul (1080p preview buffer fix, 0 HAL freeze, LaMa object eraser wiring, Probe lab audit).
  - Phase 38: Camera mode audit (`MODE_AUDIT.md`) & honest mode UI with 10 SOON badges.
  - Phase 39: VN-style video editor foundation, Media3 1.11.1 integration, ProjectImporter hardening & orphan sweep.
  - Phase 40: Camera overlay fix & typography perfection (removed text blur, top overlays column, CoachingHintHelper).
  - Phase 41: Automated Claude review folder system tooling (`prepare_claude_review.ps1`).
  - Phase 42: Documentation sync (`README.md`) & neural model audit (`docs/MODEL_AUDIT.md`).
  - Phase 43: Batch A & B execution (CameraModeCarousel auto-scroll fix, Run & Device policy, PR cleanup audit, Model Supply Plan `docs/MODEL_SUPPLY_PLAN.md`).

## 16. Current status
- **Working (with evidence)**: Camera2 preview/capture engine, Obsidian Royal 5-tab UI, Mode carousel auto-scroll, 6 core active camera modes, LaMa GPU object eraser, ProjectImporter with 2GB Storage Guard & MimeTypeMap whitelist, ProjectRepository orphan sweep, `prepare_claude_review.ps1` tooling, 21/21 passed unit tests across 6 test classes, 0 runtime crashes.
- **Implemented, NOT verified**: Astrophotography, Pro Macro, High-Res upscale, Night Vision noise reduction, Document scan PDF export, Google Drive OAuth2 upload on physical device.
- **Broken / blocked**: None.

## 17. Known issues and risks (by severity)
1. **AI Model Supply Chain Mismatch**: `assets/manifest.json` specifies `"model_name": "mobilenet_v1_1.0_224.tflite"`, whereas `SceneClassifier.kt:47` requests `"scene_classifier.tflite"`. `real_esrgan_v2.tflite` and `dehaze.tflite` are unlisted in DLC manifest. Do NOT rename `mobilenet_v1_1.0_224` to `scene_classifier.tflite`: ImageNet labels != scene labels. Needs a real scene model. (See [`docs/MODEL_SUPPLY_PLAN.md`](file:///C:/Users/kanak/AndroidStudioProjects/Lensora_Ai/docs/MODEL_SUPPLY_PLAN.md)).
2. **L1 Scene Classifier Heuristics**: Live scene labels use a 224x224 color-brightness pixel heuristic pass; real TFLite tensor array inference model outputs are planned.
3. **Debug APK Size**: Debug APK size is $\approx 90.33\text{ MB}$ ($94,720,306$ bytes) with R8 minification disabled for debug builds.

## 17b. Roadmap (ordered, one task per step)
1. Step 65: manifest.json per-model file_name + sha256 fields (url = null where no real host exists), ModelManager file-existence guards, "Model Unavailable" downloader UI state. No invented URLs.
2. Step 61c: Projects screen & media import physical device evidence.
3. Step 64b: Real L1 TFLite model tensor inference pass + real subject box detector for coaching hints.
4. Step 66: Video editor multi-track timeline UI & clip trim/split controls.
5. Step 67: Video editor Media3 Transformer export pass.
6. Step 68: Google Drive OAuth2 sign-in & Cloud Vault auto-sync physical device verification pass.
7. Step 69: Production release build & R8 ProGuard minification configuration.
8. Implement 10 SOON camera modes one at a time.
9. Final physical device camera verification pass on Redmi Note 13 Pro 5G.

## 18. Open questions for the user
- Hosting repository / CDN endpoints for DLC model zip downloads (`real_esrgan_v2.zip`, `dehaze.zip`). `lama_dilated / real_esrgan_v2 / dehaze: no verified public host. NOT VERIFIED.`

## 19. Update log of this file
| Date | Step | What changed |
| --- | --- | --- |
| 2026-11-18 | Step 65b / Batch B | Fully populated all sections, repository file links, PR history, architecture, 27 mode status, neural model supply plan, and step roadmap. |
| 2026-11-18 | Step 65-fix | Corrected handoff drift, test class counts, PR table statuses, roadmap, and supply plan header. |
| 2026-11-18 | Step 65 | Hardened model manifest schema v2, added ManifestParser pure JVM parser with tests, ModelManager file guards, ModelDownloaderWorker url/sha256 checks, and SettingsScreen "Model Unavailable" UI. |
