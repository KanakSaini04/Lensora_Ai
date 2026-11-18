# Changelog - Lensora_Ai

## [Unreleased] - Step 65 (2026-11-18)

### Changed
- **Model Manifest Hardening (v2)**: Updated `app/src/main/assets/manifest.json` schema v2 with keys `scene_classifier`, `lama_dilated`, `real_esrgan_v2`, `dehaze` (`url: null`, `sha256: null`).
- **Pure JVM Manifest Parser**: Implemented pure Kotlin `ManifestParser.kt` and `ModelEntry` with safe JSON parsing and null handling.
- **Model Manager File Guards**: Updated `ModelManager.loadModel()` to check local model file existence, `length > 0`, and `canRead()` before TFLite Interpreter creation.
- **Downloader Checksum & Skip**: Updated `ModelDownloaderWorker.kt` to skip download when `url == null` or unconfigured, and verify streaming SHA-256 hashes when provided.
- **Scene Classifier Fallback**: Updated `SceneClassifier.kt` to fall back gracefully to color-brightness heuristics when model file is unavailable.
- **Model Unavailable UI**: Updated `SettingsScreen.kt` to reflect manifest entries and display "Model Unavailable" disabled button state when models are unhosted and missing locally.
- **Unit Testing**: Created `ManifestParserTest.kt` unit test suite (6 new pure JVM unit tests for parser, existence guards, and SHA-256 verification, bringing total unit test suite to 27/27 passed across 7 test classes).

## [Unreleased] - Step 65b (2026-11-18)

### Added
- **Model Supply Chain Plan**: Created `docs/MODEL_SUPPLY_PLAN.md` mapping DLC manifest endpoints, WorkManager download destinations, and exact runtime loader file expectation mismatches across `SceneClassifier.kt`, `ModelManager.kt`, and `SettingsScreen.kt`.

## [Unreleased] - Step 64a (2026-11-18)

### Added
- **Neural Model Audit**: Created `docs/MODEL_AUDIT.md` performing a read-only audit of offline neural models (`scene_classifier.tflite`, `lama_dilated.tflite`, `real_esrgan_v2.tflite`, `dehaze.tflite`), mapping asset presence, DLC manifest definitions, tile input tensor dimensions, and super-resolution output expansion notes.
