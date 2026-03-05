param(
  [string]$BuildDir = 'build',
  [string]$Generator = 'Ninja'
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

Invoke-Step -Name 'configure' -Command {
  cmake -S . -B $BuildDir -G $Generator -DCMAKE_CXX_COMPILER=clang++ -DSNOW_ENABLE_LLVM:BOOL=ON
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

Invoke-Step -Name 'compliance' -Command {
  powershell -ExecutionPolicy Bypass -File builds/tools/check-snow-v1-compliance.ps1 -BuildDir $BuildDir
}

Write-Host '[snow-gate] PASS'
