# GEMINI.md v2 - Lensora_Ai rulebook

**Read this file AND `agent.md` at the start of EVERY task and prompt. Then read `PROJECT_HANDOFF.md`.**
Merged from the generic Android rulebook (`Neccessary/GEMINI_RULES.md`, kept as reference) and the Lensora evidence rules. Where they differ, THIS file wins.
**Priority:** Safety (2) > Quality bar (12) and Honesty (5) > Project locks (0) > Behavior (1) > Workflow (3, 4) > Standards (6, 7).
If two rules clash, or a rule clashes with my prompt: STOP and ask. Do not choose silently.

---

## 0. Project locks (never change without my explicit OK)
- App `com.lensora.ai`. Pure Camera2, **zero CameraX**. L0-L4 architecture. 5 tabs. All 27 modes stay (a "SOON" badge is allowed, deleting is not). 100% offline AI.
- **Design is locked: Obsidian Royal** (Background #0E0E0E, Surface #131313, Royal Blue #2563EB, Cyan #00E5FF, Inter). Existing pills, emoji mode labels and translucent glass overlays stay. The generic palette, "no pills" and "no emoji" ideas from the old rulebook do NOT apply to existing UI. New UI must reuse the existing theme tokens.
- Test device: Redmi Note 13 Pro 5G (2312DRA50I), Android 16 / HyperOS. Emulator: `Medium_Phone`.
- **Never remove a feature.**

## 1. Behavior
1. **Understand first.** Restate the task in 1-2 lines with "Done when...". List assumptions. Ask at most 3 questions, all at once. Read the existing code and reuse before writing anything new.
2. **Build exactly what was asked.** Minimum code, smallest diff, match the file's style. No refactor, rename, reformat, import reordering or cleanup of anything not mentioned. Unrelated bug or dead code: report it, do not fix it. Remove only what YOUR change orphaned.
3. **Real code only.** No placeholders, stubs, TODOs, fake or mock data in production code. No invented APIs: use only classes that exist in the project or in dependencies already in `libs.versions.toml` / gradle files. Check every API against `minSdk`, `compileSdk` and the current Kotlin/Compose/Media3 versions; if not sure, say "not sure". No deprecated APIs in new code.
4. **Research the HOW, never change the WHAT.** The feature is fixed. Check the official docs for the project's exact versions. If my suggested method works, use it; if not, use the best working one and tell me why. A new library is never added silently: give name, version, reason, and wait.
5. **Never deliver a faked or partial feature as done.** If part is impossible, build what is possible, list what is missing, mark it **Partial** in `progress.md`.
6. **Verify, never assume.** Build after every edit. Debug from real evidence (build output, Logcat, stack trace): quote the exact error and name file/line/cause BEFORE fixing. Say "tested" only if you ran it.
7. **Halt on loops.** If a bug or build error survives **2** fixes, STOP and report. Never guess a third time.
8. **Honesty.** Root causes, not symptoms. Say "I'm not sure" when unsure. Mistakes: say so at once and fix with a new commit. Never hide a failure.
9. **Style.** Short and direct. Separate **Done / Not done / Needs my decision**.

## 2. Hard safety rules
- Do NOT change gradle files, `libs.versions.toml`, dependency/plugin/SDK versions, ProGuard/R8, `AndroidManifest.xml`, package name, Room schema/DB version, signing or `google-services.json` UNLESS the prompt's task requires it. If it does, state exactly what changed. If not stated in the prompt, ask first.
- Never touch keystores, `local.properties`, tokens, PINs or personal data. Never hardcode secrets. Never add analytics/ads or send user data off-device.
- Never move or restructure existing packages. Never touch generated files (`build/`, `.idea/`).
- **Git:** work ONLY on your step branch. You may push your own step branch, open a PR into dev, and merge it into dev yourself only after the section 12 gates pass. Never push to main, never force-push, never reset --hard or rebase, never delete branches other than your own merged step branch. The owner updates main.
- Before a change touching more than 3 files, or any DB/manifest/navigation/DI/build change: tell me to checkpoint, and give a one-line rollback plan.
- One task, one purpose. Never mix unrelated changes.

## 3. Workflow
- **One step = one branch from the latest `dev`:** `step-<NN>-<slug>` (`step-62b-...` for corrections). Never rewrite history.
- **Plan first.** State the steps and exact files. If a prompt says `PRE-APPROVED` or runs in BATCH mode, the plan is approved: proceed without waiting. Otherwise wait for my OK when the change touches more than 3 files or DB/manifest/navigation/DI/build.
- **Order:** understand -> failure paths (no permission, empty data, rotation, rapid taps, process death) -> build in small compiling slices -> tests -> Break-It -> docs -> run and evidence -> report.
- **Docs sync after every step (no extra approval needed):** `progress.md` (newest entry on top), `CHANGELOG.md`, `README.md` if behavior/setup changed, `PROJECT_HANDOFF.md`, `report.md` for device evidence. Do NOT create `PROGRESS.md` or `KANBAN.md` (Windows is case-insensitive, it would clash with `progress.md`; the roadmap lives in `PROJECT_HANDOFF.md`). Document only what exists and was verified; unverified = **NOT VERIFIED**; partial = **Partial**. Minimal edits, add-only, UTF-8 no BOM, never rewrite whole files.
- Show a docs audit list: each file Updated (one line) or No change needed (reason).
- Review package: `.\scripts\prepare_claude_review.ps1 -Step "<name>" -Since <commit> -Extra PROJECT_HANDOFF.md` and give me `Claude_review.zip`.
- Commit: `type(scope): Step NN - summary` (feat, fix, docs, test, tooling). Atomic commits. Never commit broken code.

## 4. Testing and Break-It (scaled)
- **Tests:** unit tests for every new/changed ViewModel, use case, repository, utility (normal, edge, error). Integration/UI tests where the project already has the tooling. Deterministic: no real network, no `Thread.sleep`. Never delete, weaken or `@Ignore` a test to make it pass.
- **Break-It after every task:** try to destroy your own work with real evidence per attack (what you did, expected, actual, Logcat/test line). Minimum **5 attacks** for a small fix, **10** for a step, **25** for a finished feature. Cover several layers: UI (spam taps, rotation, back at every step, font scale 1.3, small screens), state (repeated events, cancelled coroutines), data (empty, corrupted, interrupted writes, full storage), OS (process death, permission revoked, background), hardware (camera/MediaRecorder released on background). Use `adb shell monkey`, `am kill`, repetition (50+) where useful.
- Attacks you cannot run (no device, no setup): say so. Never report an attack you did not run.
- **Verdict GOOD / NOT GOOD.** Only GOOD can be called done.
- **Fix loop max 3 cycles** (test -> break -> fix root cause -> retest all). No hacks: no swallowing `catch`, `@Suppress`, `!!`, special-casing tests. Fix only code from this task; report older bugs. A build error or crash surviving 2 fixes stops the loop immediately (1.7).
- Log Break-It results in `report.md` (table: # | layer | attack | expected | actual | held/broke).

## 5. Evidence rules (non-negotiable)
- "Done" needs proof: tests, build, and a run on the emulator (device when required).
- Never write a claim in `progress.md`, `CHANGELOG.md`, `report.md`, `PROJECT_HANDOFF.md` or a PR that you did not verify. Unverified = **NOT VERIFIED**.
- Before saying "removed X", run `grep` and paste the output.
- **Never fabricate or hand-create evidence:** no fake sample files in production code (the old `LENSORA_SAMPLE_VIDEO_STREAM` text-file clips are exactly this mistake), no adb-made fake outputs, reused screenshots, edited logs.
- **Screenshots:** screen awake and unlocked, `com.lensora.ai` in foreground (`dumpsys`), wait 3 s after launch, PNG > 100 KB, not black, different files for different states (compare SHA-256). View it and describe in one line what it truly shows. Max 3 retries, then STOP.
- A command fails: paste the error. Never hide failures.
- Privacy: no PIN, emails, tokens or personal photos in repo, logs or screenshots.

## 6. Engineering standards (NEW code only; old working code stays untouched)
- **Architecture:** Compose + MVVM, one immutable `StateFlow` per screen, stateless composables, Hilt. ViewModels never hold a `Context`/`View`.
- **Threading:** never block Main. Disk/network/inference on `Dispatchers.IO`/`Default`. Collect with lifecycle (`collectAsStateWithLifecycle`/`repeatOnLifecycle`). Cancel or scope coroutines.
- **Resources:** release Camera, MediaRecorder, players, listeners, receivers in the right lifecycle callback or `DisposableEffect`. Close streams/cursors with `use {}`. Downsample/tile big images, recycle bitmaps. Fail safely on OOM with a clear message.
- **Manifest wiring in the same step** (permissions, services, FileProvider), and tell me exactly what you added. Runtime permissions just-in-time; handle denied, "don't ask again", revoked.
- **Errors:** never fail silently or crash; clear user-facing message and safe state. No empty `catch`; every caught exception is handled AND logged (`Log.e`, clear tag; never log secrets or personal data).
- **Storage/security:** scoped storage (MediaStore/SAF), no broad storage permission, `EncryptedSharedPreferences` for sensitive data, collect only needed data.
- **Compose hygiene:** strings in `strings.xml`, colors/shapes/spacing from the theme (no inline hex or magic numbers in new UI), stable `key`s in lazy lists, avoid needless recomposition.
- **Performance:** no jank, no leaks; R8 in release (Step 69).

## 7. UI standards (new UI, inside the Obsidian Royal lock)
- Reuse theme tokens. Touch targets >= 48dp. 8dp grid. Primary actions in the lower third. One primary filled button per screen.
- Insets applied ONCE at the right level (`Scaffold` innerPadding or one `statusBarsPadding`, not stacked). Content never hides behind status bar, cutout, nav bar or keyboard; check with keyboard open and font scale 1.3.
- **`Modifier.blur()` never on a container with text or icons.**
- Text in rows/buttons: `maxLines` + ellipsis, except essential info (errors, key values) which wraps. Forms/settings scroll. No nested unbounded vertical scrollers.
- No card inside a card. Real loading (skeleton), empty and error states with a retry where it makes sense; destructive actions need confirmation; transient status via Snackbar.
- No fake UI: no hardcoded dummy values; every screen driven by real state.
- Copy: short, plain, specific. No buzzwords, no em dashes in user-facing text, no fake metrics/reviews/counters, no lorem ipsum, no "Made with AI" anywhere.
- Back behavior: back from a secondary tab returns to the start tab; Predictive Back only if added with my OK (manifest change).

## 8. Run and device policy
- After EVERY change: build, install, launch on the emulator, open the affected screen, paste `adb -s emulator-5554 logcat -d -s AndroidRuntime:E`. Label every result **EMULATOR** or **DEVICE**.
- Use the physical device only when the prompt says `DEVICE REQUIRED: yes`. If it is not connected: STOP and tell me.
- Camera2 hardware behavior (HAL, lenses, RAW, video, thermal, GPU speed, HyperOS kills) cannot be verified on the emulator: **NOT VERIFIED on device**.
- The emulator may be locked: wake and unlock it with the PIN given in the prompt (never store the PIN anywhere).
- **Default Device (EMULATOR)**: The virtual device (`Medium_Phone`, Android 16 / API 35/36, `emulator-5554`) is the DEFAULT test device. You may start and manage it yourself. Always pass `-s emulator-5554` when running `adb` commands (as multiple devices may be connected).
- **Physical Device (REDMI)**: The physical Redmi Note 13 Pro 5G (`5ceab3e6`) is the owner's daily phone. Use it ONLY for capabilities the emulator cannot verify (Camera2 HAL, RAW, video recording, thermal behavior, HyperOS task killer) and for ONE final confirmation before merging a UI feature.
- **Redmi Access Protocol**: Before touching the Redmi device, you MUST send a chat message: `"I need the Redmi for <what>, about <N> minutes. Reply READY."`, then STOP and wait for owner approval. When finished, say `"Redmi free"`. Never touch the Redmi without asking first.
- **Result Labeling**: Every test/verification result MUST be labeled **EMULATOR** or **DEVICE**. A UI feature is merged only after all criteria are verified on the EMULATOR AND one final DEVICE confirmation (requested via the protocol above).

## 9. Report format (final message & reply.md, always)
- **`reply.md` File**: Create or OVERWRITE `reply.md` at repo root at the end of every task (never append). Include Status, Goal & acceptance criteria matrix, Design/Root cause summary, What changed list, Evidence (tests, device, Break-It), Risks/rollback, and Docs audit.
- **Final Message Format**:
  1. `Rules loaded. Task understood as: ...` (start of the task)
  2. **Changed:** files, one line each, plus assumptions
  3. **Verified:** pasted output (tests, build, grep, logcat, screenshots with a one-line description)
  4. **Break-It:** attacks run, verdict GOOD / NOT GOOD
  5. **NOT verified**
  6. **Problems found** (including unrelated ones, unfixed)
  7. **How to try it manually** + **Risks / rollback** (big changes)
  8. **Docs audit**
  9. **Commit message** (one line) and PR link
  10. **Done / Not done / Needs my decision**, then `Next step: ...`

## 10. Definition of Done (ALL true before saying "done")
- [ ] Does exactly what was asked, end to end, no stubbed or faked parts
- [ ] Builds with no new warnings/errors; existing features and tests still pass
- [ ] Error, empty, loading, offline and permission-denied states handled
- [ ] New tests pass; Break-It verdict GOOD; `report.md` updated
- [ ] Follows sections 0-8; no secrets, debug code or placeholder content
- [ ] Docs synced (section 3); minimal diff; commit message and PR provided
- [ ] I was told honestly about every risk and everything left undone
- [ ] Owner does not need to find any bug; section 12 gates all passed.

## 11. Launch gate (before any "ready to launch" claim)
Custom adaptive/monochrome icon, SplashScreen API, Privacy Policy and Terms (in-app and public URL, drafted from what the app really collects, flagged for my legal review), custom domain/support email, all debug/test/placeholder content removed, no fake metrics, release build checked (R8 on, no debug logs, no secrets), all tests passing. If anything is missing, list it and do not mark launch-ready.

## 12. Quality bar (owner's rule, very high priority)
- Quality over quantity. One fully working feature beats many features with bugs. A feature with known bugs is never acceptable. If a feature cannot be made bug-free, do not ship it: leave it out or hide it behind a SOON badge, and say why.
- One feature at a time. Finish it, verify it on the device, merge it, then start the next. No parallel half-built features.
- The owner must not have to find bugs. The owner only reviews finished, verified work. Never ask the owner to test something you can test yourself on the Redmi (adb -s 5ceab3e6; an emulator may also be connected). Automate what you can (adb input, uiautomator, screenshots, logcat) and look at the results yourself.
- Goal-driven work: the owner gives the goal and acceptance criteria (what), you research and choose the how. Work like a senior engineer who owns the feature.
- Stale-value check (every Compose change): review every long-lived block (pointerInput(Unit), LaunchedEffect(Unit), remember lambdas, callbacks stored in objects) for captured values that can change (sizes, scale, mode, flags). Use rememberUpdatedState or proper keys. Add a test or device check for it.
- Merge gates. A change is merged only when ALL are true: all acceptance criteria verified on the device, unit tests pass, Break-It (10+ real attacks) verdict GOOD, docs honest, no known bug left. If anything is unverified, do not merge; report exactly what remains.
- If a bug survives 3 real fix attempts: stop, restore the last working state (git revert or abandon the branch), and report. Do not ship a workaround.
- Report back to the owner only when the work is merged and verified (or when you are truly blocked). Report: root cause, what changed, evidence per acceptance criterion, PR link, merge commit, anything NOT VERIFIED, risks.

---
**Golden rule: when unsure, ask. Do less, not more. Never break working code. Build exactly what was asked, test it, attack it, prove it, and tell the truth.**
