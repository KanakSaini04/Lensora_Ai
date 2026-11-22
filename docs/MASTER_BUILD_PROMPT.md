# MASTER_BUILD_PROMPT.md - Lensora_Ai: build everything, resumable

FIRST: read `GEMINI.md` (v2), `agent.md`, `PROJECT_HANDOFF.md` fully and obey them.
This file is the standing order. It is saved in the repo as `docs/MASTER_BUILD_PROMPT.md`. Any AI can resume the work from this file plus `BUILD_QUEUE.md`.

## 0. How this works (read twice)
- The user pastes this prompt ONCE. After that, a new session only needs: **"Read docs/MASTER_BUILD_PROMPT.md and BUILD_QUEUE.md, then CONTINUE."**
- You work through the queue in section 6 **without asking me anything**. You may be cut off at any time (quota, context, crash). So: small tasks, state saved in git after every task, never rely on memory of the chat.
- **MASTER BUILD MODE overrides these rules ONLY:** "one task per prompt", "one branch per step", "wait for plan OK", "separate PR per step", "docs sync after every step" (see sections 3 and 4). It does NOT override Safety (GEMINI.md sec. 2), Evidence (sec. 5), project locks (sec. 0), or Run/device policy (sec. 8).
- Never stop to ask a question. When a rule clashes or a decision is open: choose the most conservative option, write it into `BUILD_QUEUE.md` under DECISIONS (one line, with the reason), and continue. If you cannot continue a task safely: mark it BLOCKED (section 5) and go to the next task.
- Honesty beats progress. A task you cannot prove is NOT VERIFIED or Partial, never Done. Never fake evidence, never fake a feature (no fake sample data in production code).

## 1. Session start (every session)
1. `git status`, `git branch --show-current`, `git log -5 --oneline`. Paste nothing long into chat; write command output to `build_logs/<name>.txt` (this folder is git-ignored: add `build_logs/` to `.gitignore` once) and quote only the key lines.
2. If `GEMINI.md` does not start with `# GEMINI.md v2` but `GEMINI_RULES.md` does, copy `GEMINI_RULES.md` over `GEMINI.md` (docs commit) and keep `GEMINI_RULES.md` as it is. If `master_prompt.md` exists and conflicts with GEMINI.md, GEMINI.md wins; log the conflict in DECISIONS.
3. If `BUILD_QUEUE.md` does not exist, create it from section 6 (format in section 4) and do section 2 (setup). If it exists, resume: find rows marked IN_PROGRESS. Inspect `git status`/diff: finish the task if it is nearly done and builds, otherwise `git restore` the unfinished work and redo it. Log what you did.
4. Pick the first TODO task whose needs are satisfied. Never skip ahead because a task looks more fun.

## 2. Setup (P0, once)
- Branch: from latest `dev` create `build/all-features`. Then `git merge --no-ff origin/step-67-export` (this brings the open PRs #100 and #101: timeline, trim/split/reorder/delete, undo/redo, Media3 export). If the merge conflicts in code, resolve conservatively keeping both sides' intent, or mark P0 BLOCKED and tell me.
- Baseline: fresh `.\gradlew.bat cleanTestDebugUnitTest testDebugUnitTest` and `assembleDebug`. Record hash, test count, APK size in `BUILD_QUEUE.md` and `PROJECT_HANDOFF.md` section 0.
- Save this prompt exactly as `docs/MASTER_BUILD_PROMPT.md` (if it is not there). Create `BUILD_QUEUE.md`. Commit: `docs(build): Step P0 - master build setup`.
- Open ONE draft PR `build/all-features` -> `dev` titled "Lensora_Ai full build (rolling)". Keep updating it by pushing. **Never merge it.**
- Emulator preflight: `adb devices`. If `emulator-5554` is online, unlock if needed (the user may give the PIN in the chat message that pasted this prompt, as "EMULATOR PIN: ...". Use it only inside adb commands, NEVER write it to any file, log, commit or PR), take a screenshot, validate it (> 100 KB, not black). If the emulator is off or black: do NOT start or reconfigure it; run only tasks that need no emulator (unit tests, docs, pure logic) and mark the others "NEEDS-EMU" until it works.

## 3. Git rules in this mode
- ONE long-lived branch `build/all-features`. **One commit per task**, message `type(scope): Step <ID> - <summary>`. Every commit must build and pass the unit tests. Never commit broken code.
- Push after every commit. Never push to `main` or `dev`, never force-push, reset --hard, rebase, or delete branches. Never merge. I merge the rolling PR myself.
- After each phase, regenerate the review package: `.\scripts\prepare_claude_review.ps1 -Step "<phase name>" -Since <commit before the phase> -Extra PROJECT_HANDOFF.md,BUILD_QUEUE.md` and say where `Claude_review.zip` is.

## 4. Per-task loop and BUILD_QUEUE.md format
**Per task (in this order):** 1) set the row IN_PROGRESS and commit it with the task; 2) read the existing code and reuse it; 3) list failure paths; 4) implement in small compiling slices; 5) unit tests (new logic needs tests; fakes/test doubles only in test code); 6) build + run all unit tests; 7) install and launch on the emulator, open the affected screen, paste the key lines of `adb logcat -d -s AndroidRuntime:E`; 8) Break-It: 5 attacks (small task) or 10 (mode/feature), recorded as one-line rows in `report.md` under the phase, each with a real result; 9) for UI tasks: valid screenshots in `docs/screenshots/<task-id>/` (awake screen, foreground check, wait 3 s, > 100 KB, not black, SHA-256 differs from every other screenshot, VIEW it and describe it in one line in the queue row); 10) update the queue row + one `CHANGELOG.md` line; 11) commit + push.
**Docs per phase (not per task, to save tokens):** `progress.md`, `README.md` (only if behavior/setup changed), `PROJECT_HANDOFF.md` (sections 0, 13, 16, 17, 17b), `report.md`.
**Fix loop:** a build error or crash that survives 2 fixes = stop that task, mark BLOCKED(reason), restore the working tree, go to the next task.
**BUILD_QUEUE.md columns:** `ID | Task | Needs | Status | Commit | Evidence | Notes`. Status: TODO, IN_PROGRESS, DONE (proof in Evidence), PARTIAL, NOT VERIFIED (code done, proof missing), BLOCKED(reason). Sections: QUEUE, DECISIONS, BLOCKERS (what the user must do, in plain steps), RUN LOG (one line per session).

## 5. Blocked tasks and pre-approvals
- **Never block the whole run.** If a task needs something I must give, mark it BLOCKED with the exact action I must take (put it under BLOCKERS), and continue with the next task. Block types: `USER` (my account, my files, my listening/tapping), `MODEL` (a .tflite file or hosting URL), `DEP` (a new library: write `docs/DEPENDENCY_PROPOSALS.md` with name, version, why, alternative; do NOT add it), `DEVICE` (needs the Redmi: implement and emulator-verify what you can, mark device NOT VERIFIED), `EMU` (emulator unavailable).
- **Pre-approved gradle/manifest changes (only these):** (a) task 6.1 R8 + a local `releaseTest` build type signed with the debug key (no keystore touched); (b) task 1.4 foreground-service entries/permissions needed for the export worker. Any other gradle, manifest, dependency, Room-schema, signing or SDK change = BLOCKED with a proposal. If the existing dependencies already contain what you need (Media3, WorkManager, Coil, Hilt, TFLite), use them.
- Never touch `local.properties`, keystores, tokens, `google-services.json`. No analytics. No user data off-device.

## 6. THE QUEUE (do in this order; each line is one task and one commit)
### P1 - Editor and export correctness (EMULATOR)
- **1.1** Remove the fake sample generator in `ProjectsScreen.kt` EmptyProjectsState (text-file "videos", blue JPG, `/sdcard/sample_video*.mp4` reads). The empty-state "New Project" opens the same PhotoPicker as the top bar. Proof: `grep -rn "LENSORA_SAMPLE"` returns nothing; screenshot of the empty state; picker opens.
- **1.2** One editor header only (editor view shows the editor's own header with undo/redo/Export; the Projects TopAppBar shows only in list view). Back and delete project still work.
- **1.3** Export dialog: outside tap and back press must NOT cancel a running export (`DialogProperties(dismissOnClickOutside=false, dismissOnBackPress=false)`); only the "Cancel Export" button cancels. Show success (e.g. "Saved to Movies/Lensora_Ai") and failure messages.
- **1.4** `ExportWorker` correctness: (a) output size from the project aspect at COMPOSITION level, not per item (`ASPECT_MODE = CROP_TO_FILL` as one constant; the user can switch to letterbox later); (b) guarantee an audio track when photos and videos are mixed (check which Media3 API exists in the project's Media3 version; if unsure say "not sure" and show what you verified); (c) MediaStore failure: delete the pending row, return a clear failure, never silent success; (d) delete the internal copy after a successful MediaStore save, keep it if MediaStore failed; (e) run as a foreground task (`setForeground`, notification channel, foreground service type dataSync, verify against the WorkManager version docs). Unit tests for size/aspect math.
- **1.5** Test media: generate real valid test files (if `ffmpeg` is on the PC: `testsrc` video with a sine audio track, 8-10 s, one 1280x720 and one 720x1280, plus a JPG made with a normal tool; if not, mark BLOCKED(USER): "put 2 real videos and 1 photo in C:\Lensora_testmedia"). Push them to the emulator and import ONLY through the PhotoPicker. Then fix thumbnails (blank) and the black preview with REAL media. Thumbnails must not need a new dependency (e.g. `MediaMetadataRetriever` on `Dispatchers.IO` with a small cache). Evidence: screenshots of the timeline with real thumbnails and a playing preview; trim, split, reorder, delete, undo, redo each with its own screenshot AND the project-JSON line after the action; export 720p and 1080p on the emulator with `ffprobe` output (duration within +/-0.5 s, WxH matches the aspect, audio present); a cancel test (button) leaving no partial file.
- **1.6** Editor state safety: after delete/undo no selection may point to a missing clip; edits survive force-stop and relaunch; rotation does not lose state.
Phase report: Break-It for the editor (10 attacks), review zip.

### P2 - AI pipeline (real or honestly Partial)
- **2.1** `SceneInference`: when `scene_classifier.tflite` exists and passes `ModelFileGuard`, run it on the 224x224 preview frame at the existing 2 FPS cadence off the main thread, map class ids to the scene labels via a labels file in assets (create the mapping file only from a real label list; if there is none, BLOCKED(MODEL) "labels.txt for the chosen model"). When the model or labels are missing, keep the current heuristic path unchanged and expose which source produced the label in the state (do not show fake "AI" claims). Tests with an interface-based fake interpreter (test code only).
- **2.2** `ModelContractChecker`: at load, read the input/output tensor shapes and dtypes of each of the 4 models and compare with the contract the code assumes (512x512 tiles, channels, 2x output for super-res). A mismatch marks the model "Incompatible" in the Settings model section with a plain message and never crashes. Tests with fakes. Real-model proof: BLOCKED(MODEL) until the files exist.
- **2.3** Tiled pipeline: make `real_esrgan_v2` (output tile larger than input), `dehaze` and `lama_dilated` tile flows correct for their output size (stitch at the right scale, recycle every tile bitmap, no OOM on a 12.5 MP test image). Unit tests with fake interpreters asserting output dimensions and that every tile is recycled.
- **2.4** Model supply: re-check `docs/MODEL_SUPPLY_PLAN.md` against the code. Everything that can be done without a model file or URL is done; write the exact list of files/URLs/SHA-256 I must provide into BLOCKERS(MODEL).
Phase report.

### P3 - Camera modes (use `docs/MODE_AUDIT.md` and the README mode table as the spec)
For EVERY mode that is not WORKING with evidence, in this order of feasibility, make it really work or keep it honestly SOON with a one-line reason: BEAUTY, WATERMARK, FOOD, DOCUMENT, TIME-LAPSE, SLOW-MOTION, NIGHT VISION, PRO MACRO, ULTRA MACRO, ASTRO, DEEP SKY, RAW DNG, brand looks (LEICA, ZEISS, HASSELBLAD, XMAGE, HARCOURT), SCAN (PDF via `android.graphics.pdf.PdfDocument`; QR/edge detection libraries only if already in the project, else BLOCKED(DEP)), PANORAMA, DUAL-VIDEO, HIGH-RES (BLOCKED(MODEL) until `real_esrgan_v2`). One commit per mode. Per mode: mode switch works, capture/record produces a valid file (check with `ffprobe` or by decoding), resources are released on background, no crash, Break-It 10 attacks. Camera2 capability checks first (`CameraCharacteristics`): if the hardware does not support the mode (e.g. constrained high-speed, concurrent cameras), degrade gracefully with a clear message, never crash. Hardware behavior can only be proven on the Redmi: mark it `NOT VERIFIED on device`. Never change a mode's name, label or position.
Phase report.

### P4 - Editor features (VN-style)
- **4.1** Per-clip speed control in the UI (the engine already has `speed`); preview and export agree on duration.
- **4.2** Templates: `TemplateLoader` + `assets/templates/*.json` create a project with the template's slots and aspect; the user fills slots from the picker.
- **4.3** Look presets from the Vibes tab applied to the export as a video effect, only if the project's Media3 version has a suitable effect (check the exact API first; if not, BLOCKED(DEP) with a proposal).
- **4.4** Text overlay on a clip (position, size, color from theme tokens), preview and export.
- **4.5** Per-clip volume/mute with an audio-aware export.
Each: unit tests, emulator preview + export proof (ffprobe), screenshots, Break-It 10. Phase report.

### P5 - Cloud Vault (Google Drive)
- **5.1** Review the OAuth and upload code. Write `docs/DRIVE_SETUP.md` with exact steps for the user (package `com.lensora.ai`, debug and release SHA-1 from `.\gradlew.bat signingReport`, OAuth client types, consent screen test users). Fix code-level bugs only (token storage in `EncryptedSharedPreferences`, Wi-Fi only constraint, resumable upload, SHA-256 verify before purge: the local file is deleted ONLY after the cloud checksum matches). Unit tests with a fake HTTP layer.
- **5.2** Real sign-in and upload: BLOCKED(USER) (my Google account, on a device). Never store emails or tokens anywhere.

### P6 - Release hardening
- **6.1** R8 for a `releaseTest` build type (minify + shrink resources, keep rules for Hilt, TFLite incl. GPU delegate, Media3, WorkManager, Compose, serialization). Pre-approved gradle change. Build it, install on the emulator, smoke-test all 5 tabs, a mode switch, an import and an export. Record APK size before/after.
- **6.2** Memory: `dumpsys meminfo` before/after 20 repeated camera-mode switches and 3 exports; no growing leak; document the numbers.

### P7 - Quality and launch gate
- **7.1** Feature-wide Break-It, 25 attacks per group (camera, gallery, studio/editor/export, settings/models, vibes). Fix root causes found inside this branch (max 3 cycles per group). All results in `report.md`.
- **7.2** Drafts only: Privacy Policy and Terms (what the app really collects), simple scrollable in-app screens reusing theme tokens, flagged "legal review needed". No icon or splash changes (design decision for the user: BLOCKED(USER) asset).

### P8 - Final docs
Full sync of `README.md` (honest mode and model status tables), `PROJECT_HANDOFF.md` (every section current, update log), `progress.md`, `CHANGELOG.md`, `report.md`, `BUILD_QUEUE.md`. Final review zip and the final message.

### P9 - Device verification (USER-TRIGGERED, do nothing until I write "DEVICE NOW")
When I write "DEVICE NOW" and the Redmi (2312DRA50I) is connected: run `docs/DEVICE_VERIFY_BATCH_D.md` (the user saves it there) with these changes: it is the final device pass for everything above; no code changes unless I ask; every result labeled DEVICE; evidence outside the repo first, privacy rules apply (no personal photos, no accounts, no PIN).

## 7. Anti-patterns that already cost us days (do not repeat)
- Claiming "removed X / verified Y" without pasted output or a viewed screenshot.
- Black, dimmed, mid-launch or identical screenshots used as proof. Fake files made by hand. Fake sample clips in production code.
- Mixing unrelated changes in one commit. "Fixing" code you were not asked to touch (report it instead).
- Calling a task Done because it compiles. Done = built + tests + emulator run + evidence (+ device proof only when the task needs hardware, otherwise labeled NOT VERIFIED on device).
- Long pasted logs in chat (waste of quota). Write logs to `build_logs/`.

## 8. Session end message (always, max 30 lines)
Tasks DONE this session (IDs + commit hashes) | PARTIAL / NOT VERIFIED | BLOCKED with the exact action I must take | Problems found (unfixed) | Next task | Review zip path | the line: "To continue: paste 'Read docs/MASTER_BUILD_PROMPT.md and BUILD_QUEUE.md, then CONTINUE.'"
End with: `Next step: ...`
