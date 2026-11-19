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
| 1.5 | Test media generation & real timeline verification | ffmpeg / test media | BLOCKED(USER) | - | - | Put 2 real videos (1280x720 & 720x1280) and 1 photo in C:\Lensora_testmedia |
| 1.6 | Editor state safety & lifecycle persistence | None | DONE | 3955f4c | 48 tests pass, clip selection fallback verified | Clip selection validity, rotation & process restart |
| 2.1 | SceneInference TFLite integration | model / labels | BLOCKED(MODEL) | - | - | labels.txt and scene_classifier.tflite model file needed |
| 2.2 | ModelContractChecker shape & dtype validator | None | DONE | 0510392 | 55 tests pass, 5 contract tests, real-model proof BLOCKED(MODEL) | Expose incompatible state safely |
| 2.3 | Tiled pipeline scaling & memory management | None | DONE | 100fb02 | 58 tests pass, tile scaling & 12.5MP grid math verified | Real-ESRGAN, Dehaze, LaMa tile recycle & unit tests |
| 2.4 | Model supply checklist & gap review | None | DONE | 148eace | MODEL_SUPPLY_PLAN.md synced, exact BLOCKERS(MODEL) listed | Verify MODEL_SUPPLY_PLAN.md |
| 3.1 | BEAUTY camera mode verification | None | DONE | 74a910f | 58 tests pass, HAL face detect check & safe fallback, NOT VERIFIED on device | Camera2 HAL check & graceful fallback |
| 3.2 | WATERMARK camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.3 | FOOD camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.4 | DOCUMENT camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.5 | TIME-LAPSE camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.6 | SLOW-MOTION camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.7 | NIGHT VISION camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.8 | PRO MACRO camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.9 | ULTRA MACRO camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.10 | ASTRO camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.11 | DEEP SKY camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.12 | RAW DNG camera mode verification | None | TODO | - | - | Camera2 HAL check & graceful fallback |
| 3.13 | Brand Looks (LEICA, ZEISS, HASSELBLAD, XMAGE, HARCOURT) | None | TODO | - | - | Color matrix profiles active |
| 3.14 | SCAN mode (PDF creation via PdfDocument) | None | TODO | - | - | android.graphics.pdf.PdfDocument |
| 3.15 | PANORAMA camera mode verification | None | TODO | - | - | Dynamic sweep guide |
| 3.16 | DUAL-VIDEO camera mode verification | None | TODO | - | - | Concurrent camera check |
| 3.17 | HIGH-RES camera mode verification | model | TODO | - | - | Upscale fallback check |
| 4.1 | Per-clip speed control in UI | None | TODO | - | - | Preview & export duration match |
| 4.2 | Project Templates Loader | None | TODO | - | - | Load templates from assets |
| 4.3 | Vibes look presets applied on export | None | TODO | - | - | Media3 video effects |
| 4.4 | Text overlay on clip | None | TODO | - | - | Position, size, color theme tokens |
| 4.5 | Per-clip volume & mute control | None | TODO | - | - | Audio-aware export |
| 5.1 | Cloud Vault Drive OAuth & Upload code review | None | TODO | - | - | Resumable upload, SHA-256 verify |
| 5.2 | Real sign-in and upload | USER | TODO | - | - | Google account verification |
| 6.1 | R8 Release hardening (releaseTest build type) | None | TODO | - | - | Minify + shrink resources |
| 6.2 | Memory dumpsys leak analysis | None | TODO | - | - | 20 mode switches + 3 exports |
| 7.1 | Feature-wide Break-It test suite | None | TODO | - | - | 25 attacks per feature group |
| 7.2 | In-app Privacy Policy & Terms drafts | None | TODO | - | - | Theme-aware legal scroll views |
| 8.1 | Documentation sync pass | None | TODO | - | - | Final README, HANDOFF, progress sync |
| 9.1 | Redmi Note 13 Pro physical device verification | USER | TODO | - | - | USER-TRIGGERED pass |

## DECISIONS
- 2026-11-18: Initialized master build queue from docs/MASTER_BUILD_PROMPT.md.
- 2026-11-18: Task 1.5 marked BLOCKED(USER) because ffmpeg is not installed on PC. Action required: Put 2 real videos and 1 photo in C:\Lensora_testmedia.
- 2026-11-18: Task 2.1 & 2.4 model requirements listed in BLOCKERS(MODEL). All code-level guards, contracts, fallbacks, and UI handles missing models safely.

## BLOCKERS
- **Task 1.5 (USER)**: Please put 2 real videos (8-10 s, one 1280x720 and one 720x1280 with audio) and 1 photo (JPG) in `C:\Lensora_testmedia`.
- **Tasks 2.1-2.4 (MODEL)**: Provide model files or hosting URLs + SHA-256 hashes for:
  1. `scene_classifier.tflite` (with `labels.txt`)
  2. `lama_dilated.tflite`
  3. `real_esrgan_v2.tflite`
  4. `dehaze.tflite`

## RUN LOG
- 2026-11-18: Session started. Baseline setup completed (P0).
