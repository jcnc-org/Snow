# Snow Pass Invariants v1.0

Status: Accepted  
Date: 2026-03-05

This document defines pass-level contracts required by Snow v1.0.

## Canonicalize
- Input invariants: validated SIR module; each function has explicit CFG blocks.
- Output invariants: no semantic change; structure remains validator-compatible.
- Failure modes: invalid pre-state should be reported by `sir-validator` diagnostics.

## ConstantFold
- Input invariants: binary ops have type-consistent operands; literals are parseable.
- Output invariants: folded values preserve type and behavior; removed instructions are dead replacements.
- Failure modes: divide-by-zero fold is skipped; malformed instruction patterns are left unchanged.

## CfgSimplify
- Input invariants: block labels are unique; terminators are present.
- Output invariants: unreachable blocks removed; branch targets remain valid; phi inputs refer to existing predecessors.
- Failure modes: malformed control-flow may surface as validator errors after the pass.

## CopyPropagation
- Input invariants: load/store pointer relationships are explicit in SIR.
- Output invariants: replaced operands are transitively resolved aliases; side-effecting operations keep ordering.
- Failure modes: unknown aliasing patterns are conservatively ignored.

## DeadCodeElimination
- Input invariants: SSA def-use graph can be derived from instruction operands.
- Output invariants: only non-side-effecting unused defs are removed.
- Failure modes: if use-count computation is inconsistent, remaining validator checks must fail.

## Inline
- Input invariants: candidate callees are pure, small, and structurally analyzable.
- Output invariants: inlined body operands remapped to caller SSA names; no recursive self-inline.
- Failure modes: unsupported candidate shape is skipped; malformed remap must be detected by validator.
