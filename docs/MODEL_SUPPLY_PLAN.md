# Neural Model Supply Chain Audit & Remediation Plan - Step 65

**Audit Date**: 2026-11-18
**Mode**: Step 65 Applied (Manifest Schema v2, Pure JVM Parser, ModelManager Existence Guards, Downloader Checks, and Settings UI Applied)

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
- **Edit Needed**: Add per-model `file_name`, `url`, and `sha256` keys. For models without a verified public host, set `"url": null` and `"sha256": null` (NOT VERIFIED - no confirmed host). Do not invent fake URLs.

```json
{
  "scene_classifier": {
    "file_name": "scene_classifier.tflite",
    "url": null,
    "sha256": null
  },
  "lama_dilated": {
    "file_name": "lama_dilated.tflite",
    "url": null,
    "sha256": null
  },
  "real_esrgan_v2": {
    "file_name": "real_esrgan_v2.tflite",
    "url": null,
    "sha256": null
  },
  "dehaze": {
    "file_name": "dehaze.tflite",
    "url": null,
    "sha256": null
  }
}
```

### 2. `app/src/main/java/com/lensora/ai/ai/SceneClassifier.kt`
- **Line 47**: `modelManager?.loadModel("scene_classifier.tflite")`
- **Edit Needed**: Ensure filename matches DLC manifest target filename. Treat missing `scene_classifier.tflite` as unavailable and fall back to the heuristic pass.

### 3. `app/src/main/java/com/lensora/ai/ai/ModelManager.kt`
- **Lines 44-46**: `loadModel("real_esrgan_v2.tflite")`, `loadModel("dehaze.tflite")`
- **Edit Needed**: Add safety checks for model file existence before attempting Interpreter instantiation.

### 4. `app/src/main/java/com/lensora/ai/ui/settings/SettingsScreen.kt`
- **Lines 105, 120, 443, 532**: Hardcoded model filenames `"mobilenet_v1_1.0_224.tflite"`
- **Edit Needed**: Replace hardcoded string constants with manifest references and display "Model Unavailable" when `"url"` is `null`.

---

## 4. Scene classifier caveat
The ImageNet MobileNet model (`mobilenet_v1_1.0_224.tflite`) classifies general ImageNet objects (1000 categories) and is NOT a scene classification model. Do NOT alias or rename `mobilenet_v1_1.0_224.tflite` to `scene_classifier.tflite`. The loader in `SceneClassifier.kt` must treat missing `scene_classifier.tflite` as unavailable and fall back gracefully to the color-brightness heuristic pass.
