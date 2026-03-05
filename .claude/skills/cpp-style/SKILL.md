---
name: cpp-style
description: Apply Snow C++ coding style and comment policy. Use when editing C++ headers/sources, introducing APIs, enforcing ownership rules, or validating file-size and readability constraints.
---

# C++ Style

## Language and Ownership Rules

- Use C++20.
- Prefer RAII and Rule of Zero.
- Do not use owning raw pointers.
- Use `std::unique_ptr` by default for ownership transfer.
- Use `std::shared_ptr` only when shared ownership is required by design.

## Source Layout Rules

- Keep `include/` public interfaces minimal and stable.
- Keep implementation details in `src/`.
- Keep each file single-purpose and under line limits.
- Favor small helper types/functions over monolithic function bodies.

## File Size Rules

- `src/` and `include/`: `<= 800` lines.
- `tests/` and tooling scripts: `<= 1000` lines.
- Split before exceeding limits.

## Comment Policy

- Add module intent comment in substantial `.cpp` files:
  - `// Module: <short purpose>`
- Add key comments only where logic is non-obvious:
  - invariants
  - control-flow correctness requirements
  - ABI/LLVM assumptions
- Avoid noise comments that restate obvious code.

## API and Naming Rules

- Keep public interfaces explicit and narrow.
- Use descriptive names for pipeline stages and invariants.
- Make failure modes explicit in diagnostics and return structures.

## Formatting and Includes

- Format with repository `.clang-format`.
- Keep include order deterministic.
- Prefer project headers (`"snow/..."`) for internal modules.

## Pre-Merge Checklist

- Format check passes.
- Architecture check passes.
- No file exceeds size limits.
- New complex code contains key invariant comments.
