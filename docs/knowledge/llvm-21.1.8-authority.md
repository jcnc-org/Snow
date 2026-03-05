# LLVM Authority Reference (Snow v1)

- Version: `21.1.8`
- Retrieved: `2026-03-05`
- Scope: Snow codegen/lowering/object emission toolchain assumptions

## Entry 1: LLVM CMake Package Discovery

- Source: https://llvm.org/docs/CMake.html
- Version: LLVM 21.x CMake config model
- Retrieved: 2026-03-05
- Why it matters: Snow requires deterministic LLVM discovery via `find_package(LLVM 21.1.8 EXACT CONFIG REQUIRED)`.

## Entry 2: Target Triple and TargetMachine Usage

- Source: https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/LangImpl08.html
- Version: LLVM TargetMachine flow (21.x compatible usage pattern)
- Retrieved: 2026-03-05
- Why it matters: Snow object emission depends on lookupTarget + createTargetMachine + data layout/target triple setup.

## Entry 3: LLVM IR Language Reference

- Source: https://llvm.org/docs/LangRef.html
- Version: LLVM IR LangRef (21.x line)
- Retrieved: 2026-03-05
- Why it matters: Snow textual lowering emits LLVM IR directly, so instruction and type text must remain valid.

## Entry 4: LLVM Source-Level Tooling Baseline

- Source: https://github.com/llvm/llvm-project/releases/tag/llvmorg-21.1.8
- Version: `llvmorg-21.1.8`
- Retrieved: 2026-03-05
- Why it matters: Snow v1 policy is pinned to this release for CI and developer toolchain parity.
