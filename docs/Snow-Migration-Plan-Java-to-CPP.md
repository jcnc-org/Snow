# Snow Migration Plan: Java VM -> C++ LLVM (v1.0)

Status: Completed (C++ mainline cutover)

Date: 2026-03-05

## 1. Objective

Complete migration from Java+VM implementation to a C++20+LLVM native compiler mainline.

## 2. Final Cutover State

- Repository build/test path is C++ only.
- Root CMake layout is canonical (`cmake -S . -B build`).
- Java source/modules/Maven scripts are removed from active repository flow.
- Historical Java-era documentation is preserved under `docs/legacy/`.

## 3. Active Delivery Scope (v1)

- Frontend, semantic analysis, ownership checks
- SIR build + validation
- O0/O2 pass pipeline
- LLVM lowering and native artifact emission
- Runtime ABI integration and executable entry wrapping
- CLI commands and debug emit surfaces
- Compliance + PR gate scripts for C++ mainline

## 4. Validation Policy

v1 acceptance requires:

- `cmake` configure/build success on supported toolchains
- `ctest` suite green
- `builds/tools/check-snow-v1-compliance.ps1` pass
- `builds/tools/run-snow-cpp-gate.ps1` pass

## 5. Notes

- ABI/mangling/ownership/SIR semantic changes remain spec-governed and require same-PR doc updates.
- Any future Java compatibility work is treated as historical/reference analysis only, not a required gate.
