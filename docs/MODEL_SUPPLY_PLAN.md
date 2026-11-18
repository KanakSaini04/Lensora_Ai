# Neural Model Supply Chain Audit & Remediation Plan - Step 64b

**Audit Date**: 2026-11-18
**Mode**: READ-ONLY Model Supply Chain Plan (No production code modifications applied)

---

## 1. Supply Chain Architecture Overview

```
┌────────────────────────────────────────────────────────┐
│ DLC Asset Manifest (app/src/main/assets/manifest.json) │
└───────────────────────────┬────────────────────────────┘
                            │ Download URLs & Target Filenames
                            ▼
┌────────────────────────────────────────────────────────┐
│ ModelDownloaderWorker (WorkManager, HTTP Zip Extract)  │
└───────────────────────────┬────────────────────────────┘
                            │ Saves to app filesDir
                            ▼
┌────────────────────────────────────────────────────────┐
│ Target Directory: context.filesDir/models/<filename>  │
└───────────────────────────┬────────────────────────────┘
                            │ File Stream Ingestion
                            ▼
┌────────────────────────────────────────────────────────┐
│ Runtime AI Loaders (ModelManager, SceneClassifier, etc)│
└───────────────────────────┬────────────────────────────┘
```

---

## 2. Supply Chain Mismatch Analysis Table

| Model Target | Manifest Key (`assets/manifest.json`) | Downloaded Filename | Runtime Loader Expectation | Mismatch Status |
| :--- | :--- | :--- | :--- | :--- |
| **Scene Classifier** | `model_name` | `mobilenet_v1_1.0_224.tflite` | `scene_classifier.tflite` (`SceneClassifier.kt:47`) | ❌ **NAME MISMATCH** (Loader expects `scene_classifier.tflite`) |
| **Object Eraser** | `lama_model_name` | `lama_dilated.tflite` | `lama_dilated.tflite` (`ObjectEraser.kt:29`) | ✅ **MATCH** |
| **AI Super-Res** | *Unlisted* | *Missing* | `real_esrgan_v2.tflite` (`ModelManager.kt:44`) | ❌ **MISSING FROM MANIFEST** |
| **AI Dehaze** | *Unlisted* | *Missing* | `dehaze.tflite` (`ModelManager.kt:45`) | ❌ **MISSING FROM MANIFEST** |

---

## 3. Required File-by-File Edits (NOT APPLIED)

### 1. `app/src/main/assets/manifest.json`
- **Edit Needed**: Add entries for `real_esrgan_v2.tflite` and `dehaze.tflite` DLC download endpoints, and align `model_name` to `"scene_classifier.tflite"`.

```json
{
  "model_name": "scene_classifier.tflite",
  "url": "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224.zip",
  "lama_model_name": "lama_dilated.tflite",
  "lama_url": "https://storage.googleapis.com/download.tensorflow.org/models/tflite/lama_dilated.zip",
  "esrgan_model_name": "real_esrgan_v2.tflite",
  "esrgan_url": "https://storage.googleapis.com/download.tensorflow.org/models/tflite/real_esrgan_v2.zip",
  "dehaze_model_name": "dehaze.tflite",
  "dehaze_url": "https://storage.googleapis.com/download.tensorflow.org/models/tflite/dehaze.zip"
}
```

### 2. `app/src/main/java/com/lensora/ai/ai/SceneClassifier.kt`
- **Line 47**: `modelManager?.loadModel("scene_classifier.tflite")`
- **Edit Needed**: Ensure filename matches DLC manifest target filename.

### 3. `app/src/main/java/com/lensora/ai/ai/ModelManager.kt`
- **Lines 44-46**: `loadModel("real_esrgan_v2.tflite")`, `loadModel("dehaze.tflite")`
- **Edit Needed**: Add safety checks for model file existence before attempting Interpreter instantiation.

### 4. `app/src/main/java/com/lensora/ai/ui/settings/SettingsScreen.kt`
- **Lines 105, 120, 443, 532**: Hardcoded model filenames `"mobilenet_v1_1.0_224.tflite"`
- **Edit Needed**: Replace hardcoded string constants with manifest references.
