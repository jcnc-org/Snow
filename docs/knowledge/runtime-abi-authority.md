# Runtime ABI Authority Reference (Snow v1)

- Version: Snow runtime ABI v1
- Retrieved: `2026-03-05`
- Scope: executable entry wrapping, call ABI, stack alignment, runtime C surface

## Entry 1: System V AMD64 ABI

- Source: https://refspecs.linuxbase.org/elf/x86_64-abi-0.99.pdf
- Version: SysV AMD64 ABI
- Retrieved: 2026-03-05
- Why it matters: Snow Linux/macOS x86_64 call conventions and stack alignment assumptions.

## Entry 2: Microsoft x64 ABI

- Source: https://learn.microsoft.com/cpp/build/x64-calling-convention
- Version: MSVC x64 calling convention docs
- Retrieved: 2026-03-05
- Why it matters: Snow Windows target `x86_64-pc-windows-msvc` ABI behavior.

## Entry 3: AArch64 Procedure Call Standard

- Source: https://github.com/ARM-software/abi-aa/blob/main/aapcs64/aapcs64.rst
- Version: AAPCS64
- Retrieved: 2026-03-05
- Why it matters: Snow Apple Silicon target ABI compatibility.

## Entry 4: Snow Runtime ABI Spec

- Source: ../Snow-Runtime-ABI-v1.md
- Version: Snow v1 accepted spec
- Retrieved: 2026-03-05
- Why it matters: Internal contract for `snow_runtime_start`, entry wrapping, and runtime helper surface.
