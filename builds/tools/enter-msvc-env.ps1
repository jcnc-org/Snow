# Dot-source to import MSVC Build Tools environment variables into current PowerShell session.
# Usage:
#   . .\builds\tools\enter-msvc-env.ps1

$ErrorActionPreference = 'Stop'

$vswhere = 'C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe'
$installPath = $null

if (Test-Path $vswhere) {
  try {
    $installPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Workload.VCTools -property installationPath
    if ([string]::IsNullOrWhiteSpace($installPath)) {
      $installPath = $null
    }
  } catch {
    $installPath = $null
  }
}

if (-not $installPath) {
  if (Test-Path 'C:\BuildTools') {
    $installPath = 'C:\BuildTools'
  }
}

if (-not $installPath) {
  throw 'MSVC Build Tools installation not found.'
}

$vcvars = Join-Path $installPath 'VC\Auxiliary\Build\vcvars64.bat'
if (-not (Test-Path $vcvars)) {
  throw "vcvars64.bat not found at $vcvars"
}

$cmd = '"' + $vcvars + '" >nul && set'
$envDump = cmd /c $cmd

foreach ($line in $envDump) {
  $idx = $line.IndexOf('=')
  if ($idx -le 0) {
    continue
  }
  $name = $line.Substring(0, $idx)
  $value = $line.Substring($idx + 1)
  Set-Item -Path "Env:$name" -Value $value
}

Write-Host "[msvc-env] loaded from $installPath"
cmd /c "where cl"
cmd /c "cl /Bv" | Select-Object -First 8
