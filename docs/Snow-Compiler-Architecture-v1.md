# Snow Compiler Architecture v1.0 (C++ Migration)

Status: Accepted

Date: 2026-03-04

## 1. Overview

Snow v1.0 compiler is re-architected from Java+VM into a C++20 compiler with LLVM backend.

Core pipeline:

- Frontend: Lexer, Parser, AST
- Middle-end: Semantic analysis, ownership checking, SIR building, SIR validation, optimization passes
- Backend: LLVM lowering and native code generation

Primary backend route is fixed:

`AST -> SIR -> LLVM -> Object/Executable`

Legacy Java materials are archived as historical documentation only and are outside active build/test policy.

## 2. Compilation Pipeline

### 2.1 End-to-end Flow

```text
Source (.snow)
  -> Lexer
  -> Parser
  -> AST
  -> Semantic Analysis
  -> Ownership Check
  -> SIR Build
  -> sir-validator
  -> Optimization Passes (O0/O2)
  -> LLVM Lowering
  -> LLVM CodeGen
  -> Object / Executable
```

This overview is normative for contributors and tooling integration.

### 2.2 Stage Contracts

- Lexer: produce deterministic token stream with source spans.
- Parser: produce AST or syntax diagnostics.
- Semantic Analysis: resolve names/types/modules/visibility and emit semantic diagnostics.
- Ownership Check: enforce move/drop safety and compute lifetime facts.
- SIR Build: emit strongly-typed SSA with explicit CFG.
- sir-validator: verify SSA/CFG/type/lifetime invariants.
- Passes: transform SIR while preserving semantic equivalence.
- LLVM Lowering: map validated SIR into LLVM IR.
- LLVM CodeGen: produce object or executable for selected target.

## 3. Scope and Non-goals

### 3.1 In Scope

- Frontend, Sema, Ownership, SIR, validator, optimization passes
- LLVM backend and runtime ABI integration
- CLI and package manifest flow (`snow.toml`)
- Compiler debug emits and diagnostics

### 3.2 Out of Scope (v1.0)

- Implicit GC
- JIT
- Full IDE protocol surface
- Incremental compilation cache
- New custom VM backend work

## 4. Project Layout (repository root)

```text
include/snow/
  common/
  frontend/
  sema/
  ownership/
  sir/
  passes/
  codegen/
  driver/
  runtime/
src/
  frontend/
  sema/
  ownership/
  sir/
  passes/
  codegen/
  driver/
  runtime/
tools/
  cloud2toml/
tests/
```

Layer dependencies are one-way from driver to backend to middle-end to frontend.

## 5. Frontend Rules

### 5.1 Name Resolution Order

Resolution order is fixed:

`local -> current module -> imported modules`

### 5.2 Import Forms

Supported forms:

- `import math.vector`
- `import math.vector as vec`
- `import math.*`

`star import` is allowed but should emit warning `W_STAR_IMPORT_DISCOURAGED` by default.

### 5.3 Import Conflicts

If multiple imports expose the same unqualified symbol, compilation fails with `AmbiguousSymbol`.

### 5.4 Visibility

- `private` (default): current module only
- `internal`: package scope
- `pub`: exported across modules

### 5.5 Module Build Order

Module dependency graph is a DAG. Build order uses topological sort. Cycles are compile errors.

### 5.6 Function Statement Model

Function bodies are modeled as ordered statement lists in AST, not single ad-hoc control-flow fields.

MVP statements:

- `return <expr>;`
- `if <cond> { ... } else { ... }`
- `while <cond> { ... }`
- `break;`
- `continue;`
- `let <name>[: <type>] = <expr>;`
- `<name> = <expr>;`
- expression statement (`<expr>;`)

## 6. Memory and Ownership Model

- Local variables use stack semantics by default.
- Heap allocation is provided by runtime allocator APIs.
- No implicit GC in MVP.
- Non-copy types are move-only by default.
- Copy types (primitive scalars) may duplicate freely.

Drop semantics:

- Variables drop on scope exit in reverse declaration order.
- Using moved values is an error (`use-after-move`).
- Branch/loop control flow must prove exactly-once drop for owned values.
- Call arguments are pass-by-value; passing non-copy values consumes ownership.
- Assignment reinitializes target ownership from right-hand side value.

## 7. SIR and Validation

SIR is formally specified in `Snow-SIR-Spec-v1.md` and is mandatory for all middle-end work.

Validator checks:

- SSA correctness
- CFG validity
- Type consistency
- Lifetime/drop consistency

SIR builder uses a canonical function return block (`fn_return`) to merge return paths and keep drop insertion deterministic.

Validation policy:

- Debug: after every pass
- Release: at SIR build completion, after O2 pipeline, before LLVM lowering

## 8. Optimization Pipeline

### 8.1 O0

1. SIR Build
2. Canonicalize
3. sir-validator
4. LLVM Lowering

### 8.2 O2

1. SIR Build
2. sir-validator
3. ConstantFold
4. CFG Simplify
5. Copy Propagation
6. Dead Code Elimination
7. Inline (heuristic)
8. CFG Simplify
9. Dead Code Elimination
10. sir-validator
11. LLVM Lowering

Every pass must document input/output invariants and failure modes.

## 9. Runtime ABI and Entry

Runtime ABI details are normative in `Snow-Runtime-ABI-v1.md`.

Entry wrapping policy:

- Executable builds auto-generate host `main` wrapper
- Call chain: `host main -> snow_runtime_start -> user main`
- Library/object outputs must not auto-wrap entry

## 10. LLVM Target Strategy

Supported architectures:

- x86_64
- aarch64

Target triples:

- `x86_64-pc-windows-msvc`
- `x86_64-unknown-linux-gnu`
- `x86_64-apple-darwin`
- `aarch64-apple-darwin`

Default target is host triple.

LLVM version strategy:

- Locked toolchain: LLVM 21.1.8
- CI required matrix: LLVM 21.1.8 on supported host platforms

## 11. Symbol Mangling

Mangle format v1:

`_snow_<module_path>_<item_name>_<sig8>`

Rules:

- `<module_path>` replaces `.` with `_`
- any non-identifier character in module path (for example `:`, `/`, `\`, `-`) is normalized to `_`
- `<sig8>` is an 8-hex stable signature hash
- `extern "C"` disables mangling for FFI surface

## 12. Diagnostics

Severities:

- Error
- Warning
- Note
- InternalError

Required fields:

- code
- severity
- message
- file
- line
- column
- range
- suggestion (optional)

Example format:

```text
error: variable 'x' not defined
 --> main.snow:10:5
```

## 13. CLI Surface

Command names (compat retained):

- `compile`
- `run`
- `build`
- `init`
- `clean`
- `version`

Core compile flags:

- `--target`
- `--opt=0|2`
- `--emit-tokens`
- `--emit-ast`
- `--emit-sema`
- `--emit-sir`
- `--emit-cfg`
- `--emit-llvm`

## 14. Packaging and Manifest

- Canonical project manifest is `snow.toml`.
- Migration tool `cloud2toml` converts legacy cloud manifest into `snow.toml`.

## 15. Testing and Acceptance

Required test coverage areas:

- lexical and parser correctness
- semantic/name/import/visibility behavior
- ownership and drop checks
- SIR validator positive/negative cases
- O0/O2 semantic equivalence
- ABI behavior across targets
- entry wrapping policy validation

## 16. Governance

This architecture is enforced by repository root `AGENTS.md`.
Changes to ABI, SIR, ownership semantics, mangling, or CLI compatibility require spec updates in the same change set.
