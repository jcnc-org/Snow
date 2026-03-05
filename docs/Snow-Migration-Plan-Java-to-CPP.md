# Snow Migration Plan: Java VM -> C++ LLVM (v1.0)

Status: Active

Date: 2026-03-04

## 1. Objective

Migrate Snow compiler implementation from Java+VM pipeline to C++20+LLVM native pipeline while preserving a measurable compatibility baseline through differential tests.

## 2. Baseline Strategy

- Java implementation remains reference behavior source during migration.
- New C++ implementation lives in `snow-cpp/`.
- Differential harness compares Java output A vs C++/LLVM output B.

Mismatch classification:

- `JAVA_BUG`
- `LLVM_BUG`
- `SEMANTIC_UNSPECIFIED`

## 3. Phased Delivery

### Phase 0: Spec Freeze and Governance

Deliver:

- `AGENTS.md`
- architecture/SIR/ABI/migration specs
- CI skeleton

Exit criteria:

- specs reviewed and accepted
- governance constraints enforceable

### Phase 1: C++ Project Bootstrap

Deliver:

- CMake project (`snow-cpp/`)
- core library targets
- CLI executable target
- optional LLVM integration switch

Exit criteria:

- clean configure + build on supported dev env

### Phase 2: Frontend MVP

Deliver:

- lexer/token stream
- parser/AST skeleton
- diagnostic plumbing

Exit criteria:

- parser test corpus runs
- `--emit-tokens` and `--emit-ast` available

### Phase 3: Semantic and Module Rules

Deliver:

- name resolution (`local -> module -> imports`)
- import alias/star support
- conflict diagnostics (`AmbiguousSymbol`)
- visibility checks

Exit criteria:

- semantic tests for imports/conflicts/visibility pass
- `--emit-sema` available

### Phase 4: Ownership and Drop

Deliver:

- move/use-after-move checks
- basic exactly-once drop path checks
- ownership facts export

Exit criteria:

- ownership unit tests pass

### Phase 5: SIR and Validator

Deliver:

- SIR data model and printer
- `drop` instruction support
- sir-validator with SSA/CFG/type/lifetime checks

Exit criteria:

- `--emit-sir` and `--emit-cfg` stable output
- validator negative tests pass

### Phase 6: Pass Pipeline (O0/O2)

Deliver:

- pass manager
- O0 and O2 fixed pipelines
- pass-level invariant declarations

Exit criteria:

- O0/O2 semantic consistency tests pass on corpus

### Phase 7: LLVM Lowering and Runtime ABI

Deliver:

- SIR->LLVM lowering
- target triple selection
- runtime ABI integration
- executable entry wrapping to `snow_runtime_start`

Exit criteria:

- object/executable emission works
- ABI smoke tests pass
- `--emit-llvm` available

### Phase 8: Package and Tooling

Deliver:

- `snow.toml` support
- `cloud2toml` conversion tool

Exit criteria:

- package manifest migration e2e tested

### Phase 9: Differential Hardening and Release

Deliver:

- differential harness in CI
- triage workflow for mismatches
- v1.0 release checklist

Exit criteria:

- target test matrix green
- open divergence backlog classified

## 4. Risk Register

- LLVM API drift across versions -> mitigated by pinning LLVM 21.1.8 in CI and local tooling
- semantics drift during migration -> mitigated by differential harness and deterministic dumps
- ownership bugs in complex CFG -> mitigated by sir-validator lifetime checks and negative tests
- cross-platform ABI regressions -> mitigated by ABI conformance tests per target

## 5. Acceptance Criteria

v1.0 is accepted when:

- all normative specs are implemented or explicitly marked deferred
- CLI supports required commands/emit flags
- O0 and O2 pipelines are functional and tested
- runtime entry behavior matches policy (exe-only wrapping)
- differential test workflow is active in CI

## 6. Rollout Notes

- Keep Java path available during migration for confidence.
- Document any semantic deltas explicitly as language v2 decisions.
- Treat ABI/mangling changes as breaking unless versioned.
