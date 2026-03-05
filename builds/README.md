# Builds

## Recommended Local Flow

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/install-snow-cpp-env.ps1
powershell -ExecutionPolicy Bypass -File builds/tools/build-snow-cpp.ps1
```

## Compliance and Gate

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/check-snow-v1-compliance.ps1
powershell -ExecutionPolicy Bypass -File builds/tools/run-snow-cpp-gate.ps1
```

## One-Click Release Package

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/package-snow-release.ps1
```

Fast local repackage (skip gate/build, package existing artifacts):

```powershell
powershell -ExecutionPolicy Bypass -File builds/tools/package-snow-release.ps1 -SkipGate -SkipBuild
```

All Java/Maven release scripts have been removed. This directory now serves C++ mainline build and validation only.
