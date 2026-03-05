# Authority KB Update SOP

This procedure governs updates to `docs/knowledge/`.

## 1. Trigger Conditions

Update authority entries when:

- LLVM/toolchain version policy changes
- ABI assumptions change
- build/test scripts adopt new third-party APIs
- codegen or driver behavior depends on new external docs

## 2. Required Metadata

Every authority entry must include:

- `Source:`
- `Version:`
- `Retrieved:`
- `Why it matters:`

## 3. Update Rules

- Prefer official primary documentation.
- Keep entries concise and Snow-specific.
- Keep pinned version text aligned with:
  - `AGENTS.md`
  - `cmake/DetectLLVM.cmake`
  - CI/gate scripts

## 4. Validation

Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/check_knowledge_base.ps1
```

Any missing field or version mismatch is a hard failure.
