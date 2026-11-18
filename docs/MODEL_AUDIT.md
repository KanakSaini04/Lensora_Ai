# Neural AI Model Audit Report - Step 64a

**Audit Date**: 2026-11-18
**Mode**: READ-ONLY Model Audit (Zero production code modifications, zero downloaded model binaries)

---

## 1. Model File Inventory & Matrix

| Model Identifier | File Name | Present in `assets/`? | Present in `filesDir/models/`? | Listed in DLC Manifest? | File Size | Input Tensor Shape / Dtype | Output Tensor Shape / Dtype |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Scene Classifier** | `scene_classifier.tflite` | ❌ No | ❌ No | ❌ No (`mobilenet_v1_1.0_224.tflite` listed) | `NOT VERIFIED` | `NOT VERIFIED` | `NOT VERIFIED` |
| **Object Eraser** | `lama_dilated.tflite` | ❌ No | ❌ No | ⚠️ Yes (`manifest.json`) | `NOT VERIFIED` | `NOT VERIFIED` | `NOT VERIFIED` |
| **AI Super-Res** | `real_esrgan_v2.tflite` | ❌ No | ❌ No | ❌ No | `NOT VERIFIED` | `NOT VERIFIED` | `NOT VERIFIED` |
| **AI Dehaze** | `dehaze.tflite` | ❌ No | ❌ No | ❌ No | `NOT VERIFIED` | `NOT VERIFIED` | `NOT VERIFIED` |

---

## 2. Code Ingestion vs. Tensor Specification Mismatch Audit

### a. `scene_classifier.tflite`
- **Code Reference**: `SceneClassifier.kt:47` (`modelManager?.loadModel("scene_classifier.tflite")`).
- **DLC Manifest Reference**: `assets/manifest.json` specifies `"model_name": "mobilenet_v1_1.0_224.tflite"`.
- **Mismatch**: File name mismatch between runtime loader (`scene_classifier.tflite`) and DLC downloader manifest (`mobilenet_v1_1.0_224.tflite`). `SceneClassifier.kt` currently falls back to color-brightness pixel heuristics on downscaled 224x224 bitmaps.

### b. `lama_dilated.tflite`
- **Code Reference**: `ObjectEraser.kt` & `ModelManager.kt:69`.
- **DLC Manifest Reference**: `assets/manifest.json` lists `"lama_model_name": "lama_dilated.tflite"`.
- **Code Feeding Configuration**: `ObjectEraser.kt` builds a 4-channel float input buffer (`1 x 512 x 512 x 4`, RGB + 1-channel mask) for 512x512 tile regions. Output expects a `1 x 512 x 512 x 3` float tensor.

### c. `real_esrgan_v2.tflite`
- **Code Reference**: `ModelManager.kt:44` & `AiProcessorWorker.kt:80`.
- **DLC Manifest Reference**: Missing in `assets/manifest.json`.
- **Code Feeding Configuration**: Code passes 512x512 input tiles (`1 x 512 x 512 x 3`).
- **Mismatch Note**: Standard 2x super-resolution AI upscale models accept a 512x512 input tile and produce a 1024x1024 output tile (`1 x 1024 x 1024 x 3`). The pipeline must account for tile dimension expansion during image reconstruction.

### d. `dehaze.tflite`
- **Code Reference**: `ModelManager.kt:45`.
- **DLC Manifest Reference**: Missing in `assets/manifest.json`.
- **Code Feeding Configuration**: Code passes 512x512 input tiles (`1 x 512 x 512 x 3`) and expects `1 x 512 x 512 x 3` output.
