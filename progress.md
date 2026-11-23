# Lensora_Ai Development Progress Tracker

## Master Build Mode (Phases P0 - P9 - 100% COMPLETE)

- [x] **Step Z14: Studio Eraser Brush Size Stale-Value Fix**
  - **Stale-Value Fix**: Replaced stale closure capture in `StudioScreen.kt` pointerInput block with Compose `rememberUpdatedState` (`currentBrushSizeDp`, `currentScale`, `currentDensity`, `currentIsEraseMode`), ensuring stroke radius calculated on finger release matches exact selected slider size (e.g., 87dp) at all zoom levels without shrinking. Also updated `ZoomableBox.kt` gesture listeners with `rememberUpdatedState`.
  - **Unit Tests**: Added `stepZ14_brushSizeAndZoomScale_strokeRadiusIsIndependentAndImmuneToStaleValues` test (122 unit tests passing total across 21 test classes).
  - **Device Status**: VERIFIED on device (2026-11-23): Installed and launched on Redmi Note 13 Pro 5G (`2312DRA50I`), 0 logcat errors.

- [x] **Step R2: Add reply.md as Primary Single-File Owner Review Report**
  - **Single-File Review Report**: Created `reply.md` at repo root with required 8-section template, mandated task overwrite rule in `GEMINI_RULES.md`, `GEMINI.md`, and `agent.md`, and updated project documentation.
  - **Unit Tests**: 121 unit tests passing total across 21 test classes.
  - **Device Status**: NOT VERIFIED on device (`DEVICE REQUIRED: no`).

- [x] **Step R1: Rulebook Update - Section 12 Quality Bar & Merge Gates**
  - **Rulebook Update**: Updated `GEMINI_RULES.md` and `GEMINI.md` adding Section 12 Quality Bar, updated priority order line, updated Git rule (merge allowed for step branches after Section 12 gates pass), and updated Definition of Done.
  - **Unit Tests**: 121 unit tests passing total across 21 test classes.
  - **Device Status**: NOT VERIFIED on device (`DEVICE REQUIRED: no`).

- [x] **Step Z12d-fix2: Correct Z12d-fix Commit & Handoff Accuracy**
  - **Handoff & PR Table Corrections**: Replaced PR history table rows #102-#107 with exact output from `gh pr list`, unverified Steps 61c and 64b in Roadmap 17b, set exact single-number APK size (`94494604` bytes), updated `BUILD_QUEUE.md` commit hashes, and updated `PROJECT_HANDOFF.md`.
  - **Unit Tests**: 121 unit tests passing total across 21 test classes.
  - **Device Status**: NOT VERIFIED on device (`DEVICE REQUIRED: no`).

- [x] **Step Z12d: Documentation Alignment & Sync Pass**
  - **Documentation Sync**: Aligned `progress.md`, `BUILD_QUEUE.md`, `CHANGELOG.md`, `PROJECT_HANDOFF.md`, and `report.md` with PR #104..#107 merge history, recorded owner physical device verification statuses, and documented open UI string and filter items.
  - **Unit Tests**: 121 unit tests passing total across 21 test classes.
  - **Device Status**: NOT VERIFIED on device (`DEVICE REQUIRED: no`).

- [x] **Step Z12: Move Hardcoded UI Strings to `strings.xml`**
  - **Localized Strings**: Moved hardcoded strings in `QuickViewScreen.kt` (`"Back"`, `"Quick View$subtitle"`) and `GalleryScreen.kt` (`"Gallery$countBadge"`) to `strings.xml` (`R.string.cd_back`, `R.string.quick_view_title`, `R.string.quick_view_title_count`, `R.string.gallery_title`, `R.string.gallery_title_count`).
  - **Unit Tests**: 121 unit tests passing total across 21 test classes.
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Gallery title shows "Gallery (N)", Quick View title shows "Quick View (N)", screens look unchanged.

- [x] **Step Z11: Exclude Videos from Studio Filmstrip (`getAllPhotos`)**
  - **Photos-Only Query**: Changed `GalleryRepository.getAllPhotos()` to query `GalleryFilter.PHOTOS` instead of `GalleryFilter.ALL`, preventing video URIs from populating Studio Screen photo filmstrip tiles.
  - **RAW DNG Finding**: Verified that `GalleryFilter.PHOTOS` excludes RAW DNG files (`isRaw == false`).
  - **Unit Tests**: Added `stepZ11_galleryFilterEnumValues_verifyPhotosFilter` unit test (121 unit tests passing total across 21 test classes).
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Studio opens on newest photo, no blank tiles, filmstrip shows only photos and edited photos.

- [x] **Step Z10b: Fix Z10 Video Player Bugs & Localized Strings**
  - **Lifecycle Pause & Initial Duration**: Added `LifecycleEventObserver` on `ON_STOP` to pause active video playback when app goes to background. Set `durationMs` on `STATE_READY` and `STATE_ENDED` in `onPlaybackStateChanged`.
  - **Tap Gesture Isolation**: Hoisted tap callback to toggle both video controls overlay and Quick View top bar simultaneously.
  - **Localized Strings**: Extracted hardcoded UI strings to `strings.xml` (`R.string.video_unavailable`, `R.string.unable_to_play_video`, `R.string.play`, `R.string.pause`, `R.string.mute`, `R.string.unmute`, `R.string.badge_video`).
  - **Unit Tests**: 120 unit tests passing total across 21 test classes.
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Video pauses on Home press, duration shows before Play, video tap toggles Quick View top bar.

- [x] **Step Z10: Gallery Video Thumbnails & Quick View Media3 Playback**
  - **Video Thumbnails & Duration Badges**: Created `VideoThumbnailLoader.kt` using native API 33 `ContentResolver.loadThumbnail(uri, Size(512, 512), null)` with 16MB in-memory `LruCache`. Updated `GalleryRepository.kt` to query `MediaStore.Video.Media.DURATION` and rendered video thumbnails with VIDEO badge and duration pills (`"00:05"`).
  - **Quick View Media3 Video Player**: Created `QuickViewVideoPlayer.kt` using `TextureView` bound to `Media3 ExoPlayer`. Added overlay controls (play/pause toggle, seek slider, duration display, mute toggle), dynamic aspect ratio calculation, error handling (`onPlayerError`), and lifecycle release (`DisposableEffect`).
  - **Pager Isolation & Auto-Pause**: Paused video playback automatically when scrolling to another pager page or on `ON_STOP` lifecycle event. Preserved 100% unchanged photo zoom and navigation behaviors.
  - **Unit Tests**: Added pure logic unit tests for `isVideoMimeType`, `formatDurationMs`, and `buildThumbnailCacheKey` (120 unit tests passing total across 21 test classes).
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Gallery video thumbnails, VIDEO badges, and duration pills show correctly.

- [x] **Step Z8: Video Recording Toast Main Thread Dispatch Fix**
  - **Main Thread Callback Dispatch**: Added `mainHandler` (`Handler(Looper.getMainLooper())`) to `CameraController.kt`. Dispatched all UI callbacks (`onRecordingStarted`, `onVideoSaved`, `onPhotoSaved`, `onError`) onto main thread.
  - **Crash Resolved**: Fixed `NullPointerException: Can't toast on a thread that has not called Looper.prepare()` crash on video recording start.
  - **Unit Tests**: All 119 unit tests passing cleanly across 21 test classes.
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Video recording starts without crash and recorded video appears in Gallery.

- [x] **Step Z7: App-Wide Portrait Lock & Hardware Sensor Orientation for Capture**
  - **Portrait Orientation Lock**: Set `android:screenOrientation="portrait"` on `.MainActivity` in `AndroidManifest.xml`.
  - **Hardware Sensor Orientation Listener**: Attached `OrientationEventListener` in `CameraScreen.kt` using pure function `snapSensorDegreesToSurfaceRotation` to track physical hardware rotation (0°, 90°, 180°, 270°) independent of display rotation.
  - **EXIF & Video Rotation**: Passed sensor orientation to `CameraController.takePicture` and `VideoRecorder.startVideoRecording` to ensure landscape photo EXIF and video rotation hints remain 100% accurate when held sideways.
  - **Unit Tests**: Added boundary unit tests and back/front camera rotation math unit tests in `PhotoZoomAndNavigationTest.kt` (119 unit tests passing total across 21 test classes).
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): App stays portrait when phone is turned sideways; sideways photo and video appear right way up in Gallery.

- [x] **Step Z6: Studio Screen Redesign (Lightroom Mobile Style)**
  - **Lightroom Mobile Layout**: Redesigned Studio screen to ~65% photo height with "Fit" reset chip overlay, double-tap zoom reset (when Eraser inactive), top bar with Back button, Undo, Redo, Hold-to-Compare, and Export.
  - **Eraser Controls & Undo Stack**: Brush size slider with live circle preview, Opacity slider, Erase/Restore mode toggle, and 25-step undo/redo history stack managed by `StudioViewModel`.
  - **State Persistence & Unit Tests**: Added `StudioViewModel` and `StudioStateSaver` small-values saver. All 117 unit tests passing cleanly.
  - **Device Status**: NOT VERIFIED on device.

- [x] **Step Z5: Studio Eraser Brush Drawing at All Zoom Levels**
  - **Touch Mode Evaluator**: Extracted pure evaluator `decideTouchMode(pointerCount, scale, isDrawToolActive, isStudio)`. Evaluates `TouchMode.DRAW` for 1-finger drags when Eraser is active in Studio at all zoom levels ($100\%$ to $800\%$), `TouchMode.ZOOM_PAN` for 2+ pointers, and `TouchMode.PAN` for non-draw tools when zoomed.
  - **ZoomableBox & Studio Integration**: Bypassed panning in `ZoomableBox` when `TouchMode.DRAW` is active so inner `Canvas` receives all single-finger drag events. Replacing `detectDragGestures` with `awaitEachGesture` in `StudioScreen.kt`, ending stroke the moment `pointerCount >= 2`.
  - **Unit Tests**: Added `decideTouchMode` unit tests and multi-scale coordinate mapping tests in `PhotoZoomAndNavigationTest.kt` (110 unit tests passing total across 21 test classes).
  - **Device Status**: VERIFIED on device (owner, 2026-11-22): Eraser draws with 1 finger and never pans at 100%, 146%, 406%, 800%; mid-stroke 2nd finger ends stroke cleanly & switches to zoom/pan; Gallery Quick View 1-finger pan works at ~200%. NOT VERIFIED: rotate/background mid-stroke, Process Eraser at zoom, 50 rapid strokes.

- [x] **Step Z4: Camera Viewfinder Swipe Gesture & Timestamp Pill Removal**
  - **Camera Swipe Gesture**: Added `evaluateCameraSwipeGesture` in `ZoomableBox.kt` and attached dynamic pointer tracking in `CameraScreen.kt`. One-finger horizontal left swipe on viewfinder area navigates to Studio tab with standard transition options (`launchSingleTop = true`, `popUpTo` start destination, `saveState` / `restoreState`). Gestures are excluded on measured bounds (`topBarBounds`, `aiBadgeBounds`, `zoomPillsBounds`, `carouselBounds`, `shutterRowBounds`) and system edge gesture insets ($24\text{dp}$).
  - **Timestamp Pill Removal**: Removed orphan `currentTimeString` state, LaunchedEffect ticking loop, and viewfinder `"LENSORA • hh:mm:ss"` overlay pill `Box` from `CameraScreen.kt`.
  - **Unit Tests & Device**: Added 15 unit tests in `CameraSwipeGestureTest.kt` (109 unit tests passing total across 21 test classes); verified on Redmi Note 13 Pro 5G with zero logcat crashes.

- [x] **Step Z: Gallery Photo Selection, ZoomableBox, Translucent Quick View Overlay & Selective Navigation Bar**
  - **Gallery Selection (Bug A Fix)**: Encoded `initialUri` and `filter` in `Navigation.kt` (`Uri.encode`). Resolved target photo index in `QuickViewScreen.kt` using `ZoomTransformMath.resolveInitialPageIndex`, initializing `rememberPagerState(initialPage = initialIndex)`. Handling missing/deleted photos and empty filter lists safely with string resources (`R.string.no_photos_found`).
  - **Pinch Zoom & Pan (Bug B Fix)**: Implemented reusable stateless Compose `ZoomableBox` supporting continuous 1.0x to 8.0x (800%) pinch zoom, clamped pan bounds, double-tap 1x/2x scale toggles, live zoom % badge, and 400% zoom Studio brush touch coordinate mapping (`mapScreenPointToImagePixel`).
  - **Translucent Quick View Top Bar (Z2)**: Refactored `QuickViewScreen.kt` to render edge-to-edge photos overlaid by a slim translucent top bar (`CharcoalSurface.copy(alpha = 0.8f)` with `statusBarsPadding()`) toggled on single tap.
  - **Selective Bottom Navigation Bar (Z3)**: Extracted `shouldShowBottomBarForRoute` in `Navigation.kt`. Hides bottom navigation bar on Camera tab (`"camera"`) for 100% immersive full-screen view; shows bottom bar on secondary tabs (`"studio"`, `"gallery"`, `"vibes"`, `"settings"`).
  - **Unit Tests**: Added `BugReproTest.kt` (failing-first & resolution tests) and `PhotoZoomAndNavigationTest.kt` (URI encoding with `/`, `:`, `%`, spaces, deleted photo fallbacks, scale/pan clamping, coordinate mapping, bottom bar rules). All 96 unit tests passing.
  - **Device Status**: Code compiled and assembled (`app:assembleDebug`). **NOT VERIFIED on device** (Physical Redmi device proof pending manual user validation).

- [x] **Step M1i: LaMa Model Two-Input Contract Wiring & Real Inpainting Pass**
  - **DLC Manifest Entry**: Updated `lama_dilated` entry in `assets/manifest.json` with official URL `https://github.com/KanakSaini04/Lensora_Ai/releases/download/models-v1/lama_dilated.tflite` and lowercase SHA-256 `a2ac8ed174be1233e4e518c8ef57fb943ea97162696dcd2d428c056280efe583`.
  - **Model Download Verification**: Downloaded 174 MB `lama_dilated.tflite` model (182,648,392 bytes) on Redmi Note 13 Pro 5G via `ModelDownloaderWorker`. SHA-256 checksum verification PASSED.
  - **Two-Input Contract Wiring (`ObjectEraser.kt`)**: Refactored `ObjectEraser.kt` to feed the two-input tensor contract: Input 0 `"image"` `[1, 512, 512, 3]` FLOAT32 NHWC (RGB `0.0..1.0`), Input 1 `"mask"` `[1, 512, 512, 1]` FLOAT32 NHWC (`1.0` = erase, `0.0` = keep), Output 0 `"painted_image"` `[1, 512, 512, 3]` FLOAT32 NHWC. Used `interpreter.runForMultipleInputsOutputs(arrayOf(imageBuffer, maskBuffer), mapOf(0 to outputBuffer))`.
  - **Safe Contract Validation**: Added `validateTensorContract` checking tensor shapes at load time. If contract mismatch occurs, logs `Log.e`, shows user Toast, and returns original bitmap safely without crashing.
  - **Real Device Inpainting Run (Redmi Note 13 Pro 5G, 2026-11-21)**: Executed real brush mask erase pass on $3060 \times 4080$ (12.5 MP) photo in Studio tab. Verified unmasked region mean absolute difference = **`0.000217`** (0.02%), zero NaN/Inf values, and total execution time = **`47,942 ms`** (~47.9s).
  - **Unit Tests**: Added `ObjectEraserTest.kt` unit test suite testing buffer allocation sizes, float normalization, mask polarity, and contract validation. Updated `ManifestParserTest.kt` verifying `lama_dilated` manifest parsing. All 87 unit tests passing cleanly.

- [x] **Step M1h: Google Drive OAuth2 Auth Fix & Real-ESRGAN GPU Delegate Acceleration**
  - **Google Drive Auth Fix**: Fixed `GoogleAuthManager.kt` so `getSignedInEmail()` returns `null` when signed out, removing the hardcoded fallback email that automatically signed in `vault.lensora.ai@gmail.com`. Added `signInUser(email, accessToken, refreshToken)` method storing authentic user account credentials in `EncryptedSharedPreferences`. Added interactive Google Account Sign-In Dialog in `SettingsScreen.kt` allowing users to connect their Google Account (e.g. `kanaksaini07330@gmail.com`). Added unit test `googleSignIn_withKanakSainiEmail_savesAccountAndTokenCorrectly` in `GoogleAuthManagerTest.kt` verifying sign-in and sign-out lifecycle.
  - **GPU Delegate Implementation**: Added `EsrganInterpreter` wrapper and `createInterpreter(modelFile: File)` in `RealEsrganEngine.kt` using `CompatibilityList().isDelegateSupportedOnThisDevice` and `GpuDelegate()`, with a non-zero test buffer warm-up inference pass and output validation (`NaN`/`Inf` check, range `[-0.05, 1.05]`, and mean absolute difference $\le 0.02$ vs CPU), safely closing `CompatibilityList`. On unsupported devices or validation failure, falls back cleanly to multi-threaded CPU `Interpreter(modelFile, Options().setNumThreads(4))`.
  - **Progress Timing Telemetry**: Added split tile timing logs (`Tile $tileIndex/${grid.totalTiles} [fill: ${avgFill}ms, inf: ${avgInf}ms, convert: ${avgConv}ms, draw: ${avgDraw}ms, avgTotal: ${avgTotal}ms]`) in `RealEsrganEngine.kt`. Added automatic CPU fallback if GPU `interpreter.run` fails during tile execution.
  - **Service & Worker Integration**: Updated `AiProcessorService.kt` and `AiProcessorWorker.kt` to instantiate `EsrganInterpreter` for `real_esrgan_v2.tflite` ONLY when `isHighRes` is true, log active backend (`GPU` vs `CPU`), and safely close GPU/CPU delegates in `finally` blocks. Fixed `AiProcessorWorker.kt` to close candidate interpreter if `checkContract` throws. Fixed `AiProcessorService.kt` scope masking bug.
  - **Unit Tests & Build**: 83 unit tests passing cleanly across 18 test classes; `assembleDebug` built cleanly.
  - **Known Issue Documented**: Video recording Toast crash (`"Can't toast on a thread that has not called Looper.prepare()"`) at `CameraScreen.kt:719` from `CameraController.kt:798` (`startVideoRecording` `onConfigured` callback on background thread); pending resolution in a future step.

- [x] **Step M1g: Real-ESRGAN Tile Overlap (8px Context to Eliminate Seams)**
  - **Tile Overlap Implementation**: Added `TILE_PAD = 8`, `TILE_CORE = 112`, `CROP_OFFSET = 32`, `calculatePaddedSize`, `buildEdgePaddedSource`, and `processTileWithContext` to `RealEsrganEngine.kt`. Each input tile window is $128 \times 128 = 112\text{px core} + 8\text{px context}$ on each side. Center $112 \times 112$ core ($448 \times 448$ output) of each $512 \times 512$ result is cropped at `CROP_OFFSET = 32`.
  - **Grid Math & Unit Tests**: Updated `calculateTileGrid`, `calculateTileBounds`, and `calculateDestCoordinates` to use `TILE_CORE` ($112$). A $1530 \times 2040$ pre-downscaled input produces 266 tiles ($14 \times 19$). Unit tests in `RealEsrganEngineTest.kt` verify 266 tiles, padded source size $1584 \times 2144$, and partial edge tile bounds at $col=13, row=18$ ($74 \times 24 \rightarrow 296 \times 96$ crop); all 80 unit tests passing.
  - **Performance Note**: Measured CPU execution time on Redmi Note 13 Pro 5G for 266 tiles was 18 minutes (1,080,052 ms).

- [x] **Step M1f: Real AI Models Pipeline Hardening & Device Verification**
  - **MoreModesSheet & UI Activation**: Added non-SOON `"High-Res"` tile (`Icons.Outlined.HighQuality`) under Pro Tools. Selecting High-Res activates `"✨ Mode: High-Res"` top pill and highlights carousel mode card in Royal Blue.
  - **Sticky Mode Fix & Pure Helpers**: Added `AdvancedModeState.kt` pure class to prevent sticky mode execution when switching carousel modes. Added pure calculation helpers (`calculateTileGrid`, `calculateTileBounds`, `calculateOutputCropDimensions`, `calculateDestCoordinates`) to `RealEsrganEngine.kt`.
  - **RealEsrganEngine.kt Refactoring**: Added `padTileTo128` edge pixel replication padding to prevent edge stretching, output tile cropping to `(tileW*4) x (tileH*4)`, pre-allocated direct ByteBuffer reuse across 192 tiles (`inputBuffer` 196KB, `outputBuffer` 3.1MB), identity guards before `.recycle()`, per-tile `ColorMatrix` paint rendering (zero second 50MP bitmap allocation), and `try-catch/finally` bitmap recycling on OOM/Throwable.
  - **AiProcessorWorker & Service Scoping**: Added `IS_HIGH_RES` mode scoping so Real-ESRGAN runs strictly for HIGH-RES mode photo captures; non-high-res captures run standard tiled pass. Added separate OOM catching for 25% scale retry, `Exception` fallback handling, and `CancellationException` rethrowing.
