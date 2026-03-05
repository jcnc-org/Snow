param(
  [string]$BuildDir = 'build',
  [string]$OutputRoot = 'dist',
  [string]$Version = '',
  [string]$PackageName = '',
  [switch]$SkipGate,
  [switch]$SkipBuild,
  [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Invoke-Step {
  param(
    [Parameter(Mandatory = $true)][string]$Name,
    [Parameter(Mandatory = $true)][scriptblock]$Command
  )

  Write-Host "[snow-package] $Name"
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "Step failed: $Name (exit=$LASTEXITCODE)"
  }
}

function Require-Path {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$Label
  )

  if (-not (Test-Path $Path)) {
    throw "Missing ${Label}: $Path"
  }
}

function Copy-IfExists {
  param(
    [Parameter(Mandatory = $true)][string]$Path,
    [Parameter(Mandatory = $true)][string]$Destination
  )

  if (Test-Path $Path) {
    Copy-Item -Force $Path $Destination
  }
}

function Resolve-BinaryPath {
  param(
    [Parameter(Mandatory = $true)][string]$BuildDirPath,
    [Parameter(Mandatory = $true)][string]$NameBase
  )

  if ($env:OS -eq 'Windows_NT') {
    return Join-Path $BuildDirPath "$NameBase.exe"
  }
  return Join-Path $BuildDirPath $NameBase
}

function Detect-OsTag {
  if ($env:OS -eq 'Windows_NT') { return 'windows' }
  if ($PSVersionTable.PSVersion.Major -ge 6 -and $IsMacOS) { return 'macos' }
  return 'linux'
}

function Detect-ArchTag {
  $arch = $env:PROCESSOR_ARCHITECTURE
  if ([string]::IsNullOrWhiteSpace($arch)) {
    return 'x86_64'
  }
  switch ($arch.ToUpperInvariant()) {
    'AMD64' { return 'x86_64' }
    'X86' { return 'x86' }
    'ARM64' { return 'aarch64' }
    default { return $arch.ToLowerInvariant() }
  }
}

function Resolve-VersionTag {
  param(
    [Parameter(Mandatory = $true)][string]$SnowcPath,
    [string]$Override = ''
  )

  if (-not [string]::IsNullOrWhiteSpace($Override)) {
    return $Override.Trim()
  }

  $versionOut = & $SnowcPath version 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to query version from snowc"
  }

  $line = (($versionOut | Select-Object -First 1) -as [string]).Trim()
  if ($line -match '^snowc\s+v(.+)$') {
    return $Matches[1]
  }
  if ($line -match '^snowc\s+(.+)$') {
    return $Matches[1]
  }
  return ($line -replace '\s+', '-')
}

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root

. "$PSScriptRoot\enter-snow-cpp-env.ps1"

if (-not $SkipGate) {
  Invoke-Step -Name 'run gate' -Command {
    powershell -ExecutionPolicy Bypass -File "$PSScriptRoot\run-snow-cpp-gate.ps1" -BuildDir $BuildDir
  }
} elseif (-not $SkipBuild) {
  $buildArgs = @(
    '-ExecutionPolicy', 'Bypass',
    '-File', "$PSScriptRoot\build-snow-cpp.ps1",
    '-BuildDir', $BuildDir
  )
  if ($Clean) {
    $buildArgs += '-Clean'
  }

  Invoke-Step -Name 'build' -Command {
    powershell @buildArgs
  }
}

$buildDirPath = Resolve-Path $BuildDir
$snowcPath = Resolve-BinaryPath -BuildDirPath $buildDirPath -NameBase 'snowc'
$cloud2tomlPath = Resolve-BinaryPath -BuildDirPath $buildDirPath -NameBase 'cloud2toml'

Require-Path -Path $snowcPath -Label 'snowc binary'
Require-Path -Path $cloud2tomlPath -Label 'cloud2toml binary'

$versionTag = Resolve-VersionTag -SnowcPath $snowcPath -Override $Version
$osTag = Detect-OsTag
$archTag = Detect-ArchTag

$resolvedPackageName = if (-not [string]::IsNullOrWhiteSpace($PackageName)) {
  $PackageName.Trim()
} else {
  "snow-$versionTag-$osTag-$archTag"
}

$outputRootPath = if ([System.IO.Path]::IsPathRooted($OutputRoot)) {
  $OutputRoot
} else {
  Join-Path $root $OutputRoot
}
New-Item -ItemType Directory -Force $outputRootPath | Out-Null

$stageDir = Join-Path $outputRootPath $resolvedPackageName
$zipPath = Join-Path $outputRootPath "$resolvedPackageName.zip"

if (Test-Path $stageDir) {
  Remove-Item -Recurse -Force $stageDir
}
if (Test-Path $zipPath) {
  Remove-Item -Force $zipPath
}

New-Item -ItemType Directory -Force `
  (Join-Path $stageDir 'bin'), `
  (Join-Path $stageDir 'docs'), `
  (Join-Path $stageDir 'docs\knowledge'), `
  (Join-Path $stageDir 'tools'), `
  (Join-Path $stageDir 'lib') | Out-Null

Copy-Item -Force $snowcPath (Join-Path $stageDir 'bin')
Copy-Item -Force $cloud2tomlPath (Join-Path $stageDir 'bin')

$rootFiles = @('README.md', 'AGENTS.md', 'LICENSE', 'NOTICE')
foreach ($file in $rootFiles) {
  Copy-IfExists -Path (Join-Path $root $file) -Destination $stageDir
}

$docsToCopy = @(
  'Snow-Compiler-Architecture-v1.md',
  'Snow-Language-Syntax-v1-zh.md',
  'Snow-Language-Syntax-v1.manifest.json',
  'Snow-SIR-Spec-v1.md',
  'Snow-Runtime-ABI-v1.md',
  'Snow-Pass-Invariants-v1.md',
  'Snow-Migration-Plan-Java-to-CPP.md'
)
foreach ($doc in $docsToCopy) {
  Copy-IfExists -Path (Join-Path $root "docs\$doc") -Destination (Join-Path $stageDir 'docs')
}

$knowledgeDir = Join-Path $root 'docs\knowledge'
if (Test-Path $knowledgeDir) {
  Copy-Item -Recurse -Force (Join-Path $knowledgeDir '*') (Join-Path $stageDir 'docs\knowledge')
}

$toolScripts = @(
  'install-snow-cpp-env.ps1',
  'enter-snow-cpp-env.ps1',
  'package-snow-release.ps1'
)
foreach ($toolScript in $toolScripts) {
  Copy-IfExists -Path (Join-Path $PSScriptRoot $toolScript) -Destination (Join-Path $stageDir 'tools')
}

$libDir = Join-Path $root 'lib'
if (Test-Path $libDir) {
  Copy-Item -Recurse -Force (Join-Path $libDir '*') (Join-Path $stageDir 'lib')
}

$gitSha = ''
if (Get-Command git -ErrorAction SilentlyContinue) {
  $shaOut = git rev-parse --short HEAD 2>$null
  if ($LASTEXITCODE -eq 0) {
    $gitSha = ($shaOut | Select-Object -First 1)
  }
}

$releaseInfo = @(
  'Snow Release Package',
  "package: $resolvedPackageName",
  "version: $versionTag",
  "os: $osTag",
  "arch: $archTag",
  "created_utc: $((Get-Date).ToUniversalTime().ToString('o'))",
  "git_sha: $gitSha",
  "build_dir: $BuildDir",
  "gate_ran: $([bool](-not $SkipGate))"
)
Set-Content -NoNewline -Path (Join-Path $stageDir 'RELEASE_INFO.txt') -Value ($releaseInfo -join "`n")

Compress-Archive -Path $stageDir -DestinationPath $zipPath -Force

Write-Host "[snow-package] package dir: $stageDir"
Write-Host "[snow-package] package zip: $zipPath"
Write-Host "[snow-package] done"
