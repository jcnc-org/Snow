# Snow v1 Performance Baseline

This directory stores baseline-oriented performance artifacts for architecture-first refactors.

## Scope

- compile-phase timings from `snowc compile --dump=timings`
- pass timings from pass manager instrumentation

## Workflow

1. Build `snowc`.
2. Run:

```powershell
powershell -ExecutionPolicy Bypass -File tools/run_perf_bench.ps1 -BuildDir build
```

3. Compare generated report to `baseline.md`.

Maintainability-first policy:

- use this benchmark harness for regression visibility,
- defer aggressive optimization rewrites to dedicated performance waves.
