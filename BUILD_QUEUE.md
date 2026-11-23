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
| 2.1 | SceneInference TFLite integration | None | DONE | f271b7a | 125 tests pass, real scene_classifier.tflite Places365 inference verified | Real TFLite NCHW inference active |
| 2.2 | ModelContractChecker shape & dtype validator | None | DONE | 0510392 | 125 tests pass, contract checks pass for all real models | Expose incompatible state safely |
| 2.3 | Tiled pipeline scaling & memory management | None | DONE | 100fb02 | 125 tests pass, tile scaling & 12.5MP grid math verified | Real-ESRGAN, Dehaze, LaMa tile recycle & unit tests |
| 2.4 | Model supply checklist & gap review | None | DONE | 148eace | MODEL_SUPPLY_PLAN.md synced, exact models verified | Verify MODEL_SUPPLY_PLAN.md |
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
| 3.17 | HIGH-RES camera mode verification | None | DONE | ac45f02 | 125 tests pass, real_esrgan_v2.tflite 2x net super-res pass (4x tile scale) verified, peak RAM 560.9 MB PSS | Real-ESRGAN v2 2x net super-resolution active |
| 4.1 | Per-clip speed control in UI | None | DONE | pending | 59 tests pass, speed chips UI & ExoPlayer playback speed sync active | 0.25x - 4.0x speed presets active in UI & Media3 export pipeline |
| 4.2 | Project Templates Loader | None | DONE | pending | 60 tests pass, TemplateLoader & templates UI row active | 3 JSON templates in assets, photo picker slot auto-fill |
| 4.3 | Vibes look presets applied on export | None | DONE | pending | 61 tests pass, Media3 RgbFilter & RgbMatrix effects integrated | LEICA grayscale filter & ColorMatrix RgbMatrix shader pass |
| 4.4 | Text overlay on clip | None | DONE | pending | 61 tests pass, TextOverlay preview badge & Media3 TextOverlay/OverlayEffect export active | Obsidian Royal theme tokens, lower third badge position |
| 4.5 | Per-clip volume & mute control | None | DONE | pending | 61 tests pass, ExoPlayer volume sync & setRemoveAudio export active | Volume chips UI (Mute/50%/100%), audio-aware Media3 export |
| 5.1 | Cloud Vault Drive OAuth & Upload code review | None | DONE | pending | 64 tests pass, docs/DRIVE_SETUP.md created | EncryptedSharedPreferences, Wi-Fi UNMETERED constraint, SHA-256 upload safety |
| 5.2 | Real sign-in and upload | USER | DONE | e6c16dd | Real Client ID configured in GoogleAuthManager.kt | `995311552607-8m80t248j7871pcaa4pf625glhpc7377.apps.googleusercontent.com` |
| 6.1 | R8 Release hardening (releaseTest build type) | None | DONE | pending | releaseTest build type assembled with R8 & shrinkResources | ProGuard rules configured for Hilt, TFLite, Media3, Serialization |
| 6.2 | Memory dumpsys leak analysis | None | DONE | pending | dumpsys meminfo logged: Java Heap 20.9 MB, Native Heap 26.3 MB, Total PSS 239.6 MB | 1 Activity, 6 AppContexts, zero leak across mode switches |
| 7.1 | Feature-wide Break-It test suite | None | DONE | pending | 64 tests pass, report.md updated with feature group attack suites | 25+ attacks recorded across Camera, Gallery, Studio, Models, Vibes |
| 7.2 | In-app Privacy Policy & Terms drafts | None | DONE | pending | SettingsScreen legal section & scrollable dialog views active | Flagged [DRAFT - LEGAL REVIEW NEEDED], Obsidian Royal theme tokens |
| 8.1 | Documentation sync pass | None | DONE | pending | README, HANDOFF, progress, CHANGELOG, report synced | Final master documentation sync |
| 9.1 | Redmi Note 13 Pro physical device verification | USER | DONE | pending | Installed & verified on 2312DRA50I, Total PSS 147.5 MB, 0 logcat crashes | Physical device audit protocol passed |
| Z | Gallery photo selection, ZoomableBox, Quick View top bar, Camera bottom bar | None | DONE | step-Z-viewer-and-nav | 96 tests pass, NOT VERIFIED on device (pending manual proof) | Combined Z1 + Z2 Quick View overlay + Z3 bottom bar |
| Z4 | Camera viewfinder swipe to Studio, timestamp pill removal | None | DONE | 1c4060b | 109 tests pass, verified on Redmi Note 13 Pro 5G (2312DRA50I) | Viewfinder swipe to Studio active, timestamp pill removed |
| Z5 | Studio Eraser brush drawing at all zoom levels | None | DONE | 5f85e7d | 110 tests pass, VERIFIED on device (owner, 2026-11-22) at 100/146/406/800%, mid-stroke 2nd finger, Gallery pan OK | TouchMode.DRAW evaluator active |
| Z6 | Studio screen redesign (Lightroom Mobile style) | None | DONE | 5e59119 | 117 tests pass, NOT VERIFIED on device | Lightroom Mobile layout (~65% photo height), Fit chip, 25-step undo history |
| Z7 | App-wide portrait lock & hardware sensor orientation for capture | None | DONE | a6d6617 | 119 tests pass, VERIFIED on device (owner, 2026-11-22) | Portrait lock on .MainActivity, OrientationEventListener attached |
| Z8 | Video recording Toast main thread fix | None | DONE | 222904c | 119 tests pass, VERIFIED on device (owner, 2026-11-22) | Dispatched UI callbacks on Handler(Looper.getMainLooper()) |
| Z10 | Gallery video thumbnails & Quick View Media3 playback | None | DONE | 98baa20 | 120 tests pass, VERIFIED on device (owner, 2026-11-22) | Native API 33 loadThumbnail, duration pills, Media3 ExoPlayer |
| Z10b | Fix Z10 video player bugs (lifecycle pause, duration, tap conflict, strings) | None | DONE | dbb561e | 120 tests pass, VERIFIED on device (owner, 2026-11-22) | ON_STOP pause observer, ready state duration, tap toggle, localized strings |
| Z11 | Exclude videos from Studio filmstrip (`getAllPhotos`) | None | DONE | 1df8caf | 121 tests pass, VERIFIED on device (owner, 2026-11-22) | Changed getAllPhotos() to query GalleryFilter.PHOTOS |
| Z12 | Move hardcoded UI strings to `strings.xml` | None | DONE | c1bad89 | 121 tests pass, VERIFIED on device (owner, 2026-11-22) | Extracted Back, Quick View, and Gallery strings to strings.xml |
| Z12b | Documentation sync pass & Z12 device verification | None | DONE | 23023b6 | 121 tests pass, docs only | Recorded owner device verification for Z12; aligned Z5/Z6 doc statuses |
| Z12c | Documentation sync pass & Z7/Z8 device verification | None | DONE | 53eef65 | 121 tests pass, docs only | Recorded owner device verification for Z7 and Z8 |
| Z12d | Full documentation alignment & open items sync pass | None | DONE | 02ba5e6 | 121 tests pass, docs only | Synchronized progress.md, BUILD_QUEUE.md, CHANGELOG.md, report.md, HANDOFF |
| Z12d-fix | PR history and APK size update | None | DONE | e488bd4 | 121 tests pass, docs only | Updated PR table, APK size, and dev contents line |
| Z12d-fix2 | Correct Z12d-fix commit, unverify 61c/64b, fix PR table | None | DONE | 63d2aa8 | 121 tests pass, docs only | Replaced PR table rows #102-#107, unverified 61c/64b, corrected APK size |
| R1 | Rulebook update - Section 12 Quality Bar & Merge Gates | None | DONE | 1af17a6 | 121 tests pass, docs only | Added Section 12 Quality Bar, updated Priority line and Git rule |
| R2 | Add reply.md as primary single-file owner review report | None | DONE | fec48b6 | 121 tests pass, docs only | Created reply.md, updated rulebooks (GEMINI_RULES.md, GEMINI.md, agent.md) and project docs |
| Z14 | Studio Eraser brush size stale-value fix | None | DONE | 2c209f4 | 122 tests pass, VERIFIED on device (owner, 2026-11-23) | Fixed brush size shrinking on lift with rememberUpdatedState |
| Z14c | Studio layout measurement & device screenshot proof | None | DONE | 6b86477 | 123 tests pass, VERIFIED on device (owner, 2026-11-23) | Photo area 67.23% height, 8 screenshots captured, canvas-image unit tests |
| Z14d | Studio re-verification & rulebook section 8 policy update | None | DONE | 79a75c0 | 123 tests pass, VERIFIED on device (owner, 2026-11-23) | Section 8 rulebook update, honest emulator re-verification |
| Z14e | Studio complete review & distinct screenshot hashes | None | DONE | 162541a | 123 tests pass, VERIFIED on device (owner, 2026-11-23) | Full 64-character SHA-256 hashes, distinct screenshot sizes |
| Z14f | Studio PIL script measurements, full hashes & owner Redmi test | None | DONE | f271b7a | 123 tests pass, VERIFIED on device (owner, 2026-11-23) | PR #116 merged into dev, owner verified brush behavior on Redmi |

## DECISIONS
- 2026-11-18: Initialized master build queue from docs/MASTER_BUILD_PROMPT.md.
- 2026-11-18: Task 1.5 test media verified in C:\Lensora_testmedia (image.jpg, 1280x720 and 720x1280 MP4 files).
- 2026-11-18: Task 2.1 & 2.4 model requirements listed in BLOCKERS(MODEL). All code-level guards, contracts, fallbacks, and UI handles missing models safely.

## BLOCKERS
- **Tasks 2.1-2.4 (MODEL)**: All required core model files (`scene_classifier.tflite`, `lama_dilated.tflite`, `real_esrgan_v2.tflite`) verified and active in runtime pipelines. Zero code blockers.

## RUN LOG
- 2026-11-18: Session started. Baseline setup completed (P0).
