---
name: refactor-roadmap
description: Execute Snow’s big-bang maintainability refactor milestones. Use when sequencing architecture-wide changes, mapping milestone tags, or checking completion criteria across governance, code splits, gates, and docs.
---

# Refactor Roadmap

## Milestone Map

- `M0` baseline snapshot
- `M1` governance index + skills
- `M2` architecture/style/knowledge gate tooling
- `M3` CMake per-layer targets
- `M4` monolith splits (parser/sir-builder/driver/lowering/passes/tests)
- `M5` CLI normalization (`--dump` structured output)
- `M6` perf instrumentation + docs migration
- `M7` full green gate and merge

## Execution Principles

- Maintainability and extensibility first.
- Preserve grammar semantics while making parser architecture extension-friendly.
- Allow controlled interface breaks where needed for architecture cleanup.
- Keep each milestone testable and reviewable.

## Done Criteria

- Hard gates pass.
- Layer boundaries are tool-enforced.
- File size policies are green.
- Key authority docs exist with provenance.
- Refactor milestone checklist is complete.
