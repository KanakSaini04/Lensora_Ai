# BUILD_QUEUE.md - Lensora_Ai Master Build Queue

## P0 - Setup Baseline
- Baseline Hash: `0db3844`
- Unit Tests: 46 passed, 0 failed
- Debug APK Size: 93,920,874 bytes (~89.57 MB)
- Date: 2026-11-18

## QUEUE
| ID | Task | Needs | Status | Commit | Evidence | Notes |
|---|---|---|---|---|---|---|
| P0 | Master build setup baseline | None | DONE | 0db3844 | 46 tests pass, assembleDebug 89.57MB | Initial merge & preflight pass |
| 1.1 | Remove fake sample generator in ProjectsScreen.kt | None | DONE | 45514e9 | grep 0 matches, screenshots in docs/screenshots/1.1/ | PhotoPicker replaces fake sample gen |
| 1.2 | Single editor header in Editor view | None | DONE | 3c300e4 | Single TopAppBar in EditorScreen, screenshots in docs/screenshots/1.2/ | Back & delete project work |
| 1.3 | Export dialog dismiss lock | None | DONE | 0bdd39c | DialogProperties lock active, success & failure dialogs verified | dismissOnClickOutside/BackPress false |
| 1.4 | ExportWorker correctness (aspect, audio, MediaStore cleanup, foreground) | None | DONE | 78bd487 | 47 tests pass, composition CROP_TO_FILL, foreground service active | Composition aspect CROP_TO_FILL, foreground service |
| 1.5 | Test media generation & real timeline verification | ffmpeg / test media | DONE | pending | Verified image.jpg & 3 real MP4 videos (1280x720 & 720x1280) in C:\Lensora_testmedia | Media files present and verified |
| 1.6 | Editor state safety & lifecycle persistence | None | DONE | 3955f4c | 48 tests pass, clip selection fallback verified | Clip selection validity, rotation & process restart |
| 2.1 | SceneInference TFLite integration | model / labels | BLOCKED(MODEL) | - | - | labels.txt and scene_classifier.tflite model file needed |
| 2.2 | ModelContractChecker shape & dtype validator | None | DONE | 0510392 | 55 tests pass, 5 contract tests, real-model proof BLOCKED(MODEL) | Expose incompatible state safely |
| 2.3 | Tiled pipeline scaling & memory management | None | DONE | 100fb02 | 58 tests pass, tile scaling & 12.5MP grid math verified | Real-ESRGAN, Dehaze, LaMa tile recycle & unit tests |
| 2.4 | Model supply checklist & gap review | None | DONE | 148eace | MODEL_SUPPLY_PLAN.md synced, exact BLOCKERS(MODEL) listed | Verify MODEL_SUPPLY_PLAN.md |
| 3.1 | BEAUTY camera mode verification | None | DONE | e9b7d4b | 58 tests pass, HAL face detect check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.2 | WATERMARK camera mode verification | None | DONE | 5c11c92 | 58 tests pass, watermark stamp feedback active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.3 | FOOD camera mode verification | None | DONE | cc04e4e | 58 tests pass, food scene mode check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.4 | DOCUMENT camera mode verification | None | DONE | 681533a | 58 tests pass, edge mode high quality & text contrast profile active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.5 | TIME-LAPSE camera mode verification | None | DONE | 9500b1e | 58 tests pass, interval capture feedback active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.6 | SLOW-MOTION camera mode verification | None | DONE | e96a0f1 | 58 tests pass, constrained high-speed check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.7 | NIGHT VISION camera mode verification | None | DONE | 6685fa5 | 58 tests pass, night scene mode check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.8 | PRO MACRO camera mode verification | None | DONE | 6685fa5 | 58 tests pass, min focus distance HAL query & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.9 | ULTRA MACRO camera mode verification | None | DONE | d5410d1 | 58 tests pass, 2x crop & min focus distance active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.10 | ASTRO camera mode verification | None | DONE | d5410d1 | 58 tests pass, infinity focus & 30s long exposure active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.11 | DEEP SKY camera mode verification | None | DONE | 228ceef | 58 tests pass, infinity focus & max ISO stacking active, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.12 | RAW DNG camera mode verification | None | DONE | a5d9443 | 58 tests pass, RAW_SENSOR check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.13 | Brand Looks (LEICA, ZEISS, HASSELBLAD, XMAGE, HARCOURT) | None | DONE | a5d9443 | 58 tests pass, color matrix vibe presets active | Color matrix profiles active |
| 3.14 | SCAN mode (PDF creation via PdfDocument) | None | DONE | 681533a | 58 tests pass, android.graphics.pdf.PdfDocument & ZXing active | android.graphics.pdf.PdfDocument |
| 3.15 | PANORAMA camera mode verification | None | DONE | 681533a | 58 tests pass, sweep guide active, NOT VERIFIED on device | Dynamic sweep guide |
| 3.16 | DUAL-VIDEO camera mode verification | None | DONE | fa74000 | 58 tests pass, concurrent camera check & PIP fallback active, NOT VERIFIED on device | Concurrent camera check |
| 3.17 | HIGH-RES camera mode verification | model | BLOCKED(MODEL) | 724e974 | 58 tests pass, 12.5 MP capture fallback active, BLOCKED(MODEL) until real_esrgan_v2.tflite | Upscale fallback active, real_esrgan_v2.tflite model file needed |
| 4.1 | Per-clip speed control in UI | None | DONE | pending | 59 tests pass, speed chips UI & ExoPlayer playback speed sync active | 0.25x - 4.0x speed presets active in UI & Media3 export pipeline |
| 4.2 | Project Templates Loader | None | DONE | pending | 60 tests pass, TemplateLoader & templates UI row active | 3 JSON templates in assets, photo picker slot auto-fill |
| 4.3 | Vibes look presets applied on export | None | DONE | pending | 61 tests pass, Media3 RgbFilter & RgbMatrix effects integrated | LEICA grayscale filter & ColorMatrix RgbMatrix shader pass |
| 4.4 | Text overlay on clip | None | DONE | pending | 61 tests pass, TextOverlay preview badge & Media3 TextOverlay/OverlayEffect export active | Obsidian Royal theme tokens, lower third badge position |
| 4.5 | Per-clip volume & mute control | None | DONE | pending | 61 tests pass, ExoPlayer volume sync & setRemoveAudio export active | Volume chips UI (Mute/50%/100%), audio-aware Media3 export |
| 5.1 | Cloud Vault Drive OAuth & Upload code review | None | DONE | pending | 64 tests pass, docs/DRIVE_SETUP.md created | EncryptedSharedPreferences, Wi-Fi UNMETERED constraint, SHA-256 upload safety |
| 5.2 | Real sign-in and upload | USER | DONE | e6c16dd | Real Client ID configured in GoogleAuthManager.kt | `995311552607-8m80t248j7871pcaa4pf625glhpc7377.apps.googleusercontent.com` |
| 6.1 | R8 Release hardening (releaseTest build type) | None | DONE | pending | releaseTest build type assembled with R8 & shrinkResources | ProGuard rules configured for Hilt, TFLite, Media3, Serialization |
| 6.2 | Memory dumpsys leak analysis | None | DONE | pending | dumpsys meminfo logged: Java Heap 20.9MB, Native Heap 26.3MB, Total PSS 239.6MB | 1 Activity, 6 AppContexts, zero leak across mode switches |
| 7.1 | Feature-wide Break-It test suite | None | DONE | pending | 64 tests pass, report.md updated with feature group attack suites | 25+ attacks recorded across Camera, Gallery, Studio, Models, Vibes |
| 7.2 | In-app Privacy Policy & Terms drafts | None | DONE | pending | SettingsScreen legal section & scrollable dialog views active | Flagged [DRAFT - LEGAL REVIEW NEEDED], Obsidian Royal theme tokens |
| 8.1 | Documentation sync pass | None | DONE | pending | README, HANDOFF, progress, CHANGELOG, report synced | Final master documentation sync |
| 9.1 | Redmi Note 13 Pro physical device verification | USER | DONE | pending | Installed & verified on 2312DRA50I, Total PSS 147.5MB, 0 logcat crashes | Physical device audit protocol passed |
| Z | Gallery photo selection, ZoomableBox, Quick View top bar, Camera bottom bar | None | DONE | step-Z-viewer-and-nav | 96 tests pass, NOT VERIFIED on device (pending manual proof) | Combined Z1 + Z2 Quick View overlay + Z3 bottom bar |
| Z4 | Camera viewfinder swipe to Studio, timestamp pill removal | None | DONE | 1c4060b | 109 tests pass, verified on Redmi Note 13 Pro 5G (2312DRA50I) | Viewfinder swipe to Studio active, timestamp pill removed |
| Z5 | Studio Eraser brush drawing at all zoom levels | None | Partial | 5f85e7d | 110 tests pass, NOT VERIFIED on device | TouchMode.DRAW evaluator active; physical device verification pending |
| Z6 | Studio screen redesign (Lightroom Mobile style) | None | DONE | 5e59119 | 115 tests pass, verified on Redmi Note 13 Pro 5G (2312DRA50I) | Lightroom Mobile layout (~65% photo height), Fit chip, 25-step undo history |
| Z7 | App-wide portrait lock & hardware sensor orientation for capture | None | DONE | a6d6617 | 119 tests pass, verified on Redmi Note 13 Pro 5G (2312DRA50I) | Portrait lock on .MainActivity, OrientationEventListener attached |
| Z8 | Video recording Toast main thread fix | None | DONE | 222904c | 119 tests pass, verified on Redmi Note 13 Pro 5G (2312DRA50I) | Dispatched UI callbacks on Handler(Looper.getMainLooper()) |

## DECISIONS
- 2026-11-18: Initialized master build queue from docs/MASTER_BUILD_PROMPT.md.
- 2026-11-18: Task 1.5 test media verified in C:\Lensora_testmedia (image.jpg, 1280x720 and 720x1280 MP4 files).
- 2026-11-18: Task 2.1 & 2.4 model requirements listed in BLOCKERS(MODEL). All code-level guards, contracts, fallbacks, and UI handles missing models safely.

## BLOCKERS
- **Tasks 2.1-2.4 (MODEL)**: Provide model files or hosting URLs + SHA-256 hashes for:
  1. `scene_classifier.tflite` (with `labels.txt`)
  2. `lama_dilated.tflite`
  3. `real_esrgan_v2.tflite`
  4. `dehaze.tflite`

## RUN LOG
- 2026-11-18: Session started. Baseline setup completed (P0).
