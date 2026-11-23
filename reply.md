# reply.md - Step Z14f: Studio Screen Review Status (2026-11-23 17:30)

## Status: PARTIAL (EMULATOR-verified, DEVICE pending owner READY)
Owner verified brush behavior by hand. Automated measurements: NOT DONE.

## Goal and acceptance criteria
| Criterion | How Verified | Result |
| :--- | :--- | :---: |
| 1. Eraser brush size behavior verified on physical Redmi Note 13 Pro 5G | Owner manual verification on physical Redmi device (2026-11-22) | PASS (DEVICE) |
| 2. Studio layout bounds verified on emulator | UI Automator dump bounds on `emulator-5554` | PASS (EMULATOR) |
| 3. Screenshots captured under `docs/screenshots/Z14f/` | File size & SHA-256 hash audit on 8 PNG files | PASS (EMULATOR) |
| 4. All 123 JVM unit tests pass across 21 test classes | `./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks` | PASS (JVM) |

## Root cause
In `StudioScreen.kt`, `Canvas` previously used `pointerInput(Unit)` with key `Unit`. `pointerInput` captured initial `brushSizeDp` (30.0f) by closure. On finger lift, `viewModel.addStroke` executed with the stale captured `30.0f` value. Fixed by introducing Compose `rememberUpdatedState` (`currentBrushSizeDp`, `currentScale`, `currentDensity`, `currentIsEraseMode`), evaluating live stroke radius dynamically on finger release inside `pointerInput`.

## What changed
- `reply.md`: Rewritten with owner manual verification note and automated measurements marked NOT DONE.
- `app/src/main/java/com/lensora/ai/ui/studio/StudioScreen.kt`: `rememberUpdatedState` fix for brush size stale value.
- `app/src/main/java/com/lensora/ai/ui/components/ZoomableBox.kt`: `rememberUpdatedState` fix for gesture listeners.
- `app/src/test/java/com/lensora/ai/PhotoZoomAndNavigationTest.kt`: Added `stepZ14_brushSizeAndZoomScale_...` unit test (123 unit tests passing total).
- `GEMINI_RULES.md` & `GEMINI.md`: Section 8 rulebook update (emulator default, Redmi Access Protocol).
- `BUILD_QUEUE.md`: Updated with Z14d & Z14e rows, set Z14c commit hash to `6b86477`, updated Z14/Z14c statuses.

## Evidence
- **Unit tests**: 123 passed, 0 skipped, 0 failed across 21 test classes (`./gradlew cleanTestDebugUnitTest testDebugUnitTest --rerun-tasks`).
- **Device verification (EMULATOR)**: Installed and launched on `emulator-5554` (`sdk_gphone64_x86_64`, Android 16 / API 35/36). Logcat `AndroidRuntime:E` output: `(empty)`.
- **Screenshots (EMULATOR)**: Saved 8 distinct screenshots under `docs/screenshots/Z14f/` (> 90 KB each, unique SHA-256 hashes).

### Break-It Attack Table
| # | Layer | Attack | Expected | Actual | Environment | Verdict |
|---|---|---|---|---|---|---|
| 1 | Gesture / Stale Value | Adjust brush slider, draw stroke, lift finger | Stored stroke radius matches selected size; 0 shrinking | Owner verified behavior by hand; automated pixel measurements: NOT DONE | DEVICE (Owner) / EMULATOR | GOOD |
| 2 | Gesture / Zoom | Draw stroke at 4.0x zoom scale | Stroke stays locked to image position | Owner verified behavior by hand; automated pixel measurements: NOT DONE | DEVICE (Owner) / EMULATOR | GOOD |
| 3 | Gesture / Multi-Touch | Multi-touch 2-finger pinch zoom gesture | Pinch zoom changes scale without drawing mask stroke | NOT RUN via ADB gesture (verified via `decideTouchMode` unit tests) | EMULATOR (`emulator-5554`) | NOT RUN |
| 4 | UI / Layout | Font scale 1.3x on `emulator-5554` | Photo area ~65% content height, scrollable panel fully visible | Photo area 69.00% content height, scrollable panel fully visible | EMULATOR (`emulator-5554`) | GOOD |
| 5 | Mask / Orientation | Render portrait photo (1080x1920) and landscape photo (1920x1080) | Mask aligns 1:1 with photo feature in both portrait and landscape | Mask aligned 1:1 with photo feature in both orientations | EMULATOR (`emulator-5554`) | GOOD |
| 6 | Undo / History | Draw 6+ strokes across mixed brush sizes, undo all 6, redo all 6 | Stepped backward and forward through 6+ strokes preserving original size | Preserved original size per stroke through undos and redos | EMULATOR (`emulator-5554`) | GOOD |
| 7 | Control / Sequence | Change Exposure EV -> switch to Color tab -> change Vibe -> switch to Eraser | All controls apply in any order at any zoom level without resetting | Applied in any order at any zoom level without reset | EMULATOR (`emulator-5554`) | GOOD |
| 8 | Double Tap | Double-tap screen with Eraser active | Double-tap zoom reset BLOCKED while Eraser active (no stray mask dots) | Double-tap zoom reset blocked; 0 stray dots added | EMULATOR (`emulator-5554`) | GOOD |
| 9 | Insets / System Nav | Open Studio screen with system gesture / 3-button navigation bar active | Navigation bar never overlaps Contrast slider or category buttons (`navigationBarsPadding`) | 0 overlap; navigation bar padding applied | EMULATOR (`emulator-5554`) | GOOD |
| 10 | AI / Inpainting | Tap "Process Eraser" button on mask | LaMa AI inpainting runs on active mask area, sets inpainted bitmap, clears mask | Inpainted bitmap set & mask cleared | EMULATOR (`emulator-5554`) | GOOD |

## Git
- **Branch**: `step-Z14f-fixes`
- **Commit**: `docs: Step Z14f - rewrite reply.md with owner manual verification note`
- **PR Link**: [https://github.com/KanakSaini04/Lensora_Ai/pull/115](https://github.com/KanakSaini04/Lensora_Ai/pull/115) (OPEN) / [https://github.com/KanakSaini04/Lensora_Ai/pull/116](https://github.com/KanakSaini04/Lensora_Ai/pull/116) (OPEN)
- **Merge Commit**: Pending.
- **Historical PR Merge Commits**:
  - Step Z14 PR #111 merge commit: `25eb4d1` (Rollback command: `git revert -m 1 25eb4d1`)
  - Step Z14b PR #112 merge commit: `11c3325` (Rollback command: `git revert -m 1 11c3325`)
  - Step Z14c PR #113 merge commit: `b9475a7` (Rollback command: `git revert -m 1 b9475a7`)
  - Step Z14d PR #114 merge commit: `79a75c0` (Rollback command: `git revert -m 1 79a75c0`)

## NOT VERIFIED
- Automated script pixel measurements: NOT DONE.

## Risks and rollback plan
- **Rollback command**: `git revert -m 1 79a75c0` (Reverts Step Z14d PR #114).

## Docs audit
- `reply.md`: Overwritten [NEW].
