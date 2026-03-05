param(
  [string]$BuildDir = 'build',
  [string]$Generator = 'Ninja',
  [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Step {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][scriptblock]$Command
  )

  Write-Host "[snow-build] $Name"
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "Step failed: $Name (exit=$LASTEXITCODE)"
  }
}

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

. "$PSScriptRoot\enter-snow-cpp-env.ps1"

if ($Clean -and (Test-Path $BuildDir)) {
  Remove-Item -Recurse -Force $BuildDir
}

Invoke-Step -Name 'configure' -Command {
  cmake -S . -B $BuildDir -G $Generator -DCMAKE_CXX_COMPILER=clang++ -DSNOW_ENABLE_LLVM:BOOL=ON
}

Invoke-Step -Name 'build' -Command {
  cmake --build $BuildDir
}

Invoke-Step -Name 'test' -Command {
  ctest --test-dir $BuildDir --output-on-failure
}

Write-Host '[snow-build] Done.'
