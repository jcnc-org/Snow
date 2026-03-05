# AGENTS.md (Snow Compiler v1.0 Governance)

This file defines mandatory architecture and code-quality constraints for all contributors and AI agents working on Snow.

## 1. Scope

- Applies to all C++ compiler work in this repository root layout.
- Java/Maven implementation is decommissioned from active build/test flows.
- If this file conflicts with local style habits, this file wins.

## 2. Architecture Boundaries

- Pipeline is fixed: `Source -> Lexer -> Parser -> AST -> Semantic Analysis -> Ownership Check -> SIR Build -> sir-validator -> Passes -> LLVM Lowering -> LLVM CodeGen -> Object/Executable`.
- Backend target path is fixed: `AST -> SIR -> LLVM`.
- Custom VM is not a primary backend for v1.0.

### Layering

- `frontend`: tokenization, parsing, AST only.
- `sema`: symbol tables, type checks, import/name resolution, visibility checks.
- `ownership`: ownership/lifetime checks and drop-insertion facts.
- `sir`: strongly typed SSA IR data model and printers.
- `passes`: standalone IR transforms.
- `codegen/llvm`: lowering to LLVM IR and target config.
- `runtime`: C ABI runtime surface used by generated programs.
- `driver/cli`: command orchestration, flag handling, compile entrypoint.

No layer may depend upward.

## 3. C++ Standards

- Language: C++20.
- Prefer Rule of Zero and RAII.
- Owning raw pointers are forbidden.
- Use `std::unique_ptr`/`std::shared_ptr` only where ownership semantics are explicit.

## 4. File and Module Size

- Prefer single responsibility per file.
- Hard guidance: each source file should stay under 800 lines.
- If file grows beyond 800 lines, split by concern immediately.

## 5. SIR and Pass Rules

- SIR must be strongly typed, SSA, explicit CFG, platform-independent.
- Any IR transformation must be an isolated pass in `passes/`.
- New pass must document:
  - input invariants
  - output invariants
  - failure modes
- `sir-validator` must run:
  - debug builds: after every pass
  - release builds: key checkpoints

## 6. Ownership and Lifetime

- MVP has no implicit GC.
- Language model is ownership + deterministic drop.
- `drop` is side-effecting; optimization cannot remove/reorder it unsafely.

## 7. Import/Name/Visibility Policy

- Resolution order: `local -> current module -> imported modules`.
- Unqualified symbol collisions across imports must error (`AmbiguousSymbol`).
- `star import` is allowed but discouraged and should emit warning by default.
- Visibility defaults to private. `pub` is required for cross-module exports.

## 8. ABI and Runtime Constraints

- ABI baseline is native platform ABI (SysV/MSVC/AArch64 PCS).
- Stack alignment at call boundary: 16 bytes.
- Runtime entry chain for executables: `host main -> snow_runtime_start -> user main`.
- Runtime auto-entry wrapping applies only to executables, not libraries.

## 9. LLVM and Target Policy

- Locked LLVM toolchain: 21.1.8.
- CI matrix must validate LLVM 21.1.8 on supported host platforms.
- Supported targets:
  - x86_64-pc-windows-msvc
  - x86_64-unknown-linux-gnu
  - x86_64-apple-darwin
  - aarch64-apple-darwin
- Default target is host triple.

## 10. Diagnostics

Every diagnostic must include:

- severity (`Error`/`Warning`/`Note`/`InternalError`)
- stable code
- message
- source file and precise span (line/column/range)
- optional suggestion

## 11. Debug Facilities (Required)

Driver must support:

- `--emit-tokens`
- `--emit-ast`
- `--emit-sema`
- `--emit-sir`
- `--emit-cfg`
- `--emit-llvm`

## 12. Serialization and Introspection

- AST/Sema/SIR/CFG/Diagnostic structures must support debug dump (text or JSON).
- Dumps must be deterministic to support CI and diff-based debugging.

## 13. Testing and Differential Validation

- C++ unit/CLI/compliance gates are mandatory for all PRs.
- Deterministic dumps and validator checks are the primary regression signals in v1.

## 14. Change Control

- Any change touching ABI, mangling, ownership semantics, or SIR instruction semantics requires spec update in `docs/` within the same PR.
- Any command-line compatibility change requires CLI docs update.
