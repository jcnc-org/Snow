---
name: compiler-layering
description: Enforce Snow compiler layering and dependency direction. Use when adding/moving files, changing module boundaries, introducing new targets, or reviewing architecture coupling across frontend/sema/ownership/sir/passes/codegen/runtime/driver/cli.
---

# Compiler Layering

## Enforce Layer Responsibilities

- Keep `frontend` limited to tokens/parser/AST concerns.
- Keep `sema` limited to symbol/type/import/visibility resolution.
- Keep `ownership` limited to move/lifetime/drop facts.
- Keep `sir` limited to IR model/build/validation.
- Keep `passes` limited to IR transforms.
- Keep `codegen/llvm` limited to lowering/object emission/LLVM target wiring.
- Keep `runtime` limited to C ABI runtime entry/helpers.
- Keep `driver`/`cli` limited to orchestration and user interface.

## Enforce Dependency Direction

Allowed dependency direction:

`common -> frontend -> sema -> ownership -> sir -> passes -> codegen/llvm -> driver -> cli`

Additional rule:

- `runtime` is standalone and may be consumed by link/orchestration flows.
- A layer must not include headers from a later/upstream layer.

## Apply CMake Target Boundaries

- Maintain one target per layer (`snow_common`, `snow_frontend`, `snow_sema`, `snow_ownership`, `snow_sir`, `snow_passes`, `snow_codegen_llvm`, `snow_runtime`, `snow_driver`).
- Link each target only to allowed lower layers.
- Keep include directories unified at repo `include/`, but enforce boundaries by target links and architecture check script.

## Split Policy

- If core file exceeds 800 lines, split by behavior:
  - parser: expressions/statements/module + dump utilities
  - driver: project graph, compile pipeline, artifact/toolchain
  - sir builder: emit/build vs dump/cfg helpers
  - lowering: textual lowering, LLVM init, object emission
- Preserve stable behavior first, then move code.

## Review Checklist

- No upward includes introduced.
- New symbols placed in correct layer namespaces.
- Target link graph remains acyclic.
- Architecture checker reports zero errors.
