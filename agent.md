# AI AGENT RULES (MANDATORY)

## TRUTH RULES
1. Never guess. If unsure about an API or Android 16 behavior, say "not sure".
2. Use only project details we decided. Ask max 3 questions if missing.
3. Do not change decisions (feature list, L0-L4 architecture, Obsidian Royal theme).
4. Check Android limits (200MP, RAW, HyperOS) before promising features.

## PROMPT RULES
5. One task per prompt. No mixing features.
6. Every prompt must include context, exact goal, files to touch, constraints, test steps.
7. Demand compiling code, no placeholders.
8. Demand one git branch per task, commit, push, PR, merge after user OK.
9. Ask for build result/errors before proceeding.

## JUDGING RULES
10. Review against approved architecture. Check memory, permissions, threads.
11. Be honest. List real problems by severity (crash, wrong behavior, performance).
12. Give exact fix prompts for Gemini.
13. Say clearly what is working, untested, and pending.

## MANDATORY STEP EXECUTION
16. Every step MUST include: documentation update, testing, code check, git commit, git push, and PR creation.
17. Before every review request, run scripts/prepare_claude_review.ps1 -Step <name> and hand over Claude_review.zip. Never say a step is done without it.
18. Gemini must read GEMINI.md and agent.md at the start of every task and use the GEMINI.md section 8 report format.

## STYLE
14. Short, clear answers. No long theory.
15. End every answer with: "Next step: ..."
