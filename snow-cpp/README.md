# snow-cpp

Snow compiler v1.0 C++ bootstrap implementation.

## Build

```bash
cmake -S snow-cpp -B snow-cpp/build
cmake --build snow-cpp/build
ctest --test-dir snow-cpp/build --output-on-failure
```

Windows one-click environment setup:

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1
```

Compliance check:

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/check-snow-v1-compliance.ps1
```

## Commands

```bash
snowc version
snowc compile --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm input.snow
snowc build path/to/project-root
snowc run input.snow
snowc init
snowc clean
```

## Notes

- v1 backend requires LLVM `21.1.8` and `SNOW_ENABLE_LLVM=ON`.
- Supported target triples are fixed to:
  - `x86_64-pc-windows-msvc`
  - `x86_64-unknown-linux-gnu`
  - `x86_64-apple-darwin`
  - `aarch64-apple-darwin`
- Architecture and constraints are defined by root `AGENTS.md` and v1 docs in `docs/`.
- Differential helper executable: `snow-diff-harness`.
- PR/local gate entrypoint: `powershell -ExecutionPolicy Bypass -File builds/tools/run-snow-cpp-gate.ps1 -JavaCmd "<java baseline command>"`.
