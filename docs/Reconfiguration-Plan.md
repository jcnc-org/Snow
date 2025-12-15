## Snow Reconfiguration Plan (Compiler + VM)

This document is a staged, end-to-end plan to move Snow toward a stable, portable, and extensible layered architecture.

### Phase 0 — Correctness & Safety Gates (must ship first)

- Enforce correct boolean condition typing (`if`/`loop` conditions must be `boolean`).
- Make call results safe to ignore: any non-`void` call used in statement position must discard its return value (emit `POP`).
- Make syscall error state thread-local (no global errno races).
- On syscall failure, return a value in the same ABI category (or nothing for `VOID`) and best-effort repair stack delta.
- Remove “unknown syscall ⇒ default I32” behavior from codegen; require explicit ABI specs for any syscall opcode used by the compiler.
- Make arrays invariant at the type level (except `any[]` as a dynamic surface).

### Phase 1 — Freeze “Snow Core” Runtime Value Model (portable contract)

- Define and enforce the runtime value model:
  - `Value` types are the only values on the operand stack.
  - Heap objects are opaque references (`RefValue`) with a closed set of kinds (string/bytes/array/dict/…).
  - No host containers (`List/Map/byte[]`) cross the syscall ABI boundary.
- Convert remaining syscalls that still accept host containers to consume Snow heap objects (array/dict/string) instead.
- Add a bytecode verifier that checks:
  - stack effects
  - call/syscall argument counts
  - syscall return category correctness

### Phase 2 — Fix the “Struct == Array” Boundary (largest architectural change)

- Introduce a dedicated heap kind for struct instances, e.g. `STRUCT`.
- Add runtime operations for struct allocation and field access (prefer VM opcodes or runtime intrinsics over OS syscalls):
  - `OBJ_NEW(typeId, fieldCount)`
  - `OBJ_GET(obj, fieldIndex)`
  - `OBJ_SET(obj, fieldIndex, value)`
- Make the compiler lower `new T(...)` and `obj.field` to these object operations, never to array syscalls.

### Phase 3 — Type System: Dict + Nullable + Container Soundness

- Add `DictType` as a first-class type and stop treating `map` as `any`.
- Add `T?` (nullable) and use it for syscalls like `GETENV`.
- Define string semantics explicitly (`length` in bytes vs code points) and keep conversions explicit (`STR_TO_UTF8`, `UTF8_TO_STR`).
- Keep arrays invariant; if covariance is needed, introduce explicit `slice`/view types or runtime-checked casts.

### Phase 4 — Syscall Surface: Tiering for Portability

- Split syscalls into tiers:
  - `core.*` runtime builtins (language semantics)
  - `os.*` portable OS abstraction
  - `posix.*` / `linux.*` / `win.*` extensions (not part of the frozen portable ABI)
- Generate `SyscallTable` + docs from a single source of truth.

### Phase 5 — Backend Independence (JVM + Native + AOT)

- Define a canonical binary bytecode format (keep text format as assembler/debug).
- Remove JVM-only behaviors from ISA semantics (no `BigInteger`, no implicit UTF-8 decode for bytes-to-string printing in core opcodes).
- Make memory model assumptions explicit (GC barriers, reference identity, threading model).

