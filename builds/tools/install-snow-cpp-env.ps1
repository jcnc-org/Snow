param(
  [switch]$IncludeMsvc,
  [switch]$Force,
  [switch]$UseChocoFallback = $true
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Message) {
  Write-Host "[snow-env] $Message"
}

function Test-Command([string]$Name) {
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-IsAdmin {
  $id = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = [Security.Principal.WindowsPrincipal]::new($id)
  return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-WingetInstall {
  param(
    [Parameter(Mandatory = $true)][string]$Id,
    [string]$Extra = ''
  )

  if (-not (Test-Command 'winget')) {
    throw "winget not found"
  }

  $base = @(
    'install',
    '--id', $Id,
    '--exact',
    '--accept-package-agreements',
    '--accept-source-agreements',
    '--silent',
    '--disable-interactivity'
  )

  if ($Extra) {
    $tokens = $Extra -split ' '
    foreach ($t in $tokens) {
      if ($t.Trim().Length -gt 0) {
        $base += $t.Trim()
      }
    }
  }

  Write-Step "winget $($base -join ' ')"
  & winget @base
}

function Invoke-ChocoInstall {
  param(
    [Parameter(Mandatory = $true)][string]$Package,
    [string]$Extra = ''
  )

  if (-not (Test-Command 'choco')) {
    throw "choco not found"
  }

  $args = @('install', $Package, '-y', '--no-progress')
  if ($Extra) {
    $tokens = $Extra -split ' '
    foreach ($t in $tokens) {
      if ($t.Trim().Length -gt 0) {
        $args += $t.Trim()
      }
    }
  }

  Write-Step "choco $($args -join ' ')"
  & choco @args
}

function Ensure-Tool {
  param(
    [Parameter(Mandatory = $true)][string]$ToolName,
    [Parameter(Mandatory = $true)][string]$WingetId,
    [Parameter(Mandatory = $true)][string]$ChocoPackage,
    [string]$WingetExtra = '',
    [string]$ChocoExtra = ''
  )

  if ((Test-Command $ToolName) -and -not $Force) {
    Write-Step "$ToolName already present; skip"
    return
  }

  $ok = $false
  try {
    Invoke-WingetInstall -Id $WingetId -Extra $WingetExtra
    $ok = $true
  } catch {
    Write-Step "winget install failed for ${ToolName}: $($_.Exception.Message)"
  }

  if (-not $ok -and $UseChocoFallback) {
    try {
      Invoke-ChocoInstall -Package $ChocoPackage -Extra $ChocoExtra
      $ok = $true
    } catch {
      Write-Step "choco fallback failed for ${ToolName}: $($_.Exception.Message)"
    }
  }

  if (-not $ok) {
    throw "Failed to install required tool: $ToolName"
  }
}

function Refresh-Path {
  $machine = [Environment]::GetEnvironmentVariable('Path', 'Machine')
  $user = [Environment]::GetEnvironmentVariable('Path', 'User')
  $env:Path = "$machine;$user"
}

function Ensure-UserPathContains {
  param(
    [Parameter(Mandatory = $true)][string]$Directory
  )

  if (-not (Test-Path $Directory)) {
    return
  }

  $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
  if ([string]::IsNullOrWhiteSpace($userPath)) {
    $userPath = ''
  }

  $parts = $userPath -split ';' | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
  $normalizedTarget = $Directory.Trim().TrimEnd('\\')

  foreach ($part in $parts) {
    if ($part.TrimEnd('\\') -ieq $normalizedTarget) {
      return
    }
  }

  $newPath = if ($userPath.Trim().Length -eq 0) { $Directory } else { "$userPath;$Directory" }
  [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
  Write-Step "Added to User PATH: $Directory"
}

Write-Step "Starting Snow C++ environment bootstrap"
Write-Step "IncludeMsvc=$IncludeMsvc Force=$Force UseChocoFallback=$UseChocoFallback"

Ensure-Tool -ToolName 'cmake' -WingetId 'Kitware.CMake' -ChocoPackage 'cmake'
Ensure-Tool -ToolName 'ninja' -WingetId 'Ninja-build.Ninja' -ChocoPackage 'ninja'
Ensure-Tool -ToolName 'clang' -WingetId 'LLVM.LLVM' -ChocoPackage 'llvm'

if ($IncludeMsvc) {
  if (-not (Test-IsAdmin)) {
    Write-Step 'MSVC Build Tools installation usually requires Administrator. Re-run PowerShell as Administrator for this step.'
  }

  $msvcFound = Test-Command 'cl'
  if ($msvcFound -and -not $Force) {
    Write-Step 'cl already present; skip MSVC install'
  } else {
    $msvcOverride = '--override "--quiet --wait --norestart --nocache --installPath C:\\BuildTools --add Microsoft.VisualStudio.Workload.VCTools --add Microsoft.VisualStudio.Component.VC.Tools.x86.x64 --add Microsoft.VisualStudio.Component.Windows11SDK.22621"'
    $msvcInstalled = $false

    try {
      Invoke-WingetInstall -Id 'Microsoft.VisualStudio.2022.BuildTools' -Extra $msvcOverride
      $msvcInstalled = $true
    } catch {
      Write-Step "winget MSVC install failed: $($_.Exception.Message)"
    }

    if (-not $msvcInstalled -and $UseChocoFallback) {
      try {
        Invoke-ChocoInstall -Package 'visualstudio2022buildtools' -Extra '--package-parameters "--add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --passive --norestart"'
        $msvcInstalled = $true
      } catch {
        Write-Step "choco MSVC fallback failed: $($_.Exception.Message)"
      }
    }

    if (-not $msvcInstalled) {
      Write-Step 'MSVC Build Tools not installed automatically. Install manually:'
      Write-Step 'winget install --id Microsoft.VisualStudio.2022.BuildTools --exact --override "--add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --passive --norestart"'
    }
  }
}

Ensure-UserPathContains -Directory 'C:\Program Files\CMake\bin'
Ensure-UserPathContains -Directory 'C:\Program Files\LLVM\bin'

Refresh-Path

Write-Step 'Tool versions after install:'
try { & cmake --version } catch { Write-Step 'cmake unavailable in current session' }
try { & ninja --version } catch { Write-Step 'ninja unavailable in current session' }
try { & clang --version } catch { Write-Step 'clang unavailable in current session' }

if ($IncludeMsvc) {
  try {
    & cmd /c 'where cl'
  } catch {
    Write-Step 'cl not visible in current shell PATH yet (open Developer PowerShell after MSVC install).'
  }
}

Write-Step 'If current terminal still cannot find cmake/clang, either open a new terminal or run:'
Write-Step '. .\\builds\\tools\\enter-snow-cpp-env.ps1'
Write-Step 'Bootstrap completed.'
