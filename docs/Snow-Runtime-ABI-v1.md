# Snow Runtime ABI Specification v1.0

Status: Accepted

Date: 2026-03-04

## 1. Purpose

Defines stable ABI contract between generated code and Snow runtime.

## 2. Calling Convention

Snow uses native platform calling conventions:

- x86_64 SysV (Linux/macOS)
- x86_64 MSVC (Windows)
- AArch64 PCS (arm64 targets)

General rules:

- arguments: registers first, overflow on stack
- integer/pointer return: `rax` (x86_64) / `x0` (aarch64)
- floating return: `xmm0` (x86_64) / `v0` (aarch64)
- stack alignment at call boundary: 16 bytes

## 3. Data Layout

- `bool`: 1 byte
- `i32`: 4 bytes
- `i64`: 8 bytes
- pointer: target pointer width
- struct: C-like layout + natural alignment

Struct rules:

- member offsets are alignment-aware
- struct alignment is max member alignment
- tail padding allowed

## 4. Symbol and Linkage

Default symbol mangling:

`_snow_<module_path>_<item_name>_<sig8>`

FFI boundary:

- `extern "C"` disables mangling
- FFI signatures must use C-compatible layout and calling convention

## 5. Runtime Entry Model

### 5.1 Executables

Compiler auto-generates host entry wrapper:

`host main -> snow_runtime_start(...) -> user main`

Runtime start responsibilities:

- initialize allocator
- initialize panic/error subsystem
- initialize stdlib runtime hooks
- invoke user entry
- perform teardown

### 5.2 Libraries and Object Files

No auto-generated entry wrapping.

## 6. Runtime Surface (MVP)

Required runtime APIs (symbol names indicative):

- `snow_runtime_start`
- `snow_alloc`
- `snow_free`
- `snow_panic`
- drop dispatch helpers

## 7. Target Matrix

Supported triples:

- `x86_64-pc-windows-msvc`
- `x86_64-unknown-linux-gnu`
- `x86_64-apple-darwin`
- `aarch64-apple-darwin`

Default target is host triple.

## 8. LLVM Version Policy

- Locked LLVM toolchain: 21.1.8
- CI matrix validates LLVM 21.1.8 on supported host platforms

## 9. Validation and Testing

Required tests:

- call/return ABI conformance for integer/float/pointer
- struct-by-value and struct-by-pointer layout tests
- stack alignment checks in generated call sites
- runtime entry wrapping tests (exe yes, lib no)

## 10. Compatibility Rules

Changes to calling convention assumptions, struct layout rules, or runtime entry semantics are breaking and require:

- ABI version note
- migration guidance
- cross-target test updates
