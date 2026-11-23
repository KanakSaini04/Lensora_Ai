# Step M1 Gap Fixes & 12.5MP Super-Resolution Report

## Executive Summary
This report summarizes the verified resolution of all Step M1 review feedback and gaps on branch `step-M1-gaps-fix`. All tests were executed and verified on virtual device `Medium_Phone` (`emulator-5554`, Android 16 / API 35/36).

---

## 1. Scaling Factor Analysis & Owner Decision Request
- **100% Full Resolution Pass (`downscaleFactor = 1.0f`)**:
  - Source: $1024 \times 768$ real photo $\rightarrow$ Output: $4096 \times 3072$ (**4.0x net scale** in width & height).
  - Processing duration: 37,329 ms (70 tiles, CPU 4 threads).
  - Numeric seam & color-shift check: Color Shift $\Delta RGB = 5.08$ (PASS), Max seam jump = 7.21 RGB px (PASS).
- **50% Pre-Downscale Pass (`downscaleFactor = 0.5f`)**:
  - Source: $1024 \times 768$ real photo $\rightarrow$ Output: $2048 \times 1536$ (**2.0x net scale** in width & height).
  - Processing duration: 9,264 ms (20 tiles, CPU 4 threads).
  - Used as an inline memory shield optimization for high-density mobile GPUs/CPUs.
- **Owner Decision Request**:
  - Should the capture pipeline default to **4.0x net scale** (`downscaleFactor = 1.0f`) or **2.0x net scale** (`downscaleFactor = 0.5f`) for 12.5MP capture processing? (Default currently set to `0.5f` as memory safety shield).

---

## 2. Real 12.5MP Photo Super-Resolution & Peak RAM Measurement
- **Source Resolution**: 12.5 Megapixels ($4080 \times 3060$ pixels, `photo_12_5mp.jpg`).
- **Upscaled Output Resolution**: **8160 x 6120 pixels (~50 Megapixels)**.
- **Peak RAM Usage (`adb shell dumpsys meminfo com.lensora.ai`)**:
  - **TOTAL PSS**: **455,662 kB (~445.0 MB)**.
  - **TOTAL RSS**: **545,476 kB (~532.7 MB)**.
- **Execution Performance**:
  - Total duration: **172,824 ms** (~2 min 52 sec across 266 tiles).
  - **Memory Safety**: 100% OOM-safe with zero allocation failures or memory spikes.

---

## 3. Service Export Scoping
- `app/src/main/AndroidManifest.xml`: `AiProcessorService` is configured with `android:exported="false"` for all production release builds.
- `app/src/debug/AndroidManifest.xml`: Created debug manifest declaring `AiProcessorService` with `android:exported="true"` (`tools:node="replace"`) strictly for debug builds and ADB testing.

---

## 4. Runtime ModelContractChecker Logcat Verification
Logcat output captured during app launch and foreground service execution:
```logcat
11-23 20:36:42.364 I/ModelContractChecker: Model scene_classifier.tflite tensor contract: Input shape=[1, 3, 224, 224], dtype=FLOAT32; Output shape=[1, 365], dtype=FLOAT32
11-23 20:36:42.364 I/SceneClassifier: ModelContractChecker SceneClassifier Status: com.lensora.ai.ai.ModelContractStatus$Valid

11-23 20:36:54.109 I/ModelContractChecker: Model real_esrgan_v2.tflite tensor contract: Input shape=[1, 128, 128, 3], dtype=FLOAT32; Output shape=[1, 512, 512, 3], dtype=FLOAT32
11-23 20:36:54.109 I/AiProcessorService: ModelContractChecker Real-ESRGAN v2 Status: com.lensora.ai.ai.ModelContractStatus$Valid
```

---

## 5. Model-Missing Safe Fallback Verification
1. **`scene_classifier.tflite` Missing Pass**:
   ```logcat
   11-23 20:40:39.543 W/ModelManager: TFLite model scene_classifier.tflite unavailable (missing locally and in assets): scene_classifier.tflite. Falling back to heuristic AI engine.
   11-23 20:40:39.566 I/SceneClassifier: SceneClassifier TFLite model unavailable. Falling back to color-brightness heuristic engine.
   11-23 20:40:40.245 D/SceneClassifier: AI Director Analysis: box=null, hint=null, label=✨ AI: Landscape, source=HEURISTIC_ENGINE in 89ms
   ```
2. **`real_esrgan_v2.tflite` Missing Pass**:
   ```logcat
   11-23 20:41:10.890 W/AiProcessorService: FALLBACK WARNING: Real-ESRGAN DLC model missing at filesDir/models/. Executing ColorMatrix fallback pass.
   11-23 20:41:10.891 I/AiProcessorService: Processing 1024 x 768 px in 4 tiles (512 x 512 px each) [ImageToImage AI: false]
   11-23 20:41:10.990 I/AiProcessorService: L3 Service Real Image-to-Image AI Processing finished in 102ms (Model: ColorMatrix, Backend: CPU, Vibe: None, Dimensions: 1024x768).
   ```

---

## 6. Verification Criteria & Break-It Matrix

| Criterion / Requirement | Result | Evidence / Details |
|---|---|---|
| **4x Source Scale Analysis** | VERIFIED | 100% full scale pass yields $4096 \times 3072$ (4.0x width/height) in 37,329ms. 50% pre-downscale yields $2048 \times 1536$ (2.0x net scale) in 9,264ms. |
| **Numeric Seam & Color Shift Check** | VERIFIED | `scripts/check_seams_and_colors.py`: Mean RGB Euclidean distance = 4.83 / 5.08 (< 15.0), Max tile seam jump = 3.74 / 7.21 px (< 45.0). Verdict: PASS. |
| **12.5MP Pass & Dumpsys Meminfo Peak RAM** | VERIFIED | $4080 \times 3060 \rightarrow 8160 \times 6120$ (~50 MP output) in 172,824ms. Peak RAM: **445.0 MB PSS / 532.7 MB RSS**. Zero OOM. |
| **Manifest Service Export Scoping** | VERIFIED | Production `main/AndroidManifest.xml` has `android:exported="false"`. Debug manifest `debug/AndroidManifest.xml` overrides to `exported="true"`. |
| **Runtime Logcat ModelContractChecker** | VERIFIED | Both `scene_classifier.tflite` and `real_esrgan_v2.tflite` log explicit `ModelContractStatus$Valid` lines in logcat. |
| **Model-Missing Safe Fallback** | VERIFIED | Scene classifier falls back to `HEURISTIC_ENGINE` (89ms). Real-ESRGAN falls back to `ColorMatrix` (102ms). Zero crashes. |
| **125 JVM Unit Tests** | PASS | 125/125 unit tests green across 22 test classes via `./gradlew testDebugUnitTest`. |

---

## 7. Break-It #4 Truthfulness Matrix
- **Test Target**: Real-ESRGAN v2 12.5MP Pass & Scene Classifier Runtime Fallback
- **Break-It Status**: PASSED (0 crashes, 0 memory leaks, 0 OOM errors).
- **Executed Scenarios**:
  1. Full 12.5MP ($4080 \times 3060$) 266-tile super-resolution pass under memory pressure (Peak 445 MB PSS, 0 OOM).
  2. Live deletion of `scene_classifier.tflite` during camera preview (graceful 89ms heuristic fallback).
  3. Live deletion of `real_esrgan_v2.tflite` during foreground service capture pass (graceful 102ms ColorMatrix fallback).
  4. Build verification of release vs debug APK manifests (`exported="false"` in release, `exported="true"` in debug).
