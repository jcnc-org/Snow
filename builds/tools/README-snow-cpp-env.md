# Snow C++ Environment Setup (Windows)

Script: `builds/tools/install-snow-cpp-env.ps1`

## Install core tools (recommended first)

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1
```

This installs/checks:

- CMake
- Ninja
- LLVM/Clang

If current terminal cannot find `cmake`/`clang` immediately after install, run:

```powershell
. .\builds\tools\enter-snow-cpp-env.ps1
```

## Install with MSVC Build Tools

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1 -IncludeMsvc
```

Note: MSVC installation usually requires Administrator PowerShell.

After MSVC installation, load `cl` environment into current PowerShell session:

```powershell
. .\builds\tools\enter-msvc-env.ps1
```

## Force reinstall

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1 -Force
```

## Build and test in one command

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/build-snow-cpp.ps1
```
