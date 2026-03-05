param(
  [string]$Root = ".",
  [string]$BuildDir = "build",
  [switch]$SkipDeterminism,
  [switch]$RequireDeterminismBinary
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)
$repoRoot = (Resolve-Path ".").Path

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

function Resolve-SnowcBinary {
  $candidateExe = Join-Path $BuildDir "snowc.exe"
  $candidateBin = Join-Path $BuildDir "snowc"
  if (Test-Path $candidateExe) {
    return $candidateExe
  }
  if (Test-Path $candidateBin) {
    return $candidateBin
  }
  return $null
}

function Invoke-CompileDump([string]$Snowc, [string]$SourcePath, [string]$DumpSpec, [string]$OutputPath, [int]$ExpectedExit) {
  $cmd = "`"$Snowc`" compile --dump=$DumpSpec --no-artifact `"$SourcePath`" > `"$OutputPath`" 2>&1"
  cmd /c $cmd
  $actual = $LASTEXITCODE
  if ($actual -ne $ExpectedExit) {
    Add-Error "determinism check failed for '$SourcePath': expected exit=$ExpectedExit actual=$actual"
    return $false
  }
  return $true
}

function Normalize-Text([string]$Raw, [string]$RootPath) {
  $text = [regex]::Replace($Raw, [char]27 + '\[[0-9;]*[A-Za-z]', '')
  $text = $text -replace "`r`n", "`n"

  $rootNorm = $RootPath
  $rootForward = $RootPath -replace "\\", "/"
  $rootBackward = $RootPath -replace "/", "\\"

  $text = [regex]::Replace($text, [regex]::Escape($rootNorm), "<ROOT>", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  $text = [regex]::Replace($text, [regex]::Escape($rootForward), "<ROOT>", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
  $text = [regex]::Replace($text, [regex]::Escape($rootBackward), "<ROOT>", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)

  $lines = $text -split "`n" | Where-Object {
    $_ -notmatch "^artifact:" -and $_ -notmatch "^target:"
  }

  return (($lines -join "`n").TrimEnd())
}

function Get-DiagnosticSequence([string]$Text) {
  $sequence = @()
  $pattern = '(?ms)^(error|warning|note|internal-error)\[([^\]]+)\]:.*?\r?\n\s*-->\s*(.+?):(\d+):(\d+)'
  foreach ($m in [regex]::Matches($Text, $pattern)) {
    $sequence += "$($m.Groups[1].Value)|$($m.Groups[2].Value)|$($m.Groups[3].Value)|$($m.Groups[4].Value)|$($m.Groups[5].Value)"
  }
  return $sequence
}

function Check-TimingsPresence([string]$Snowc, [string]$SourcePath, [string]$OutputPath) {
  if (-not (Invoke-CompileDump $Snowc $SourcePath "timings" $OutputPath 0)) {
    return
  }

  $raw = Get-Content -Raw $OutputPath
  $normalized = Normalize-Text $raw $repoRoot

  $requiredPatterns = @(
    'timings \(phases\)',
    '^\s*lex:\s*',
    '^\s*parse:\s*',
    '^\s*sema:\s*',
    '^\s*ownership:\s*',
    '^\s*sir-build:\s*',
    '^\s*sir-validate-pre-pass:\s*',
    '^\s*passes:\s*',
    '^\s*llvm-lower:\s*',
    'timings \(passes\)'
  )

  foreach ($pattern in $requiredPatterns) {
    if (-not [regex]::IsMatch($normalized, $pattern, [System.Text.RegularExpressions.RegexOptions]::Multiline)) {
      Add-Error "determinism timings check failed: missing field pattern '$pattern'"
    }
  }
}

function Check-DeterministicDump {
  if ($SkipDeterminism) {
    return
  }

  $snowc = Resolve-SnowcBinary
  if (-not $snowc) {
    if ($RequireDeterminismBinary) {
      Add-Error "determinism check failed: snowc binary not found in $BuildDir"
      return
    }
    Add-Warning "determinism check skipped: snowc binary not found in $BuildDir"
    return
  }

  $tmp = Join-Path $BuildDir "arch-check"
  New-Item -ItemType Directory -Force $tmp | Out-Null

  $samples = @(
    [pscustomobject]@{ Name = "minimal"; Input = "tests/data/minimal.snow"; ExpectedExit = 0 },
    [pscustomobject]@{ Name = "if_phi"; Input = "tests/data/cases/if_phi.snow"; ExpectedExit = 0 },
    [pscustomobject]@{ Name = "while_cfg"; Input = "tests/data/cases/while_cfg.snow"; ExpectedExit = 0 },
    [pscustomobject]@{ Name = "star_import_warning"; Input = "tests/data/cases/star_import_warning.snow"; ExpectedExit = 0 },
    [pscustomobject]@{ Name = "lex_error"; Input = "tests/data/cases/lex_error.snow"; ExpectedExit = 1 }
  )

  foreach ($sample in $samples) {
    $outA = Join-Path $tmp ($sample.Name + "_a.txt")
    $outB = Join-Path $tmp ($sample.Name + "_b.txt")

    if (-not (Invoke-CompileDump $snowc $sample.Input "tokens,ast,sema,sir,cfg,llvm" $outA $sample.ExpectedExit)) {
      continue
    }
    if (-not (Invoke-CompileDump $snowc $sample.Input "tokens,ast,sema,sir,cfg,llvm" $outB $sample.ExpectedExit)) {
      continue
    }

    $normalizedA = Normalize-Text (Get-Content -Raw $outA) $repoRoot
    $normalizedB = Normalize-Text (Get-Content -Raw $outB) $repoRoot

    if ($normalizedA -ne $normalizedB) {
      Add-Error "deterministic dump check failed for sample '$($sample.Name)': normalized outputs differ"
    }

    $diagA = Get-DiagnosticSequence $normalizedA
    $diagB = Get-DiagnosticSequence $normalizedB
    if (($diagA -join "`n") -ne ($diagB -join "`n")) {
      Add-Error "diagnostic order/range determinism check failed for sample '$($sample.Name)'"
    }
  }

  $timingsOut = Join-Path $tmp "timings_minimal.txt"
  Check-TimingsPresence $snowc "tests/data/minimal.snow" $timingsOut
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
