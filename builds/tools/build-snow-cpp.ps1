param(
  [string]$BuildDir = 'snow-cpp/build',
  [string]$Generator = 'Ninja',
  [switch]$Clean
)

$ErrorActionPreference = 'Stop'

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

. "$PSScriptRoot\enter-snow-cpp-env.ps1"

if ($Clean -and (Test-Path $BuildDir)) {
  Remove-Item -Recurse -Force $BuildDir
}

cmake -S snow-cpp -B $BuildDir -G $Generator -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++
cmake --build $BuildDir
ctest --test-dir $BuildDir --output-on-failure

Write-Host '[snow-build] Done.'
