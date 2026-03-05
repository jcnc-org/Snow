param(
  [string]$Root = ".",
  [string]$BuildDir = "build",
  [switch]$SkipDeterminism
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)

$errors = @()
$warnings = @()

function Add-Error([string]$Message) {
  $script:errors += $Message
}

function Add-Warning([string]$Message) {
  $script:warnings += $Message
}

function Get-LineCount([string]$Path) {
  return (Get-Content $Path | Measure-Object -Line).Lines
}

function Check-FileSizeLimits {
  $coreFiles = @()
  $testToolFiles = @()

  if (Get-Command rg -ErrorAction SilentlyContinue) {
    $coreFiles = rg --files src include -g "*.cpp" -g "*.h" -g "*.hpp"
    $testToolFiles = @()
    $testToolFiles += rg --files tests -g "*.cpp" -g "*.h" -g "*.hpp" -g "*.cmake"
    $testToolFiles += rg --files tools -g "*.ps1" -g "*.cpp" -g "*.h" -g "*.hpp"
    $testToolFiles += rg --files builds/tools -g "*.ps1" -g "*.md" -g "*.txt"
    $testToolFiles = $testToolFiles | Where-Object { $_ }
  } else {
    $coreFiles = Get-ChildItem src,include -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName }
    $testToolFiles = Get-ChildItem tests,tools,builds/tools -Recurse -File | ForEach-Object { $_.FullName }
  }

  foreach ($file in $coreFiles) {
    $lines = Get-LineCount $file
    if ($lines -gt 800) {
      Add-Error "core file exceeds 800 lines: $file ($lines)"
    }
  }

  foreach ($file in $testToolFiles) {
    $lines = Get-LineCount $file
    if ($lines -gt 1000) {
      Add-Error "tests/tools file exceeds 1000 lines: $file ($lines)"
    }
  }
}

function Check-CommentPolicy {
  $sourceFiles = @()
  if (Get-Command rg -ErrorAction SilentlyContinue) {
    $sourceFiles = rg --files src -g "*.cpp"
  } else {
    $sourceFiles = Get-ChildItem src -Recurse -File -Include *.cpp | ForEach-Object { $_.FullName }
  }

  foreach ($file in $sourceFiles) {
    $lines = Get-Content $file
    if ($lines.Count -ge 120) {
      $firstNonEmpty = ($lines | Where-Object { $_.Trim().Length -gt 0 } | Select-Object -First 1)
      if (-not $firstNonEmpty -or $firstNonEmpty -notmatch "^\s*//\s*Module:") {
        Add-Error "missing module intent comment in substantial source: $file"
      }
    }
    if ($lines.Count -ge 250) {
      $joined = $lines -join "`n"
      if ($joined -notmatch "//\s*(Invariant|Contract|Why):") {
        Add-Warning "consider adding invariant/contract comment marker in complex module: $file"
      }
    }
  }
}

function Check-LayerDependencies {
  $allowed = @{
    common    = @("common")
    frontend  = @("common", "frontend")
    sema      = @("common", "frontend", "sema")
    ownership = @("common", "frontend", "sema", "ownership")
    sir       = @("common", "frontend", "sema", "ownership", "sir")
    passes    = @("common", "sir", "passes")
    codegen   = @("common", "sir", "passes", "codegen")
    runtime   = @("common", "runtime")
    driver    = @("common", "frontend", "sema", "ownership", "sir", "passes", "codegen", "runtime", "driver")
    cli       = @("common", "driver", "cli")
  }

  $files = @()
  if (Get-Command rg -ErrorAction SilentlyContinue) {
    $files = rg --files src include/snow -g "*.cpp" -g "*.h" -g "*.hpp"
  } else {
    $files = Get-ChildItem src,include/snow -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName }
  }

  foreach ($file in $files) {
    $parts = $file -split "[\\/]"
    $from = $null
    if ($file -match "^[sS][rR][cC][\\/]" -and $parts.Count -ge 2) {
      $from = $parts[1]
    } elseif ($file -match "^[iI][nN][cC][lL][uU][dD][eE][\\/]snow[\\/]" -and $parts.Count -ge 3) {
      $from = $parts[2]
    }

    if (-not $from -or -not $allowed.ContainsKey($from)) {
      continue
    }

    foreach ($line in (Get-Content $file)) {
      if ($line -match '#include\s*[<"]snow/([^/]+)/') {
        $to = $Matches[1]
        if (-not ($allowed[$from] -contains $to)) {
          Add-Error "layer violation: $from -> $to in $file"
        }
      }
    }
  }
}

function Check-DeterministicDump {
  if ($SkipDeterminism) {
    return
  }

  $snowc = $null
  $candidateExe = Join-Path $BuildDir "snowc.exe"
  $candidateBin = Join-Path $BuildDir "snowc"
  if (Test-Path $candidateExe) {
    $snowc = $candidateExe
  } elseif (Test-Path $candidateBin) {
    $snowc = $candidateBin
  }

  if (-not $snowc) {
    Add-Warning "determinism check skipped: snowc binary not found in $BuildDir"
    return
  }

  $tmp = Join-Path $BuildDir "arch-check"
  New-Item -ItemType Directory -Force $tmp | Out-Null
  $outA = Join-Path $tmp "dump_a.txt"
  $outB = Join-Path $tmp "dump_b.txt"
  $input = "tests/data/minimal.snow"

  $cmdA = "`"$snowc`" compile --dump=sir,cfg,llvm --no-artifact `"$input`" > `"$outA`" 2>&1"
  cmd /c $cmdA
  if ($LASTEXITCODE -ne 0) {
    Add-Error "determinism check failed: first compile command exited with $LASTEXITCODE"
    return
  }

  $cmdB = "`"$snowc`" compile --dump=sir,cfg,llvm --no-artifact `"$input`" > `"$outB`" 2>&1"
  cmd /c $cmdB
  if ($LASTEXITCODE -ne 0) {
    Add-Error "determinism check failed: second compile command exited with $LASTEXITCODE"
    return
  }

  $normalize = {
    param([string]$path)
    return (Get-Content $path) |
      Where-Object { $_ -notmatch "^artifact:" } |
      Where-Object { $_ -notmatch "^target:" }
  }

  $a = & $normalize $outA
  $b = & $normalize $outB
  if (($a -join "`n") -ne ($b -join "`n")) {
    Add-Error "deterministic dump check failed: compile dumps differ across runs"
  }
}

Check-FileSizeLimits
Check-CommentPolicy
Check-LayerDependencies
Check-DeterministicDump

foreach ($w in $warnings) {
  Write-Host "[arch] WARN: $w" -ForegroundColor Yellow
}

if ($errors.Count -gt 0) {
  foreach ($e in $errors) {
    Write-Host "[arch] FAIL: $e" -ForegroundColor Red
  }
  Write-Host "[arch] FAILED ($($errors.Count) errors)" -ForegroundColor Red
  exit 1
}

Write-Host "[arch] PASS"
exit 0
