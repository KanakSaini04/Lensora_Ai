Rules loaded. Task understood as: Step 31b2 docs updates for VIDEO and CINEMATIC modes.

**Changed:**
- README.md: updated VIDEO and CINEMATIC rows to ALLOWED (advertised, not device-tested) with HAL AE FPS ranges.
- report.md: updated probe P3 description & added NOT VERIFIED note and Step 31b2 Break-It table.
- PROJECT_HANDOFF.md: updated VIDEO and CINEMATIC rows and added advertised-only modes NOT VERIFIED list.
- reply.md: single-file review report created.

**Verified:**
- Unit tests & build: `gradle_build("testDebugUnitTest assembleDebug")`
- Grep checks: 0 matches for stale text, hits in all 4 docs for "device-tested", diff stat touches .md only.

**Break-It:**
- 5 attacks run (stale text grep, 4-doc grep, diff stat scope, JSON range match, UTF-8 no BOM check), verdict **GOOD**.

**NOT verified:**
- Physical device execution for VIDEO and CINEMATIC modes.

**Problems found:** None.

**How to try it manually:** View README.md, report.md, and PROJECT_HANDOFF.md tables.
**Risks / rollback:** `git revert` or checkout `dev`.

**Docs audit:** README.md (Updated), report.md (Updated), PROJECT_HANDOFF.md (Updated), reply.md (Created).

**Commit message:** docs(probe): Step 31b2 - label Cinematic and Video as advertised, not device-tested
**PR link:** PLACEHOLDER_PR_LINK | **Merge commit:** PLACEHOLDER_MERGE_HASH

**Done.** Next step: Step 31c
