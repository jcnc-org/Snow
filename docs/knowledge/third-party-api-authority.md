# Third-Party API Authority Reference (Snow v1)

- Version: curated external API authority list
- Retrieved: `2026-03-05`
- Scope: APIs used by Snow build/test/runtime toolchain

## Entry 1: CMake

- Source: https://cmake.org/cmake/help/latest/
- Version: CMake 3.20+ policy in repository
- Retrieved: 2026-03-05
- Why it matters: target graph and test registration behavior for layered compiler build.

## Entry 2: clang / linker invocation

- Source: https://clang.llvm.org/docs/UsersManual.html
- Version: LLVM 21.x toolchain line
- Retrieved: 2026-03-05
- Why it matters: Snow driver shells out to `clang` and `llvm-ar` for artifact generation.

## Entry 3: PowerShell

- Source: https://learn.microsoft.com/powershell/scripting/
- Version: PowerShell 7+ syntax expectations
- Retrieved: 2026-03-05
- Why it matters: build and compliance gates are implemented as PowerShell scripts.
