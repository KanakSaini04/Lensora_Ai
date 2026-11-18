# Data Storage & Persistence Schema - Lensora_Ai

## 1. Storage Overview

Lensora_Ai uses three Android system storage mechanisms:
1. **SharedPreferences Tables**: System preferences and user settings.
2. **Android MediaStore SQL Tables**: Media storage for captured photos, uncompressed RAW DNG files, processed AI photos, and MP4 videos.
3. **Internal Storage Files Directory**: Downloaded TFLite AI models (`filesDir/models/`) and temporary `.tmp` atomic capture journals (`cacheDir/`).

---

## 2. SharedPreferences Storage Schemas

### 2.1 Camera Preferences (`lensora_camera_prefs`)
**Storage Context**: Device protected storage fallback via `createDeviceProtectedStorageContext()` for Direct Boot compatibility (`android:directBootAware="true"`).

| Preference Key | Data Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `camera_mode` | `String` | `"PHOTO"` | Currently active camera mode enum name (`AI_AUTO`, `PHOTO`, `PORTRAIT`, `VIDEO`, `PRO`, `CINEMATIC`). |
| `flash_mode` | `String` | `"AUTO"` | Active flash control state (`OFF`, `ON`, `AUTO`). |
| `grid_enabled` | `Boolean` | `false` | Indicates whether the 3x3 Canvas composition grid overlay is enabled. |
| `raw_dng_enabled` | `Boolean` | `false` | Indicates whether uncompressed RAW (DNG) captures are saved in PRO mode. |
| `grain_enabled` | `Boolean` | `false` | Indicates whether vintage film grain overlay is active. |

### 2.2 System Preferences (`lensora_prefs`)

| Preference Key | Data Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `cloud_sync_enabled` | `Boolean` | `false` | Enables background WorkManager Google Drive cloud backup pass upon photo capture. |
| `auto_purge_enabled` | `Boolean` | `true` | Enables automatic local MediaStore master deletion after verified cloud sync upload pass. |
| `mobilenet_model_downloaded` | `Boolean` | `false` | Indicates whether MobileNet v1 1.0 224 DLC model is downloaded and ready in internal storage. |

---

## 3. MediaStore Tables & Columns Schema

### 3.1 MediaStore Images Table (`MediaStore.Images.Media`)

| Column Name | SQL Data Type | Value / Pattern | Description |
| :--- | :--- | :--- | :--- |
| `_id` | `INTEGER PRIMARY KEY` | Auto-increment ID | Unique MediaStore row ID. |
| `DISPLAY_NAME` | `TEXT` | `LENSORA_<timestamp>.jpg`<br>`LENSORA_AI_PROCESSED_<timestamp>.jpg`<br>`LENSORA_EDITED_<timestamp>.jpg` | Final media file name. |
| `MIME_TYPE` | `TEXT` | `"image/jpeg"` | MIME type identifier. |
| `RELATIVE_PATH` | `TEXT` | `"Pictures/Lensora_Ai"` | Output directory in external storage. |
| `IS_PENDING` | `INTEGER` | `1` (writing) $\rightarrow$ `0` (ready) | Atomic write transaction state (Android 10+). |
| `DATE_ADDED` | `INTEGER` | Timestamp (seconds) | Unix epoch addition timestamp used for gallery sorting. |

### 3.2 MediaStore RAW DNG Table (`MediaStore.Images.Media`)

| Column Name | SQL Data Type | Value / Pattern | Description |
| :--- | :--- | :--- | :--- |
| `_id` | `INTEGER PRIMARY KEY` | Auto-increment ID | Unique MediaStore row ID. |
| `DISPLAY_NAME` | `TEXT` | `LENSORA_RAW_<timestamp>.dng` | Uncompressed RAW DNG file name. |
| `MIME_TYPE` | `TEXT` | `"image/x-adobe-dng"` | Adobe DNG MIME type. |
| `RELATIVE_PATH` | `TEXT` | `"Pictures/Lensora_Ai/RAW"` | Dedicated RAW output directory. |
| `IS_PENDING` | `INTEGER` | `0` | Finalized state upon DngCreator write. |

### 3.3 MediaStore Movies Table (`MediaStore.Video.Media`)

| Column Name | SQL Data Type | Value / Pattern | Description |
| :--- | :--- | :--- | :--- |
| `_id` | `INTEGER PRIMARY KEY` | Auto-increment ID | Unique MediaStore row ID. |
| `DISPLAY_NAME` | `TEXT` | `LENSORA_VIDEO_<timestamp>.mp4` | Recorded video file name. |
| `MIME_TYPE` | `TEXT` | `"video/mp4"` | MP4 container MIME type. |
| `RELATIVE_PATH` | `TEXT` | `"Movies/Lensora_Ai"` | Video output directory. |
| `IS_PENDING` | `INTEGER` | `1` $\rightarrow$ `0` | Atomic write transaction state. |

---

## 4. Internal Storage & Journal File Schema

### 4.1 Internal Storage Model Directory (`context.filesDir/models/`)

| File Path | Expected Format | Expected SHA-256 | Description |
| :--- | :--- | :--- | :--- |
| `filesDir/models/mobilenet_v1_1.0_224.tflite` | TFLite Model Binary | Calculated during streaming pass | MobileNet v1 1.0 224 model for L3 TFLite AI inference pass (`1x224x224x3` float input buffer, `1x1001` float output). |
| `filesDir/models/real_esrgan_v2.tflite` | TFLite Model Binary | DLC Download Checksum | Optional DLC model for super-resolution upscale pass. |

### 4.2 Cache Temporary Journal Directory (`context.cacheDir/`)

| File Pattern | Purpose | Lifetime | Cleanup Trigger |
| :--- | :--- | :--- | :--- |
| `cacheDir/LENSORA_<timestamp>.jpg.tmp` | Atomic L0 capture journal | Temporary during capture pass | Deleted immediately after MediaStore stream read completion. |
| `cacheDir/RAW_<timestamp>.dng.tmp` | Atomic RAW DNG capture journal | Temporary during DngCreator pass | Deleted immediately after MediaStore serialization. |
| `cacheDir/LENSORA_VIDEO_<timestamp>.mp4.tmp` | Atomic video recorder journal | Temporary during recording pass | Deleted immediately after MediaStore video finalization. |

### 4.3 Editor Project & Media Directory (`context.filesDir/projects/`)

| File / Folder Pattern | Expected Format | Journaling Strategy | Description |
| :--- | :--- | :--- | :--- |
| `filesDir/projects/<id>.json` | JSON string (`Project` model) | Atomic `.tmp` journal rename | Serialized project metadata storing clips, trims, speed, volume, and music tracks. |
| `filesDir/projects/<id>.json.tmp` | JSON string (`Project.serializer()`) | Temporary write buffer | Written atomically during `saveProject()`. Renamed to `<id>.json` upon write completion. |
| `filesDir/projects/<id>/media/<clipId>.<ext>` | Image (JPEG/PNG/HEIC/WebP) / Video (MP4/MOV/WebM/MKV) | Atomic `.tmp` journal rename | Local project media file copied by `ProjectImporter`. Extension derived via `MimeTypeMap` against safe whitelist (`jpg`, `jpeg`, `png`, `webp`, `heic`, `heif`, `mp4`, `mov`, `webm`, `3gp`, `mkv`). |
| `filesDir/projects/<id>/media/<clipId>.<ext>.tmp` | Binary byte stream | Temporary write buffer | Streamed atomically during media import pass. Renamed to `<clipId>.<ext>` upon byte copy completion. Cleaned up by `sweepOrphanProjects()` if older than 30 minutes. |
