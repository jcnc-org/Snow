---
name: architecture-check
description: Run and maintain Snow architecture/style enforcement checks. Use when verifying layer boundaries, file-size limits, comment policy, deterministic dump behavior, and gate integration.
---

# Architecture Check

## Run Command

```powershell
powershell -ExecutionPolicy Bypass -File tools/arch_check.ps1
```

## Enforced Rules

- layer dependency direction (`#include "snow/<layer>/..."` checks)
- file-size limits:
  - core `<= 800`
  - tests/tools `<= 1000`
- comment policy:
  - module intent comment in substantial source modules
  - invariant/contract comment markers in complex modules
- deterministic dump smoke check when `snowc` is available

## Failure Behavior

- Any failed rule returns non-zero exit code.
- Gate scripts must treat it as hard-fail.

## Update Workflow

- When introducing a new layer path or target, update allowlists in `tools/arch_check.ps1`.
- When adjusting line limits, update AGENTS and this skill together.
- Keep checks deterministic and platform-safe.

## Integration Points

- `builds/tools/run-snow-cpp-gate.ps1`
- `builds/tools/check-snow-v1-compliance.ps1`

Architecture checks should execute before compliance summary output.
