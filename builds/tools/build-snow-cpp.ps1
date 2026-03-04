param(
  [string]$BuildDir = 'snow-cpp/build',
  [string]$Generator = 'Ninja',
  [switch]$Clean,
  [switch]$EnableLlvm
)

$ErrorActionPreference = 'Stop'

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

. "$PSScriptRoot\enter-snow-cpp-env.ps1"

if ($Clean -and (Test-Path $BuildDir)) {
  Remove-Item -Recurse -Force $BuildDir
}

$llvmFlag = if ($EnableLlvm) { 'ON' } else { 'OFF' }
cmake -S snow-cpp -B $BuildDir -G $Generator -DCMAKE_CXX_COMPILER=clang++ "-DSNOW_ENABLE_LLVM:BOOL=$($llvmFlag)"
cmake --build $BuildDir
ctest --test-dir $BuildDir --output-on-failure

Write-Host '[snow-build] Done.'
