# Baseline Snapshot (Placeholder)

- Date: 2026-03-05
- Toolchain: LLVM 21.1.8
- Host target: expected from `snow::common::DetectHostTriple()`
- Input: `tests/data/minimal.snow`

Populate this file using the latest report produced by:

```powershell
powershell -ExecutionPolicy Bypass -File tools/run_perf_bench.ps1 -BuildDir build
```

Store representative phase timing numbers and compare drift in future refactors.
