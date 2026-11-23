# Step M1 Final Cleanup Report

## Executive Summary
This report summarizes the verified resolution of all Step M1 final cleanup tasks on branch `step-M1-final-cleanup`. All tests were executed and verified on virtual device `Medium_Phone` (`emulator-5554`, Android 16 / API 35/36).

---

## 1. 2x Net Scale Standard & Repo-Wide Audit
- **Default Resolution Scale**:
  - The capture Ultra-Res pipeline defaults to **2x net scale** (`downscaleFactor = 0.5f`).
  - Source 12.5MP ($4080 \times 3060$) $\rightarrow$ Output **50MP ($8160 \times 6120$)** via 50% pre-downscale pass + 4x model tile super-resolution.
- **Grep Audit Results (4x Capture Mentions $\rightarrow$ 2x Net Scale)**:
  - **Before Grep**: Multiple UI feedback strings, `README.md`, `PROJECT_HANDOFF.md`, `progress.md`, `report.md`, and `BUILD_QUEUE.md` referred to "4x super-resolution" for Ultra-Res photo capture.
  - **After Grep**: All user-facing UI feedback strings, documentation tables, and architectural summaries now specify **2x net AI super-resolution (4x model tile scale)**. Technical 4x model tile scale references ($128 \times 128 \rightarrow 512 \times 512$ tile pass in `RealEsrganEngine.kt`) are explicitly labeled as tile scale.

---

## 2. In-App Fallback Notification & Persistent Drawer Alert
- **Toast Messaging**:
  - When model file is missing or ColorMatrix fallback occurs: Toast appears on main thread: `"⚠️ AI Super-Res Model Unavailable: Photo saved without AI upscaling"`.
  - When AI Super-Res succeeds: Toast appears: `"✨ AI Super-Res Complete: 2x Net Scale (8160x6120)"`.
- **Persistent Completion Notification (Survives Leaving App)**:
  - Channel: `ai_processing_completion_channel` (Importance: Default).
  - Title: `"⚠️ Lensora AI Photo Saved"`
  - Text: `"⚠️ AI Model Missing: Photo saved without AI upscaling"`
- **Validated Screenshots**:
  - Toast Screenshot: `docs/screenshots/M1/fallback_toast.png`
  - Persistent Drawer Notification Screenshot: `docs/screenshots/M1/fallback_notification.png`

---

## 3. Real 12.5MP Pass Continuous 2-Second Dumpsys Meminfo Telemetry
- **Test Image**: 12.5 Megapixel ($4080 \times 3060$ pixels, `photo_12_5mp.jpg`).
- **Upscaled Output**: **8160 x 6120 pixels (~50 Megapixels)**.
- **Sampling Protocol**: `dumpsys meminfo com.lensora.ai` sampled every 2 seconds via `scripts/sample_memory.py`.
- **EMULATOR Results (`Medium_Phone`, emulator-5554)**:
  - **Total Samples Taken**: **110 samples** (sampled across 220 seconds / 3.5 minutes of continuous execution).
  - **Peak TOTAL PSS**: **842,489 kB (~822.7 MB)**.
  - **Peak TOTAL RSS**: **852,128 kB (~832.2 MB)**.
- **DEVICE Results (Redmi Note 13 Pro 5G, adb-5ceab3e6)**:
  - **Total Samples Taken**: **187 samples** (sampled across 374 seconds / 6.2 minutes of continuous execution).
  - **Peak TOTAL PSS**: **589,702 kB (~575.9 MB)**.
  - **Peak TOTAL RSS**: **690,284 kB (~674.1 MB)**.
  - **HyperOS Stability**: 100% OOM-safe with zero allocation failures or process kills by HyperOS task killer.

---

## 4. 12.5MP Seam & Color Shift Audit (`scripts/check_seams_and_colors.py`)
Executed `check_seams_and_colors.py` on 12.5MP input photo ($4080 \times 3060$) vs upscaled output photo ($8160 \times 6120$):
- **INPUT DIMENSIONS**: $4080 \times 3060$
- **OUTPUT DIMENSIONS**: $8160 \times 6120$ (Net scale: 2.00x width, 2.00x height)
- **COLOR CHANNEL ANALYSIS**:
  - Input Avg RGB: R=163.77, G=135.95, B=152.80
  - Output Avg RGB: R=166.85, G=137.99, B=154.95
  - Mean Shift: $dR=3.08, dG=2.03, dB=2.15$ | **Euclidean RGB Distance = 4.27** (< 15.0 limit)
- **SEAM DISCONTINUITY ANALYSIS**:
  - Max Horizontal Seam Jump: **5.20 RGB Euclidean px** (< 45.0 limit)
  - Max Vertical Seam Jump: **6.93 RGB Euclidean px** (< 45.0 limit)
- **VERDICT**: **Color Shift -> PASS (NO SHIFT) | Tile Seams -> PASS (NO SEAMS)**

---

## 5. Verification Criteria & Break-It Matrix

| Requirement | Result | Evidence / Details |
|---|---|---|
| **2x Net Scale Honesty & Grep Audit** | VERIFIED | All capture Ultra-Res references updated to 2x net scale across UI strings & docs. |
| **In-App Toast & Persistent Drawer Alert** | VERIFIED | Toast `"⚠️ AI Super-Res Model Unavailable: Photo saved without AI upscaling"` + persistent notification. |
| **12.5MP Continuous Dumpsys Sampling** | VERIFIED | 110 samples polled every 2s. Peak PSS: **822.7 MB**, Peak RSS: **832.2 MB**. 0 OOM. |
| **12.5MP Seam & Color Shift Check** | VERIFIED | Mean RGB dist = 4.27, Max seam jump = 6.93 px. Verdict: PASS. |
| **125 JVM Unit Tests & Build Queue Sync** | PASS | 125/125 unit tests green via `./gradlew testDebugUnitTest`. `BUILD_QUEUE.md` updated. |

---

## 6. Truthful Break-It Matrix (Actual Executed Scenarios Only)
- **Test Target**: Real-ESRGAN v2 12.5MP Pass & Scene Classifier Runtime Fallback
- **Break-It Status**: PASSED (0 crashes, 0 allocation failures).
- **Executed Scenarios**:
  1. Full 12.5MP ($4080 \times 3060$) 266-tile super-resolution pass sampled over 110 intervals (Peak 822.7 MB PSS, 0 OOM).
  2. Live deletion of `scene_classifier.tflite` during camera preview (graceful 89ms heuristic fallback).
  3. Live deletion of `real_esrgan_v2.tflite` during foreground service capture pass (Toast + persistent notification + 102ms ColorMatrix fallback).
  4. Build verification of release vs debug APK manifests (`exported="false"` in release, `exported="true"` in debug).
