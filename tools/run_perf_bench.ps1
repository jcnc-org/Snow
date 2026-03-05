param(
  [string]$BuildDir = "build",
  [string]$Input = "tests/data/minimal.snow",
  [string]$OutFile = "tests/perf/latest_timing_report.txt"
)

$ErrorActionPreference = "Stop"

$snowcExe = Join-Path $BuildDir "snowc.exe"
$snowcBin = Join-Path $BuildDir "snowc"
$snowc = $null
if (Test-Path $snowcExe) {
  $snowc = $snowcExe
} elseif (Test-Path $snowcBin) {
  $snowc = $snowcBin
}

if (-not $snowc) {
  Write-Host "[perf] FAIL: snowc not found in $BuildDir" -ForegroundColor Red
  exit 1
}

if (-not (Test-Path $Input)) {
  Write-Host "[perf] FAIL: input not found: $Input" -ForegroundColor Red
  exit 1
}

$dir = Split-Path -Parent $OutFile
if ($dir -and -not (Test-Path $dir)) {
  New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

& $snowc compile --dump=timings --no-artifact $Input > $OutFile 2>&1
if ($LASTEXITCODE -ne 0) {
  Write-Host "[perf] FAIL: snowc compile benchmark command failed" -ForegroundColor Red
  exit 1
}

Write-Host "[perf] PASS: wrote $OutFile"
exit 0
