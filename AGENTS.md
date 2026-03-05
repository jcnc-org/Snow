# Snow Compiler Governance Index (v1.0)

This file is the mandatory governance index for contributors and AI agents.
Detailed workflows and checklists are delegated to `.claude/skills/`.

## 1. Scope

- Applies to all active C++ compiler/runtime/driver/test tooling in this repository.
- Java/VM-era assets are archival only and must live under `docs/legacy/`.
- If local habits conflict with this file or referenced skills, this governance wins.

## 2. Architecture Hard Rules

- Fixed pipeline:
  `Source -> Lexer -> Parser -> AST -> Semantic Analysis -> Ownership Check -> SIR Build -> sir-validator -> Passes -> LLVM Lowering -> LLVM CodeGen -> Object/Executable`
- Fixed backend route: `AST -> SIR -> LLVM`.
- No custom VM backend in v1.

Layer responsibilities:

- `frontend`: tokenize/parse/AST only.
- `sema`: name/import/visibility/type resolution.
- `ownership`: move/lifetime/drop facts.
- `sir`: strongly typed SSA IR and validation.
- `passes`: isolated IR transforms.
- `codegen/llvm`: LLVM lowering and object emission.
- `runtime`: C ABI runtime surface.
- `driver` and `cli`: orchestration and CLI boundary.

Dependency direction is one-way (no upward dependency).

## 3. C++ and Code Quality Hard Rules

- Language: C++20.
- Prefer RAII and Rule of Zero.
- Owning raw pointers are forbidden.
- Use `std::unique_ptr`/`std::shared_ptr` only with explicit ownership intent.
- Formatting must follow repository `.clang-format`.
- Comments are required when logic is non-obvious:
  - module intent (`// Module:`) at source file top for substantial modules,
  - key invariant/contract comments on complex algorithms or state transitions.

## 4. File Size Limits (Enforced)

- Compiler core files (`src/`, `include/`) must stay `<= 800` lines.
- Tests and tool scripts (`tests/`, `tools/`, `builds/tools/`) must stay `<= 1000` lines.
- If over limit, split by concern immediately.

## 5. SIR / Pass / Ownership Constraints

- SIR must remain strongly typed, SSA, explicit CFG, platform-independent.
- Each pass must be isolated and document:
  - input invariants
  - output invariants
  - failure modes
- `drop` is side-effecting; unsafe removal/reorder is forbidden.
- `sir-validator` policy:
  - debug: after every pass
  - release: key checkpoints

## 6. ABI / LLVM / Target Constraints

- Runtime entry chain for executables:
  `host main -> snow_runtime_start -> user main`
- Entry wrapping applies to executables only.
- Call-boundary stack alignment: 16 bytes.
- Locked LLVM toolchain: `21.1.8` (exact).
- Supported targets:
  - `x86_64-pc-windows-msvc`
  - `x86_64-unknown-linux-gnu`
  - `x86_64-apple-darwin`
  - `aarch64-apple-darwin`

## 7. Diagnostics / Dumps / Determinism

Every diagnostic must include:

- severity (`Error`/`Warning`/`Note`/`InternalError`)
- stable code
- message
- precise source span (line/column/range)
- optional suggestion

Required debug dump surfaces:

- tokens
- ast
- sema
- sir
- cfg
- llvm
- timings

AST/Sema/SIR/CFG/diagnostic dumps must be deterministic.

## 8. Required Gates

Hard-fail gates for this repository:

- architecture/style check: `tools/arch_check.ps1`
- syntax alignment check: `tools/check_syntax_alignment.ps1`
- format check: `tools/check_clang_format.ps1`
- test gate: `builds/tools/run-snow-cpp-gate.ps1`
- compliance gate: `builds/tools/check-snow-v1-compliance.ps1`
- knowledge base integrity: `tools/check_knowledge_base.ps1`

## 9. Syntax Change Control

For any syntax behavior change in `lexer` / `parser` / `AST` / `sema`, the same change set must update:

- `docs/Snow-Language-Syntax-v1-zh.md`
- `docs/Snow-Language-Syntax-v1.manifest.json`
- corresponding tests (`tests/unit` and/or `tests/data/cli_cases.tsv` + case files)

`tools/check_syntax_alignment.ps1` is hard-fail and enforces alignment between syntax docs, manifest, implementation, and tests.

## 10. Change Control

The same change set must update specs/docs when changing:

- ABI or mangling
- ownership semantics
- SIR instruction semantics
- CLI compatibility
- pass invariants

## 11. Skills Index

Detailed process rules are in `.claude/skills/`:

- `compiler-layering`
- `cpp-style`
- `architecture-check`
- `testing-gate`
- `llvm-authority-kb`
- `refactor-roadmap`

When a task matches one of the above, consult that skill before editing.
