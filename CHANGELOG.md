# Changelog - Lensora_Ai

## [Unreleased] - Step 62 (2026-11-17)

### Fixed
- **Camera Screen Overlays & Typography**: Removed `Modifier.blur()` from AI badge and overlay pills in `CameraScreen.kt` to fix blurred unreadable text. Replaced with sharp, translucent dark glass background (`Color(0xFF131313)` at 0.65 alpha with 1.dp subtle border).
- **Top Overlays Collision**: Restructured top overlays into a single vertical `Column` (`CameraTopBar` $\rightarrow$ Timestamp Pill $\rightarrow$ AI Badge) respecting status bar insets to guarantee zero collision or overlap at 1.0x and 1.3x font scales on all screen sizes.
- **Composition Coaching Hint**: Implemented pure `CoachingHintHelper.coachingHintFor(box: RectF?)` in `SceneClassifier.kt` returning `null` when no subject bounding box is detected, hiding the coaching hint pill UI completely when `null`.
- **Unit Testing**: Added `SceneClassifierTest.kt` unit test suite covering pure `coachingHintFor` logic for null, centered, small, left, and right subject bounding boxes.
