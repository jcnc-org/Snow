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

All Java/Maven release scripts have been removed. This directory now serves C++ mainline build and validation only.
