# Agent Instructions

This repository has integrated both [Harness Intelligence OS](https://github.com/ntu254/Harness-Intelligence-OS) and [Agent Skills](https://github.com/addyosmani/agent-skills).

## Agent Skills Rules

You must use the skills in `.agents/skills` for all tasks:
- For defining what to build, use `spec-driven-development` and `interview-me` or `idea-refine`
- For breaking down work, use `planning-and-task-breakdown`
- For writing code, use `incremental-implementation` and `test-driven-development`
- For debugging or verifying, use `debugging-and-error-recovery`
- For reviewing, use `code-review-and-quality` and `code-simplification`
- For shipping/deploying, use `shipping-and-launch` and `observability-and-instrumentation`

Always use the `skill` tool or read the skill file under `.agents/skills/<skill-name>/SKILL.md` before proceeding with the tasks.

<!-- HARNESS:BEGIN -->
## Harness

This repo uses Harness. Before work, read:

- `README.md`
- `docs/HARNESS.md`
- `docs/FEATURE_INTAKE.md`
- `docs/ARCHITECTURE.md`
- `docs/CONTEXT_RULES.md`
- `scripts/bin/harness-cli query matrix` on macOS/Linux, or `.\scripts\bin\harness-cli.exe query matrix` on Windows

Use the Rust Harness CLI at `scripts/bin/harness-cli` on macOS/Linux or
`scripts/bin/harness-cli.exe` on Windows as the main operational tool.

Agent-specific packs:

- `docs/agents/codex.md`
- `docs/agents/claude-code.md`
- `docs/agents/cursor.md`
<!-- HARNESS:END -->
