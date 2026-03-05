# Snow Compiler (C++ Mainline)

Snow v1.0 is now a C++20 + LLVM mainline compiler repository.

## Build

```bash
cmake -S . -B build -G Ninja -DCMAKE_CXX_COMPILER=clang++ -DSNOW_ENABLE_LLVM=ON
cmake --build build
ctest --test-dir build --output-on-failure
```

Windows environment setup:

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1
```

## Quality Gates

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/check-snow-v1-compliance.ps1
powershell -ExecutionPolicy Bypass -File builds/tools/run-snow-cpp-gate.ps1
```

## CLI

```bash
snowc version
snowc compile --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm input.snow
snowc build path/to/project-root
snowc run input.snow
snowc init
snowc clean
```

## Notes

- LLVM toolchain is locked to `21.1.8` for v1.
- Supported targets:
  - `x86_64-pc-windows-msvc`
  - `x86_64-unknown-linux-gnu`
  - `x86_64-apple-darwin`
  - `aarch64-apple-darwin`
- Governance and architecture specs live in `AGENTS.md` and `docs/`.
- Historical Java-era docs are archived under `docs/legacy/`.
