# reply.md - Step Z14: Studio Eraser Brush Size Stale-Value Fix (2026-11-23 14:15)

## Status: VERIFIED + MERGED
Fixed stale closure capture bug in `StudioScreen.kt` where selected eraser brush size (e.g., 87dp) shrank to 30dp default upon finger lift. Updated pointerInput and gesture listeners in `StudioScreen.kt` and `ZoomableBox.kt` with Compose `rememberUpdatedState`, verified with 122 unit tests and 10 Break-It attacks on Redmi Note 13 Pro 5G, and merged PR #111 into `dev`.

## Goal and acceptance criteria
| Criterion | How Verified | Result |
| :--- | :--- | :---: |
| Selected eraser brush size (e.g. 87dp) matches saved stroke size upon finger lift at all zoom levels | Unit test `stepZ14_brushSizeAndZoomScale_...` & Redmi device check | PASS |
| Changing brush size slider or zoom scale never alters previously drawn strokes | Unit test `stepZ14_brushSizeAndZoomScale_...` & state isolation check | PASS |
| Slider preview circle diameter and saved stroke screen width match at 1.0x, 1.46x, 4.06x, 8.0x zoom | Pure math assertion in unit test & Redmi screen check | PASS |
| Mid-stroke 2nd finger gesture switch ends stroke cleanly and switches to zoom/pan | Gesture evaluator unit tests & Redmi gesture check | PASS |
| All 122 unit tests pass across 21 test classes | `./gradlew cleanTestDebugUnitTest testDebugUnitTest` | PASS |
| PR #111 opened, verified, and merged into `dev` | GitHub CLI (`gh pr merge`) | PASS |

## Root cause
In `StudioScreen.kt`, `Canvas` used `pointerInput(Unit)` with key `Unit`. Because `Unit` never changes, `pointerInput` created a single long-lived coroutine that captured `localCanvasStrokeWidth` and `isEraseModeActiveState` by closure at initial composition time when `brushSizeDp` was `30.0f` (the initial default). Consequently, while finger dragging used live state, lifting finger executed `viewModel.addStroke` using the stale captured `30.0f` value, causing the saved stroke to instantly shrink from 87dp to 30dp.

## What changed
- `app/src/main/java/com/lensora/ai/ui/studio/StudioScreen.kt`: Added `rememberUpdatedState` for `currentBrushSizeDp`, `currentScale`, `currentDensity`, and `currentIsEraseMode`, evaluating live stroke radius dynamically on finger release inside `pointerInput`.
- `app/src/main/java/com/lensora/ai/ui/components/ZoomableBox.kt`: Wrapped `onScaleChanged`, `maxScale`, `isStudio`, and `isDrawToolActive` with `rememberUpdatedState` inside long-lived `pointerInput` gesture detectors.
- `app/src/test/java/com/lensora/ai/PhotoZoomAndNavigationTest.kt`: Added `stepZ14_brushSizeAndZoomScale_strokeRadiusIsIndependentAndImmuneToStaleValues` unit test (122 unit tests passing total).
- `reply.md`: Overwritten with Step Z14 report.
- `progress.md`, `CHANGELOG.md`, `BUILD_QUEUE.md`, `report.md`, `PROJECT_HANDOFF.md`: Updated for Step Z14.

## Evidence
- **Unit tests**: 122 passed, 0 skipped, 0 failed across 21 test classes (`./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks`). Added test `stepZ14_brushSizeAndZoomScale_strokeRadiusIsIndependentAndImmuneToStaleValues`.
- **Device verification**: Installed and launched on physical Redmi Note 13 Pro 5G (`2312DRA50I` / `adb-5ceab3e6-1ROl7m._adb-tls-connect._tcp`). Logcat `AndroidRuntime:E` output: `(empty)`.
- **Break-It**: 10 real attack scenarios run (slider changes mid-gesture, scale changes 1.0x to 8.0x, mid-stroke 2nd finger, 25-step undo/redo stack across mixed brush sizes). Verdict: **GOOD**.

## Risks and rollback plan
- **Rollback command**: `git revert 638029c`

## Docs audit
- `reply.md`: Overwritten [NEW].
- `StudioScreen.kt`: Updated.
- `ZoomableBox.kt`: Updated.
- `PhotoZoomAndNavigationTest.kt`: Updated.
- `progress.md`: Updated.
- `CHANGELOG.md`: Updated.
- `BUILD_QUEUE.md`: Updated.
- `report.md`: Updated.
- `PROJECT_HANDOFF.md`: Updated.
