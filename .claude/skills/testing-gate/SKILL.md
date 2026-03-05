---
name: testing-gate
description: Operate Snow build/test/compliance gates. Use when running PR-quality validation, updating CTest manifests, adding regression suites, or tightening hard-fail policy for compiler changes.
---

# Testing Gate

## Canonical Validation Commands

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/build-snow-cpp.ps1
powershell -ExecutionPolicy Bypass -File builds/tools/run-snow-cpp-gate.ps1
powershell -ExecutionPolicy Bypass -File builds/tools/check-snow-v1-compliance.ps1
```

## Gate Policy

- Hard-fail on:
  - architecture/style checks
  - format checks
  - unit/CLI tests
  - compliance checks
  - knowledge-base integrity check

## CLI Regression Structure

- Keep CLI test cases declarative in a manifest file.
- Generate/register CTest tests via looped CMake logic, not hand-expanded repetitive calls.
- Keep expected output checks deterministic and source-controlled.

## Coverage Requirements

- frontend parse/diagnostics
- sema and ownership errors
- SIR validation and pass pipeline behavior
- LLVM lowering and object emission
- multi-target triple behavior
- runtime entry wrapping policy

## Maintenance Rules

- Keep test names stable and grep-friendly.
- Keep artifacts in test binary directory.
- Avoid test ordering dependencies unless explicitly declared with `DEPENDS`.
