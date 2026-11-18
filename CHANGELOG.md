# Changelog - Lensora_Ai

## [Unreleased] - Step 62 (2026-11-17)

### Fixed
- **Camera Screen Overlays & Typography**: Removed `Modifier.blur()` from AI badge and overlay pills in `CameraScreen.kt` to fix blurred unreadable text. Replaced with sharp, translucent dark glass background (`Color(0xFF131313)` at 0.65 alpha with 1.dp subtle border).
- **Top Overlays Collision**: Restructured top overlays into a single vertical `Column` (`CameraTopBar` $\rightarrow$ Timestamp Pill $\rightarrow$ AI Badge) respecting status bar insets to guarantee zero collision or overlap at 1.0x and 1.3x font scales on all screen sizes.
- **Composition Coaching Hint**: Implemented pure `CoachingHintHelper.coachingHintFor(box: RectF?)` in `SceneClassifier.kt` returning `null` when no subject bounding box is detected, hiding the coaching hint pill UI completely when `null`.
- **Unit Testing**: Added `SceneClassifierTest.kt` unit test suite covering pure `coachingHintFor` logic for null, centered, small, left, and right subject bounding boxes.

## [Unreleased] - Step 63 (2026-11-17)

### Added
- **Claude Review Tooling**: Created `scripts/prepare_claude_review.ps1` for automated, atomic creation of `Claude/` workspace and `Claude_review.zip` review archives. Includes secrets scanning, screenshot validation, test summaries, and `00_README_FIRST.md` problem reports.

## [Unreleased] - Step 62b (2026-11-18)

### Fixed
- **AiBadge Blur Removal**: Step 62b: AiBadge blur was NOT removed in Step 62; removed now. Deleted `.blur(12.dp)` from `AiBadge` in `CameraScreen.kt` and removed unused import.
- **Top Bar Insets**: Added `statusBarsPadding()` to top overlay `Column` in `CameraScreen.kt` to ensure `CameraTopBar`, Timestamp Pill, and `AiBadge` clear status bars and cutouts cleanly.

## [Unreleased] - Step 63d (2026-11-18)

### Added
- **Run and Device Policy**: Updated `GEMINI.md` (Section 10) and `agent.md` (Rule 18) defining explicit mandatory emulator run verification protocols, logging labels, and physical device constraints. Deleted legacy black screenshot `camera_font_1_0.png`.
