# Snow SIR Specification v1.0

Status: Accepted

Date: 2026-03-04

## 1. Purpose

SIR (Snow Intermediate Representation) is the canonical middle-end IR between Snow AST/sema and LLVM IR.

SIR goals:

- strongly typed
- SSA form
- explicit control flow
- platform independent

## 2. Core Structure

```text
Module
  Function
    BasicBlock
      Instruction
```

### 2.1 Module

Contains:

- module name/path
- function list
- global declarations (future extension)

### 2.2 Function

Contains:

- symbol name (mangled or internal)
- function signature
- basic block sequence
- entry block id

### 2.3 BasicBlock

Contains:

- block label/id
- ordered instruction list
- exactly one terminator instruction at end

### 2.4 Instruction

Contains:

- opcode
- typed operands
- typed result (except void-like instructions)
- source span metadata (optional but recommended)

## 3. Type System

MVP scalar types:

- `i1` (boolean)
- `i32`
- `i64`
- `f32`
- `f64`
- `ptr<T>`

Aggregates:

- `struct<...>`
- `array<T, N>`

Function types:

- `(T1, T2, ...) -> R`

All values and instruction results are statically typed.

## 4. SSA Rules

- Every SSA value is assigned once.
- Uses must be dominated by definitions.
- Merge points must use `phi` where needed.
- Non-SSA memory effects are represented through explicit memory/lifetime instructions, not implicit state.

## 5. Control Flow Rules

Terminator opcodes:

- `br`
- `cond_br`
- `ret`
- `unreachable`

Rules:

- Each basic block must end with exactly one terminator.
- No instructions may appear after a terminator.
- `phi` instructions must appear at block beginning.

## 6. Instruction Set (MVP)

### 6.1 Arithmetic

- `add`
- `sub`
- `mul`
- `div`

### 6.2 Comparison

- `eq`
- `ne`
- `lt`
- `gt`
- `le`
- `ge`

### 6.3 Control

- `phi`
- `br`
- `cond_br`
- `ret`
- `unreachable`

### 6.4 Memory

- `alloc`
- `load`
- `store`

MVP lowering rule: mutable/local reassignment is represented through explicit `alloc/load/store`, not hidden state.

### 6.5 Memory / Lifetime

- `drop <value>`

`drop` semantics:

- Executes destructor/lifetime finalization for owned value.
- For heap owners: destructor then deallocation via runtime contract.
- For stack values: destructor only.
- Side-effecting; cannot be removed or moved across dependence boundaries unless proven safe.

### 6.6 Call

- `call`

### 6.7 Aggregate

- `extract`
- `insert`

## 7. Text Form

Example:

```text
fn add(a: i32, b: i32) -> i32
entry:
  %1 = add a, b
  ret %1
```

## 8. Ownership Interaction

Ownership checker provides facts used by SIR builder to insert `drop` and mark moved values.

Invariants:

- no use-after-move value may reach codegen
- each owned value has exactly-one drop along any executable path

## 9. Validation (sir-validator)

### 9.1 Mandatory Checks

- SSA correctness (single assignment, dominance, use-def)
- CFG validity (reachable blocks, terminator integrity, predecessor consistency)
- type consistency (operand/result compatibility)
- lifetime consistency (drop count/path consistency basic checks)

### 9.2 Execution Policy

- Debug builds: run after each pass
- Release builds: run at key checkpoints
  - post SIR build
  - post optimization pipeline
  - pre LLVM lowering

## 10. Serialization and Debugging

SIR must support deterministic textual dump for:

- `--emit-sir`
- test snapshots
- pass debugging

CFG dump (`--emit-cfg`) must list blocks and successor edges deterministically.

## 11. Forward Compatibility

New opcodes or type forms require:

- spec extension
- validator rule updates
- pass invariants update
- tests for positive/negative behavior
