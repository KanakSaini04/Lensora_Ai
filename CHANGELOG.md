## [Unreleased] - Step Z7: App-Wide Portrait Lock & Hardware Sensor Orientation (2026-11-22)

### Added
- **App-Wide Portrait Lock & Hardware Sensor Orientation (Step Z7)**:
  - `AndroidManifest.xml`: Locked `.MainActivity` to portrait mode (`android:screenOrientation="portrait"`).
  - `ZoomableBox.kt`: Created pure evaluator `snapSensorDegreesToSurfaceRotation` mapping sensor angles to 0°, 90°, 180°, 270° surface rotation values with `ORIENTATION_UNKNOWN` (-1) fallback handling.
  - `CameraScreen.kt`: Registered `OrientationEventListener` bound to screen lifecycle (`DisposableEffect`) to track physical device rotation independent of window display rotation.
  - `PhotoZoomAndNavigationTest.kt`: Added boundary unit tests for `snapSensorDegreesToSurfaceRotation` and rotation math unit tests for back (sensor 90°) and front (sensor 270°) camera orientation formulas. All 119 unit tests passing cleanly.

## [Unreleased] - Step Z6: Studio Screen Redesign (Lightroom Mobile Style) (2026-11-22)

### Added
- **Studio Screen Redesign (Step Z6)**:
  - `StudioScreen.kt`: Redesigned Lightroom Mobile layout with ~65% photo height, top bar with Back button, Undo, Redo, Hold-to-Compare button, Export; secondary sub-header with PHOTO/VIDEO toggle, Time toggle, Reset All Edits button.
  - `ZoomableBox.kt`: Added "Fit" reset chip overlay and double-tap zoom reset (when Eraser inactive). Pure math functions `calculateLocalCanvasBrushWidthPx` and `calculateImageSpaceBrushRadiusPx`.
  - `StudioStrokeModels.kt` & `StudioViewModel.kt`: Created pure `StudioStroke`, `MaskComposer`, and `StrokeHistory` classes with 25-step undo history stack retained across screen rotations without `TransactionTooLargeException`.
  - `PhotoZoomAndNavigationTest.kt`: Added unit tests for brush radius math across zoom scales, 25-step undo history clamping, filmstrip selection mask reset, process eraser mask reset, and mask accuracy pixel coverage tests (J). All 115 unit tests passing cleanly.

## [Unreleased] - Step Z5: Studio Eraser Brush Drawing at All Zoom Levels (2026-11-22)

### Added
- **Studio Eraser Brush Drawing at All Zoom Levels (Step Z5)**:
  - `ZoomableBox.kt`: Added `TouchMode` enum and `decideTouchMode` pure evaluator function. Evaluates `TouchMode.DRAW` for 1-finger drags when Eraser is active in Studio at all zoom levels ($100\%$ to $800\%$), `TouchMode.ZOOM_PAN` for 2+ pointers, and `TouchMode.PAN` for non-draw tools when zoomed.
  - `StudioScreen.kt`: Passed `isStudio = true` and `isDrawToolActive = isEraserModeActive` to `ZoomableBox`. Mid-stroke 2-finger touch ends active stroke cleanly without stray lines and switches to zoom/pan.
  - `PhotoZoomAndNavigationTest.kt`: Added unit tests for `decideTouchMode` across all scales and tools, and screen-to-image coordinate mapping tests across 1.0x, 1.4x, 4.0x, 8.0x scales and pan offsets. All 110 unit tests passing cleanly.

## [Unreleased] - Step Z4: Camera Viewfinder Swipe Gesture & Timestamp Pill Removal (2026-11-22)

### Added
- **Camera Viewfinder Swipe Gesture & Timestamp Pill Removal (Step Z4)**:
  - `ZoomableBox.kt`: Added `CameraSwipeDecision` enum and `evaluateCameraSwipeGesture` pure evaluator function checking 1-pointer count, $24\text{dp}$ system edge gesture insets, and measured UI exclusion bounds (`topBarBounds`, `aiBadgeBounds`, `zoomPillsBounds`, `carouselBounds`, `shutterRowBounds`).
  - `CameraScreen.kt`: Attached dynamic pointer tracking on viewfinder overlay Box. Removed orphan `currentTimeString` state, LaunchedEffect ticking loop, and viewfinder `"LENSORA • hh:mm:ss"` overlay pill.
  - `CameraSwipeGestureTest.kt`: Added 15 unit tests covering left swipe, right swipe, vertical/diagonal swipes, short swipes, 2-pointer pinch cancellation, edge gesture insets, measured UI exclusion bounds, and zero screen size safety. All 109 unit tests passing cleanly.

## [Unreleased] - Step Z: Gallery Photo Selection, ZoomableBox, Translucent Quick View Overlay & Selective Navigation Bar (2026-11-21)

### Added
- **Step Z: Gallery Photo Selection, ZoomableBox, Translucent Quick View Overlay & Selective Navigation Bar**:
  - `Navigation.kt`: Encoded `initialUri` and `filter` arguments (`Uri.encode`). Extracted `shouldShowBottomBarForRoute` to hide bottom navigation bar on Camera tab (`"camera"`) and show it on secondary tabs (`"studio"`, `"gallery"`, `"vibes"`, `"settings"`).
  - `QuickViewScreen.kt`: Resolved target photo index via `ZoomTransformMath.resolveInitialPageIndex`, initializing `rememberPagerState(initialPage = initialIndex)`. Refactored for edge-to-edge photo rendering with a slim translucent top bar overlay (`CharcoalSurface.copy(alpha = 0.8f)` with `statusBarsPadding()`) toggling on single tap. Handled empty filter lists and deleted photos safely with string resources (`R.string.no_photos_found`).
  - `ZoomableBox.kt`: Created reusable stateless Compose `ZoomableBox` component supporting continuous 1.0x to 8.0x pinch zoom, clamped pan bounds, double-tap 1x/2x scale toggles, live zoom % badge, and 400% zoom Studio brush touch coordinate mapping (`mapScreenPointToImagePixel`).
  - Unit Tests: Added `BugReproTest.kt` (failing-first & resolution tests) and `PhotoZoomAndNavigationTest.kt` (URI encoding with `/`, `:`, `%`, spaces, deleted photo fallbacks, scale/pan clamping, coordinate mapping, bottom bar rules). All 96 unit tests passing.
  - Device Status: Compiled and assembled (`app:assembleDebug`). **NOT VERIFIED on device** (Physical Redmi device proof pending manual user validation).

## [Unreleased] - Step M1i LaMa Model Two-Input Contract Wiring (2026-11-21)

### Added
- **Step M1i LaMa Model Two-Input Contract Wiring**:
  - `assets/manifest.json`: Configured official URL `https://github.com/KanakSaini04/Lensora_Ai/releases/download/models-v1/lama_dilated.tflite` and SHA-256 `a2ac8ed174be1233e4e518c8ef57fb943ea97162696dcd2d428c056280efe583` for 174 MB `lama_dilated.tflite` DLC model entry.
  - `ObjectEraser.kt`: Refactored Object Eraser engine to feed two separate direct `ByteBuffer` inputs: Input 0 `"image"` `[1, 512, 512, 3]` FLOAT32 NHWC (RGB `0.0..1.0`) and Input 1 `"mask"` `[1, 512, 512, 1]` FLOAT32 NHWC (`1.0` = erase, `0.0` = keep), executing via `interpreter.runForMultipleInputsOutputs(arrayOf(imageBuffer, maskBuffer), mapOf(0 to outputBuffer))`.
  - Contract Validation: Added `validateTensorContract` checking tensor shapes at load time, failing safely with a user Toast and logging `Log.e` if shapes do not match.
  - Unit Tests: Added `ObjectEraserTest.kt` unit test suite testing buffer sizes, float normalization, mask polarity, and contract validation. Updated `ManifestParserTest.kt` verifying `lama_dilated` parsing. All 87 unit tests passing cleanly.

## [Unreleased] - Step M1h Real-ESRGAN GPU Delegate Acceleration & CPU Fallback (2026-11-20)

### Added
- **Step M1h Real-ESRGAN GPU Delegate Acceleration**:
  - `RealEsrganEngine.kt`: Added `EsrganInterpreter` wrapper and `createInterpreter(modelFile: File)` using `CompatibilityList().isDelegateSupportedOnThisDevice` and `GpuDelegate()`, with a non-zero test input buffer warm-up inference pass and output validation (`NaN`/`Inf` check, range `[-0.05, 1.05]`, and mean absolute difference $\le 0.02$ vs CPU), safely closing `CompatibilityList`. If CPU warm-up or GPU validation fails, safely closes interpreters and falls back to CPU multi-threading (`setNumThreads(4)`).
  - Telemetry: Added per-20-tile and final tile split timing logs (`Tile $tileIndex/${grid.totalTiles} [fill: ms, inf: ms, convert: ms, draw: ms, avgTotal: ms]`) using `SystemClock.elapsedRealtime()`.
  - Automatic CPU Fallback: Tile-level `try-catch` converts to CPU 4-thread interpreter on GPU failure and closes replacement interpreter in `finally`.
  - `AiProcessorService.kt` & `AiProcessorWorker.kt`: Resolved masked scope declarations for `esrganInterpreterObj`, instantiated interpreter ONLY when `isHighRes` is true, passed `modelFile` and `esrganObj` to pipeline, and ensured single-thread lifecycle without suspension.

## [Unreleased] - Step M1g Real-ESRGAN Tile Overlap (2026-11-20)

### Added
- **Step M1g Real-ESRGAN Tile Overlap**:
  - `RealEsrganEngine.kt`: Implemented 8px tile overlap with context cropping (`TILE_PAD = 8`, `TILE_CORE = 112`, `CROP_OFFSET = 32`, `PaddedSize`, `calculatePaddedSize`, `buildEdgePaddedSource`, and `processTileWithContext`). Input window is $128 \times 128 = 112\text{px core} + 8\text{px context}$ on each side. Center $112 \times 112$ core ($448 \times 448$ output) of each $512 \times 512$ result is cropped at `CROP_OFFSET = 32` to eliminate tile border seams.
  - `RealEsrganEngineTest.kt`: Updated unit tests for 266-tile grid math ($1530 \times 2040$ pre-downscaled input), $1584 \times 2144$ padded source size, partial edge tile core bounds ($74 \times 24 \rightarrow 296 \times 96$ crop), and overlap constants; all 80 unit tests passing.

## [Unreleased] - Step M1f Real AI Models UI Wiring & Device Verification (2026-11-20)

### Added
- **Step M1f Real AI Models UI Wiring & Hardening**:
  - `MoreModesSheet.kt`: Added non-SOON `"High-Res"` tile (`Icons.Outlined.HighQuality`) under Pro Tools. Selecting High-Res activates `"✨ Mode: High-Res"` top pill and highlights carousel mode card in Royal Blue.
  - `AdvancedModeState.kt`: Created pure class delegating mode state and carousel mode reset to prevent sticky mode execution on normal photo captures.
  - `RealEsrganEngine.kt`: Added `padTileTo128` edge pixel replication padding, output tile cropping to `(tileW*4) x (tileH*4)`, pre-allocated ByteBuffer reuse across all 192 tiles (`inputBuffer` 196KB, `outputBuffer` 3.1MB), identity recycling guards (`inTile != scaledSource`), per-tile `ColorMatrix` paint rendering without second 50MP bitmap allocation, pure tile grid math helpers (`calculateTileGrid`, `calculateTileBounds`, `calculateOutputCropDimensions`, `calculateDestCoordinates`), and `try-catch/finally` bitmap recycling on OOM/Throwable.
  - `AiProcessorWorker` & `AiProcessorService`: Added `IS_HIGH_RES` input key scoping for Real-ESRGAN, separate `OutOfMemoryError` handling for 25% scale fallback, `Exception` handling for safe standard pass fallback, and `CancellationException` rethrowing.
  - `SceneLabelMap`: Remapped `skyscraper` class (index 307) to `✨ AI: Auto`. Fixed `loadLabels` cache logic for failed asset reads. Added unit tests reading `categories_places365.txt` asserting 365 lines and index label mappings; 80 unit tests passing.
  - Physical device verification on Redmi Note 13 Pro 5G (user-run, adb logcat, 2026-11-20): High-Res mode capture yielded 6,486,777 byte saved JPEG of $6120 \times 8160$ resolution ($2\times$ net upscale vs source, 192 tiles), finishing in 459,633 ms (~7 min 40 s) with zero `AndroidRuntime` errors.

## [Unreleased] - Step M1 Real AI Models (2026-11-19)

### Added
- **Step M1 Real AI Models**: Wired real TFLite models (`scene_classifier.tflite` Places365 ResNet-18 FP16 and `real_esrgan_v2.tflite` Real-ESRGAN general x4v3 float32) via DLC manifest.
- **Places365 Scene Classifier**: Integrated NCHW Float32 ImageNet normalized input tensor processing, Softmax probability calculation, and 365-class index label mapping (`✨ AI: Landscape`, `✨ AI: Sky`, `✨ AI: Auto`).
- **Real-ESRGAN v2 Super-Resolution**: Built `RealEsrganEngine.kt` for 128x128 -> 512x512 4x tile super-resolution passes with 50% pre-downscale pass targeting ~50 MP output (~200 MB ARGB_8888 in single memory-safe bitmap) and 25% scale OOM fallback yielding 1x net resolution (no upscale).
- **Model Contract Validation**: Updated `ModelContracts` in `ModelContractChecker.kt` (`[1, 3, 224, 224]` NCHW input for Places365, `[1, 128, 128, 3]` -> `[1, 512, 512, 3]` for Real-ESRGAN v2) and dynamic `InterpreterTensorInfoProvider`.
- **SHA-256 Downloader Guard**: `ModelDownloaderWorker` validates expected SHA-256 checksums and automatically deletes corrupted/mismatched temporary model files.

## [Unreleased] - Master Build Phase P4, P5, P6, P7 (2026-11-18)

### Added
- **Task 4.1**: Per-clip speed control in UI (0.25x - 4.0x chips), timeline duration calculation, and ExoPlayer / Media3 `PlaybackParameters` playback speed sync.
- **Task 4.2**: Project Templates Loader from assets (`urban_vlog.json`, `cinematic_story.json`, `square_showcase.json`), template cards row in ProjectsScreen, and PhotoPicker slot auto-fill flow.
- **Task 4.3**: Media3 `RgbFilter` (LEICA grayscale B&W) and `RgbMatrix` (CINEMATIC, VINTAGE, ROYAL_GOLD color matrices) video effect integration into `ExportWorker.kt`.
- **Task 4.4**: Text overlay on clip (`Clip.textOverlay`), live preview badge in lower third, and Media3 `TextOverlay` / `OverlayEffect` rendering pass in `ExportWorker.kt`.
- **Task 4.5**: Per-clip volume & mute control (Mute/50%/100% chips), ExoPlayer volume sync, and audio-aware Media3 export (`setRemoveAudio(true)` when muted).
- **Task 5.1**: Cloud Vault Google Drive setup guide (`docs/DRIVE_SETUP.md`), `EncryptedSharedPreferences` token storage audit, `NetworkType.UNMETERED` Wi-Fi constraint verification, SHA-256 upload safety check before auto-purge, and unit test suite (`CloudVaultTest.kt`).
- **Task 6.1**: Release hardening R8 configuration with `releaseTest` build type and ProGuard rules for Hilt, TFLite GPU Delegate, Media3, WorkManager, Compose, and Serialization.
- **Task 6.2**: Memory leak analysis via `dumpsys meminfo` (Java Heap 20.9 MB, Native Heap 26.3 MB, Total PSS 239.6 MB, zero activity leaks across mode switches).
- **Task 7.1**: Feature-wide Break-It test suite recorded in `report.md` (25+ attacks across Camera, Gallery, Studio, Models, Vibes).
- **Task 7.2**: In-app Privacy Policy and Terms of Service scrollable dialogs in `SettingsScreen.kt` flagged `[DRAFT - LEGAL REVIEW NEEDED]`.

## [Unreleased] - Master Build Phase P1 (2026-11-18)

### Changed
- **Task 1.1**: Removed fake sample generator logic and text video files in `ProjectsScreen.kt`; EmptyProjectsState "Create New Project" button now triggers System PhotoPicker.
- **Task 1.2**: Unified Editor view headers so Projects TopAppBar renders only in list view, and added Delete Project icon button to `EditorScreen.kt` header.
- **Task 1.3**: Configured `DialogProperties(dismissOnClickOutside=false, dismissOnBackPress=false)` for running export dialogs, and added explicit export success/failure alert dialogs.
- **Task 1.4**: Updated `ExportWorker.kt` with composition-level CROP_TO_FILL aspect scaling, photo audio removal, MediaStore error handling & staging cleanup, and WorkManager dataSync foreground service promotion.
- **Task 1.6**: Ensured editor selection safety after clip delete/undo/redo with safe fallbacks, and verified project JSON persistence.
- **Task 2.2**: Implemented `ModelContractChecker.kt` to validate input/output tensor shapes and dtypes against expected model contracts (512x512, 2x super-res) and safely expose Incompatible state.
- **Task 2.3**: Built `TiledPipelineEngine.kt` supporting 1x (LaMa/Dehaze) and 2x (Real-ESRGAN v2) tiled inference with canvas stitching and immediate bitmap recycling.
- **Task 2.4**: Completed Model Supply Chain audit against `docs/MODEL_SUPPLY_PLAN.md`; documented exact BLOCKERS(MODEL) requirements.
- **Task 3.1**: Added BEAUTY camera mode verification in `CameraController.kt` with HAL face detect capability check and safe fallback.
- **Task 3.2**: Added WATERMARK camera mode verification in `CameraController.kt`.
- **Task 3.3**: Added FOOD camera mode verification in `CameraController.kt` with HAL scene mode check and safe fallback.
- **Task 3.4**: Added DOCUMENT camera mode verification in `CameraController.kt` with `EDGE_MODE_HIGH_QUALITY` setting and safe fallback.
- **Task 3.5**: Added TIME-LAPSE camera mode verification in `CameraController.kt`.
- **Task 3.6**: Added SLOW-MOTION camera mode verification in `CameraController.kt` with constrained high-speed video HAL check and safe fallback.
- **Task 3.7**: Added NIGHT VISION camera mode verification in `CameraController.kt` with low-light scene mode HAL check and safe fallback.
- **Task 3.9**: Added ULTRA MACRO camera mode verification in `CameraController.kt` combining 2x crop zoom and minimum focus distance.
- **Task 3.11**: Added DEEP SKY camera mode verification in `CameraController.kt` with max ISO sensitivity and infinity focus.
- **Task 3.12**: Added RAW DNG camera mode verification in `CameraController.kt` with RAW_SENSOR capability HAL check and safe fallback.
- **Task 3.16**: Added DUAL-VIDEO camera mode verification in `CameraController.kt` with concurrent camera HAL check and safe fallback.
- **Task 3.17**: Added HIGH-RES camera mode verification in `CameraController.kt` with ModelFileGuard real_esrgan_v2 check and safe 12.5 MP capture fallback.

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
