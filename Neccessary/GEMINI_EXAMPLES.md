# GEMINI_EXAMPLES.md

Real Lensora_Ai examples for the rules in `GEMINI.md`. Each shows the mistake, then the right move.

---

## 1. Think before coding

**Task:** "Make the coaching hint better."

**Wrong:** silently rewrites the hint logic, adds new hint types, changes thresholds.

**Right:**
```
"Better" could mean:
1. Stop showing hints without a real subject (the box is a mid-brightness pixel
   cluster that covers nearly the whole frame, so it says "Perfect Composition" everywhere)
2. Add new hint types (needs a decision from you)
3. Make wording/UI nicer

Assumption: 1. Not sure whether a real detector is available offline for this device.
One question: should this step only hide the hint, or add a detector?
```

---

## 2. Simplicity first

**Task:** "Remove the blur from the AI badge."

**Wrong:**
```kotlin
class OverlayStyleManager { fun glass(alpha: Float, border: Color): Modifier { ... } }
// then rewrite every pill to use it
```

**Right:** delete one modifier.
```diff
 Box(
     modifier = modifier
         .clip(CircleShape)
-        .blur(12.dp)
         .background(CharcoalSurface.copy(alpha = 0.65f))
```

---

## 3. Surgical changes

**Task:** fix `AiBadge` in `CameraScreen.kt` (44 KB file).

**Wrong:** reorders imports, reformats `CameraTopBar`, renames `coachingHintText`, deletes an unused function you noticed.

**Right:**
- change only the `AiBadge` modifiers
- remove `import androidx.compose.ui.draw.blur` **only if** nothing else uses it
- in the report: "Noticed: `statusBarsPadding` imported but unused elsewhere. Not touched."

Test: every changed line traces to the task.

---

## 4. Goal-driven execution

**Task:** "AI badge is unreadable."

```
1. Find the cause -> verify: `grep -n "blur(" -r app/src/main` pasted, each hit classified
2. Remove blur from AiBadge -> verify: grep shows no blur on text containers
3. Prove on device -> verify: valid screenshot at font_scale 1.0 and 1.3, text readable
4. No regression -> verify: all unit tests pass, logcat AndroidRuntime:E empty
```

---

## 5. Evidence rules

**Wrong report (this really happened):**
```
- Removed Modifier.blur() from AI badge       <- grep was never run; blur was still there
- Verified 0 crashes on device                <- no logcat attached
- Screenshot: projects.png                    <- was the Camera tab
- clip_1.png, clip_2.webp, clip_3.mp4 (0 B)   <- hand-made, not produced by the importer
```

**Right report:**
```
Changed: CameraScreen.kt (AiBadge: removed .blur(12.dp))
Verified:
  $ grep -n "blur(" -r app/src/main
  (no output)
  $ ./gradlew testDebugUnitTest   -> 21 passed, 0 failed
  $ adb logcat -d -s AndroidRuntime:E -> (empty)
  Screenshots: step62b/camera_1_0.png (212 KB), camera_1_3.png (219 KB), both viewed:
  badge text readable, no overlap with timestamp pill
NOT verified: Projects screen import (phone locked, stopped as instructed)
Problems found (not fixed): SceneClassifier never runs scene_classifier.tflite
Next step: Step 61c device evidence
```

**Screenshot preflight:**
```
adb shell dumpsys power | findstr mWakefulness      # must be Awake
adb shell dumpsys window | findstr mCurrentFocus    # must show com.lensora.ai
```
Locked or wrong app -> STOP and tell me.

---

## 6. Android gotcha: Compose blur

```kotlin
// WRONG: blurs the pill's own background, border AND text
Box(Modifier.clip(CircleShape).blur(12.dp).background(glass)) { Text("AI: Auto") }

// RIGHT: translucent glass, sharp text
Box(Modifier.clip(CircleShape).background(Color(0xFF131313).copy(alpha = 0.65f))
    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)) { Text("AI: Auto") }
```
Real backdrop blur is not what `Modifier.blur` does. Do not claim it is.

---

## Anti-patterns

| Rule | Anti-pattern | Fix |
|---|---|---|
| Think | Guessing a Camera2/HyperOS behavior | Say "not sure", ask, or test |
| Simple | Manager class for one modifier | Delete the modifier |
| Surgical | Reformatting a 44 KB file | Touch only task lines, mention the rest |
| Goal-driven | "Improve overlays" | Grep, fix, screenshot, tests |
| Evidence | Writing "verified" without output | Paste output or write NOT VERIFIED |
