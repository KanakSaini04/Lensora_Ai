# Changelog - Lensora_Ai

## [Unreleased] - Master Build Phase P1 (2026-11-18)

### Changed
- **Task 1.1**: Removed fake sample generator logic and text video files in `ProjectsScreen.kt`; EmptyProjectsState "Create New Project" button now triggers System PhotoPicker.
- **Task 1.2**: Unified Editor view headers so Projects TopAppBar renders only in list view, and added Delete Project icon button to `EditorScreen.kt` header.
- **Task 1.3**: Configured `DialogProperties(dismissOnClickOutside=false, dismissOnBackPress=false)` for running export dialogs, and added explicit export success/failure alert dialogs.
- **Task 1.4**: Updated `ExportWorker.kt` with composition-level CROP_TO_FILL aspect scaling, photo audio removal, MediaStore error handling & staging cleanup, and WorkManager dataSync foreground service promotion.
- **Task 1.6**: Ensured editor selection safety after clip delete/undo/redo with safe fallbacks, and verified project JSON persistence.
- **Task 2.2**: Implemented `ModelContractChecker.kt` to validate input/output tensor shapes and dtypes against expected model contracts (512x512, 2x super-res) and safely expose Incompatible state.
- **Task 2.3**: Built `TiledPipelineEngine.kt` supporting 1x (LaMa/Dehaze) and 2x (Real-ESRGAN v2) tiled inference with canvas stitching and immediate bitmap recycling.

## [Unreleased] - Step 67b (2026-11-18)

### Added
- **Real Media3 Transformer Export Engine**: Updated `ExportWorker.kt` to run real `androidx.media3.transformer.Transformer` composition exports on Main Looper via `suspendCancellableCoroutine`, with `EditedMediaItemSequence`, `ClippingConfiguration`, `Presentation` height scaling, progress polling, and MediaStore `IS_PENDING` publishing pattern.
- **Export Item Mapper**: Extracted `ExportItemMapper.toMappingParams()` pure mapping utility in `ExportPlanBuilder.kt`.
- **Unit Testing**: Added `ExportItemMapper` mapping verification test to `ExportPlanBuilderTest.kt` (bringing total unit test suite to 45/45 passed across 9 test classes).

## [Unreleased] - Step 66 (2026-11-18)

### Added
- **Timeline Edit Engine**: Created pure `TimelineEditEngine.kt` supporting clip `trim()`, `split()`, `reorder()`, `delete()`, and `totalDurationMs()` calculation with bound safety.
- **Undo / Redo Stack**: Implemented `TimelineUndoRedoStack` in-memory history stack tracking up to 50 timeline states.
- **Video Editor Timeline UI**: Built `EditorScreen.kt` featuring Media3 ExoPlayer preview player, scrubber bar, horizontal clip timeline, and clip edit controls panel.
- **Unit Testing**: Created `TimelineEditEngineTest.kt` unit test suite covering trim bounds, zero-length, split at edges, reorder bounds, delete missing id, and undo/redo stack (bringing total unit test suite to 40/40 passed across 8 test classes).

## [Unreleased] - Step 65 / 65c (2026-11-18)

### Changed
- **Model Manifest Hardening (v2)**: Updated `app/src/main/assets/manifest.json` schema v2 with keys `scene_classifier`, `lama_dilated`, `real_esrgan_v2`, `dehaze` (`url: null`, `sha256: null`).
- **Pure JVM Utilities**: Extracted `ModelFileGuard` (`isUsable()`) and `Sha256Util` (`hex()`, `matches()`) pure Kotlin production utilities for file validation and streaming SHA-256 calculation.
- **Pure JVM Manifest Parser**: Implemented pure Kotlin `ManifestParser.kt` and `ModelEntry` supporting complex query URLs, escaped strings, duplicate keys, and unknown keys/objects.
- **Model Manager & Downloader**: Updated `ModelManager.loadModel()` and `ModelDownloaderWorker.kt` to use `ModelFileGuard` and `Sha256Util`.
- **Scene Classifier Fallback**: Updated `SceneClassifier.kt` to fall back gracefully to color-brightness heuristics when model file is unavailable.
- **Model Unavailable UI**: Updated `SettingsScreen.kt` to reflect manifest entries and display "Model Unavailable" disabled button state when models are unhosted and missing locally.
- **Unit Testing**: Created `ManifestParserTest.kt` unit test suite covering parser edge cases, `ModelFileGuard`, and `Sha256Util`.

## [Unreleased] - Step 62 (2026-11-17)

### Fixed
- **Camera Screen Overlays & Typography**: Removed `Modifier.blur()` from AI badge and overlay pills in `CameraScreen.kt` to fix blurred unreadable text. Replaced with sharp, translucent dark glass background (`Color(0xFF131313)` at 0.65 alpha with 1.dp subtle border).
- **Top Overlays Collision**: Restructured top overlays into a single vertical `Column` (`CameraTopBar` $\rightarrow$ Timestamp Pill $\rightarrow$ AI Badge) respecting status bar insets to guarantee zero collision or overlap at 1.0x and 1.3x font scales on all screen sizes.
- **Composition Coaching Hint**: Implemented pure `CoachingHintHelper.coachingHintFor(box: RectF?)` in `SceneClassifier.kt` returning `null` when no subject bounding box is detected, hiding the coaching hint pill UI completely when `null`.
- **Unit Testing**: Added `SceneClassifierTest.kt` unit test suite covering pure `coachingHintFor` logic for null, centered, small, left, and right subject bounding boxes.

## [Unreleased] - Step 63 (2026-11-17)

### Added
- **Claude Review Tooling**: Created `scripts/prepare_claude_review.ps1` for automated, atomic creation of `Claude/` workspace and `Claude_review.zip` review archives. Includes secrets scanning, screenshot validation, test summaries, and `00_README_FIRST.md` problem reports.

## [Unreleased] - Step 62b (2026-11-18)

### Fixed
- **AiBadge Blur Removal**: Step 62b: AiBadge blur was NOT removed in Step 62; removed now. Deleted `.blur(12.dp)` from `AiBadge` in `CameraScreen.kt` and removed unused import.
- **Top Bar Insets**: Added `statusBarsPadding()` to top overlay `Column` in `CameraScreen.kt` to ensure `CameraTopBar`, Timestamp Pill, and `AiBadge` clear status bars and cutouts cleanly.

## [Unreleased] - Step 63d (2026-11-18)

### Added
- **Run and Device Policy**: Updated `GEMINI.md` (Section 10) and `agent.md` (Rule 18) defining explicit mandatory emulator run verification protocols, logging labels, and physical device constraints. Deleted legacy black screenshot `camera_font_1_0.png`.

## [Unreleased] - Step 64 (2026-11-18)

### Changed
- **Documentation Synchronization**: Synchronized `README.md` with verified code architecture (2 FPS / 500 ms AI sampling rate, compileSdk 36, minSdk 33, targetSdk 35, 21 passed unit tests, honest 27 Camera Modes status table, honest L1 SceneClassifier heuristic description, and Known Limitations section).

## [Unreleased] - Step 64a (2026-11-18)

### Added
- **Neural Model Audit**: Created `docs/MODEL_AUDIT.md` performing a read-only audit of offline neural models (`scene_classifier.tflite`, `lama_dilated.tflite`, `real_esrgan_v2.tflite`, `dehaze.tflite`), mapping asset presence, DLC manifest definitions, tile input tensor dimensions, and super-resolution output expansion notes.

## [Unreleased] - Step 65b (2026-11-18)

### Added
- **Model Supply Chain Plan**: Created `docs/MODEL_SUPPLY_PLAN.md` mapping DLC manifest endpoints, WorkManager download destinations, and exact runtime loader file expectation mismatches across `SceneClassifier.kt`, `ModelManager.kt`, and `SettingsScreen.kt`.

## [Unreleased] - Step 65-fix (2026-11-18)

### Fixed
- **Documentation Drift Corrections**: Corrected `PROJECT_HANDOFF.md` status block, test class counts (21/21 passed across 6 test classes), PR table statuses, roadmap, supply plan header (`Step 65b`), null DLC manifest URLs, and added Scene Classifier caveat section.
