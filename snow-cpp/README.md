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

## Commands

```bash
snowc version
snowc compile --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm input.snow
snowc run input.snow
snowc build input.snow
snowc init
snowc clean
```

## Notes

- LLVM integration is currently a textual lowering stub unless `SNOW_ENABLE_LLVM=ON` and LLVM >= 17 is available.
- Architecture and constraints are defined by root `AGENTS.md` and v1 docs in `docs/`.
