# Lensora_Ai Comprehensive Architecture & Phase 26 Report

## Executive Summary

**Lensora_Ai** (`com.lensora.ai`) is an AI-first computational camera application built on Android 16 (API 36) using Jetpack Compose, Material 3 (Obsidian Royal Theme), Hilt dependency injection, and a pure **Camera2 L0 Safe Core HAL Engine** (strictly zero CameraX dependencies).

This report presents the completion of **Phase 26: Live AI Director & L2 Hardware Tuning** (Blueprint Section 8). The live viewfinder analysis has been upgraded to run real-time subject detection sampling at 5 FPS (`SceneClassifier.analyzeWithDetection`). The engine calculates normalized subject bounding boxes and dynamically generates composition coaching prompts (`✨ Step Closer`, `✨ Move Right`, `✨ Move Left`, `✨ Perfect Composition`), rendered via an accent cyan (`#00E5FF`) bounding box rectangle and a frosted glass coaching hint pill on `CameraScreen.kt`. Concurrently, the **L2 AI Planner** (`AiPlanner.kt`) dynamically injects automatic Camera2 hardware tuning (`CaptureRequest`) into `CameraController.kt`: locking ISO to minimum (100) and focus to infinity (`0.0f`) for LANDSCAPE, enabling Face AE Lock for PORTRAIT, and boosting exposure compensation (+1.0 EV) for NIGHT.

---

## 1. Architectural Setup: Live AI Director & L2 Auto-Hardware Tuning

- **5 FPS Stream Sampling**: `SceneClassifier.analyzeWithDetection` runs preview frame sampling on `Dispatchers.Default` every 500ms, preserving low thermal overhead.
- **Bounding Box & Composition Coaching Logic**:
  - Calculates normalized bounding box `RectF(normLeft, normTop, normRight, normBottom)`.
  - Calculates subject center $X$ and area ratio.
  - Generates Compose coaching hint:
    - Area ratio $< 0.22 \rightarrow$ `"✨ Step Closer"`
    - Center $X < 0.40 \rightarrow$ `"✨ Move Right"`
    - Center $X > 0.60 \rightarrow$ `"✨ Move Left"`
    - Center in range $\rightarrow$ `"✨ Perfect Composition"`
- **Compose Overlays**:
  - **Bounding Box Overlay**: Cyan (`#00E5FF`) 2.dp rounded rectangle drawn around subject on viewfinder canvas.
  - **Coaching Hint Pill**: Frosted dark surface (`#131313` at 75% opacity) with cyan border and bulb icon.
- **L2 Camera2 Hardware Tuning**:
  - **LANDSCAPE**: Locks ISO to minimum (100), sets `CONTROL_AF_MODE_OFF`, and sets `LENS_FOCUS_DISTANCE = 0.0f` (infinity focus).
  - **PORTRAIT**: Sets `CONTROL_AE_LOCK = true` (Face AE Lock) and `CONTROL_AE_EXPOSURE_COMPENSATION = +0.3 EV`.
  - **NIGHT**: Sets `CONTROL_AE_EXPOSURE_COMPENSATION = +1.0 EV`.

---

## 2. Hardware Capability Probes & Live Audit Results (Xiaomi Redmi Note 13 Pro 5G)

The **Probe Lab** audits camera hardware capabilities at runtime without opening active capture sessions, ensuring 100% crash safety.

| Probe ID | Target Capability | Audit Result | Latency | Key Hardware Characteristics Findings |
| :--- | :--- | :---: | :---: | :--- |
| **P1** | 200MP Full-Res Sensor | `FAIL` | 113 ms | Max resolution exposed via standard Camera2 stream map is **12.5 MP (4080x3060)**. The 200MP sensor mode is binned by default to 12.5MP and full-res raw streams are unexposed by standard Camera2 HAL characteristics. Enforces 12.5MP capture + Real-ESRGAN AI upscale rule. |
| **P2** | RAW Sensor Support | `PASS` | 6 ms | `REQUEST_AVAILABLE_CAPABILITIES_RAW` (`RAW_SENSOR`) is fully supported on Camera ID 0. |
| **P3** | 24fps Cinema Target Rate | `PASS` | 5 ms | Target 24fps cinema capture is supported. Matching AE ranges: `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`. All exposed ranges: `[1, 5]`, `[5, 5]`, `[10, 10]`, `[14, 14]`, `[15, 15]`, `[12, 24]`, `[24, 24]`, `[7, 30]`, `[14, 30]`, `[30, 30]`. |

---

## 3. Phase 26 Live AI Director & L2 Hardware Tuning Audit Matrix

| Component | Target Action | Execution Engine | Safeguard / Fallback | Result |
| :--- | :--- | :--- | :--- | :---: |
| `SceneClassifier.kt` | Bounding Box Detection | `analyzeWithDetection()` | 5 FPS sampling loop on `Dispatchers.Default` | `PASS` |
| `CameraScreen.kt` | Cyan Bounding Box & Coaching Pill | Compose `Canvas` + `Surface` | Hidden if bounding box is null | `PASS` |
| `AiPlanner.kt` | L2 Hardware Plan Generator | `createPlan(sceneType)` | Standard `SceneType.AUTO` fallback | `PASS` |
| `CameraController.kt` | Camera2 Hardware Tuning | `CaptureRequest` repeat session | `try-catch` safety wrapping | `PASS` |

---

## 4. Verification & Logcat Crash Audit

- **Build Status**: `./gradlew assembleDebug` & `./gradlew installDebug` - **100% SUCCESS**.
- **ADB Permission Grants**: Granted `CAMERA`, `RECORD_AUDIO`, and `POST_NOTIFICATIONS` permissions programmatically.
- **Logcat Audit Logs**:
  - `SceneClassifier: Live AI Director Detection: ✨ AI: Night [✨ Step Closer] in 3ms`
  - `AiPlanner: Applied NIGHT plan: EV +1.0, AF Mode 1`
  - `AiPlanner: L2 Hardware Tuning: LANDSCAPE detected (ISO: 100, FocusDist: 0.0, FaceAE: false)`
  - `CameraController: L2 Hardware Tuning: LANDSCAPE detected, locking ISO to 100 (min ISO: 100)`
  - `CameraController: L2 Hardware Tuning: LANDSCAPE detected, locking focus to infinity (0.0f)`
- **Logcat Crash Audit**: `adb logcat -d -s AndroidRuntime:E` - **0 Runtime Crashes (`AndroidRuntime:E`)**.
