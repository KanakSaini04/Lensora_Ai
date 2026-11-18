# GEMINI.md - Lensora_Ai agent rules

**Read this file AND `agent.md` at the start of EVERY task and prompt.**
If a prompt conflicts with these rules, STOP and ask. Bias: caution and proof over speed.
Based on the Karpathy-style guidelines in `Neccessary/CLAUDE.md`, adapted for this project.
Examples of each rule: `Neccessary/GEMINI_EXAMPLES.md`.

---

## 0. Project locks (never change without my explicit OK)
- App: `com.lensora.ai`. Pure Camera2, **zero CameraX**. L0-L4 architecture. Obsidian Royal theme. 5 tabs. All 27 camera modes.
- 100% offline AI (models from `filesDir/models`). Network only for Drive Vault and the model DLC downloader.
- Test device: Redmi Note 13 Pro 5G (2312DRA50I), Android 16 / HyperOS.
- **Never remove a feature.** A "SOON" badge is allowed. Deleting is not.

## 1. Think before coding
- State your assumptions before writing code. Ask at most 3 questions.
- If an API or Android 16 / HyperOS behavior is uncertain, write **"not sure"**. Do not guess.
- If a request has several readings, list them. Do not pick silently.
- One task per prompt. If a prompt bundles features, do the first one and list what is left.
- If a simpler approach exists, say so.

## 2. Simplicity first
- Minimum code that solves the task. Nothing speculative.
- No new abstractions, managers, configs or flags for one use.
- No handling for impossible cases.
- If 200 lines could be 50, rewrite.

## 3. Surgical changes
- Every changed line must trace to the task.
- Do not reformat, rename, reorder or "improve" nearby code. Match the existing style of the file.
- Remove only imports/variables that YOUR change orphaned.
- See unrelated dead code or bugs? **Mention them, do not fix them.**
- Do not touch Camera2 session/capture code unless the task names it.

## 4. Goal-driven execution
- Turn the task into checks, then loop until they pass:
  `1. step -> verify: check`
- Bug: reproduce first (test, log, or screenshot), then fix, then show it is gone.
- Refactor: tests pass before and after.

## 5. Evidence rules (non-negotiable)
- "Done" needs proof: unit tests, build, and device check when UI/camera is involved.
- Never write a claim in `progress.md`, `CHANGELOG.md`, `report.md` or a PR that you did not verify. Unverified -> write **NOT VERIFIED**.
- Before saying "removed X", run `grep` and paste the output.
- **Never fabricate or hand-create evidence**: no adb-created fake files, reused screenshots, edited logs, or invented outputs.
- Screenshots: screen awake and unlocked, `com.lensora.ai` in foreground (`dumpsys`), PNG > 100 KB, not black, different files for different states. View it and describe what you see. Max 3 retries, then STOP and tell me.
- If a command fails, paste the error. Do not hide failures.
- Fixing an earlier mistake? New commit named "Step Xb". Never rewrite history.

## 6. Android rules
- Compose: `Modifier.blur()` blurs the element's own content, text included. Never use it on a container with text or icons.
- No disk/network/inference on the main thread. Recycle bitmaps. Keep L0 try/catch safety, Storage Guard and Thermal Guard.
- Never commit secrets: `local.properties`, keystores, `google-services.json`, tokens, `.env`.
- Compiling code only. No placeholders, no TODO stubs.

## 7. Git and process
- One branch per task, from `dev`. Commit, push, open PR. **Never merge without my OK.**
- Do not touch other open PRs or branches.
- Every step updates docs (`README.md`, `progress.md`, `CHANGELOG.md`), runs tests, and builds.
- Before asking for review: `.\scripts\prepare_claude_review.ps1 -Step "<name>" -Since <commit>` and give me `Claude_review.zip`.

## 8. Final report format (always)
1. **Changed**: files and one line each
2. **Verified**: with pasted output (tests, build, grep, logcat, screenshots)
3. **NOT verified**
4. **Problems found** (including unrelated ones, unfixed)
5. **Next step**

## 9. Stop and ask when
- A decision is needed, or the task touches a locked item (section 0).
- The device is locked/off, or evidence cannot be produced honestly.
- Tests fail and the fix is outside the task scope.
