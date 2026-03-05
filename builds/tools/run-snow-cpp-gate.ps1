param(
  [string]$BuildDir = 'snow-cpp/build',
  [string]$Generator = 'Ninja',
  [string]$JavaCmd = '',
  [string]$SnowcPath = ''
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Step {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][scriptblock]$Command
  )

  Write-Host "[snow-gate] $Name"
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "Gate step failed: $Name (exit=$LASTEXITCODE)"
  }
}

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

. "$PSScriptRoot\enter-snow-cpp-env.ps1"

if ([string]::IsNullOrWhiteSpace($JavaCmd)) {
  if (-not [string]::IsNullOrWhiteSpace($env:SNOW_JAVA_CMD)) {
    $JavaCmd = $env:SNOW_JAVA_CMD
  } else {
    throw "SNOW_JAVA_CMD is required for PR diff gate. Pass -JavaCmd or set SNOW_JAVA_CMD."
  }
}

if ([string]::IsNullOrWhiteSpace($SnowcPath)) {
  if ($IsWindows) {
    $SnowcPath = Join-Path $BuildDir 'snowc.exe'
  } else {
    $SnowcPath = Join-Path $BuildDir 'snowc'
  }
}

if ($IsWindows) {
  $DiffHarnessPath = Join-Path $BuildDir 'snow-diff-harness.exe'
} else {
  $DiffHarnessPath = Join-Path $BuildDir 'snow-diff-harness'
}

Invoke-Step -Name 'configure' -Command {
  cmake -S snow-cpp -B $BuildDir -G $Generator -DCMAKE_CXX_COMPILER=clang++ -DSNOW_ENABLE_LLVM:BOOL=ON
}

Invoke-Step -Name 'build' -Command {
  cmake --build $BuildDir
}

Invoke-Step -Name 'unit-tests' -Command {
  ctest --test-dir "$BuildDir/tests" --output-on-failure -R "^(snow_unit_tests|snow_pass_tests|snow_sema_tests|snow_common_tests|snow_pass_docs_exist)$"
}

Invoke-Step -Name 'cli-tests' -Command {
  ctest --test-dir "$BuildDir/tests" --output-on-failure -R "^snowc_"
}

$reportPath = Join-Path $BuildDir 'tests/artifacts/diff_gate_report.json'
Invoke-Step -Name 'diff-gate' -Command {
  & $DiffHarnessPath --java-cmd $JavaCmd --snowc $SnowcPath --report $reportPath --fail-on-unclassified "snow-cpp/tests/data/minimal.snow"
}

Write-Host '[snow-gate] PASS'
