param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)

$manifestPath = "docs/Snow-Diagnostics-v1.manifest.json"
$failures = @()

function Add-Failure([string]$Message) {
  $script:failures += $Message
}

function Require-File([string]$Path) {
  if (-not (Test-Path $Path)) {
    Add-Failure "missing required file: $Path"
    return $false
  }
  return $true
}

function Get-SourceFiles {
  if (Get-Command rg -ErrorAction SilentlyContinue) {
    return @(rg --files src include -g "*.cpp" -g "*.h" -g "*.hpp")
  }
  return @(Get-ChildItem src,include -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName })
}

function Add-ObservedSeverity([hashtable]$Observed, [string]$Code, [string]$Severity) {
  if (-not $Observed.ContainsKey($Code)) {
    $Observed[$Code] = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
  }
  [void]$Observed[$Code].Add($Severity)
}

if (-not (Require-File $manifestPath)) {
  foreach ($f in $failures) {
    Write-Host "[diag] FAIL: $f" -ForegroundColor Red
  }
  exit 1
}

try {
  $manifest = Get-Content -Raw $manifestPath | ConvertFrom-Json
} catch {
  Add-Failure "failed to parse manifest JSON: $manifestPath"
}

if ($manifest) {
  if (-not ($manifest.PSObject.Properties.Name -contains "naming_policy")) {
    Add-Failure "manifest missing field: naming_policy"
  }
  if (-not ($manifest.PSObject.Properties.Name -contains "entries")) {
    Add-Failure "manifest missing field: entries"
  }
}

if ($failures.Count -gt 0) {
  foreach ($f in $failures) {
    Write-Host "[diag] FAIL: $f" -ForegroundColor Red
  }
  exit 1
}

$allowedPrefixes = @()
if ($manifest.naming_policy.PSObject.Properties.Name -contains "allowed_prefixes") {
  $allowedPrefixes = @($manifest.naming_policy.allowed_prefixes | ForEach-Object { [string]$_ })
} else {
  Add-Failure "manifest naming_policy missing field: allowed_prefixes"
}

$legacyAliases = @()
if ($manifest.naming_policy.PSObject.Properties.Name -contains "legacy_aliases") {
  $legacyAliases = @($manifest.naming_policy.legacy_aliases | ForEach-Object { [string]$_ })
} else {
  Add-Failure "manifest naming_policy missing field: legacy_aliases"
}

$manifestCodes = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
$manifestSeverityByCode = @{}
$requiredEntryFields = @("code", "severity", "layer", "message_contract", "has_suggestion")

$validSeverities = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
[void]$validSeverities.Add("Error")
[void]$validSeverities.Add("Warning")
[void]$validSeverities.Add("Note")
[void]$validSeverities.Add("InternalError")

foreach ($entry in @($manifest.entries)) {
  foreach ($field in $requiredEntryFields) {
    if (-not ($entry.PSObject.Properties.Name -contains $field)) {
      Add-Failure "manifest entry missing field '$field'"
    }
  }

  if (-not ($entry.PSObject.Properties.Name -contains "code")) {
    continue
  }

  $code = [string]$entry.code
  if ([string]::IsNullOrWhiteSpace($code)) {
    Add-Failure "manifest entry code is empty"
    continue
  }

  if ($manifestCodes.Contains($code)) {
    Add-Failure "duplicate diagnostic code in manifest: $code"
    continue
  }
  [void]$manifestCodes.Add($code)

  $severity = [string]$entry.severity
  if (-not $validSeverities.Contains($severity)) {
    Add-Failure "invalid severity '$severity' for code: $code"
  }
  $manifestSeverityByCode[$code] = $severity

  if ($entry.PSObject.Properties.Name -contains "has_suggestion") {
    if ($entry.has_suggestion -isnot [bool]) {
      Add-Failure "field has_suggestion must be boolean for code: $code"
    }
  }

  foreach ($textField in @("layer", "message_contract")) {
    if ($entry.PSObject.Properties.Name -contains $textField) {
      $value = [string]$entry.$textField
      if ([string]::IsNullOrWhiteSpace($value)) {
        Add-Failure "field $textField cannot be empty for code: $code"
      }
    }
  }

  $hasAllowedPrefix = $false
  foreach ($prefix in $allowedPrefixes) {
    if ($code.StartsWith($prefix, [System.StringComparison]::Ordinal)) {
      $hasAllowedPrefix = $true
      break
    }
  }

  if (-not $hasAllowedPrefix) {
    if ($code -notin $legacyAliases) {
      Add-Failure "diagnostic code violates naming policy: $code"
    } elseif (-not ($entry.PSObject.Properties.Name -contains "legacy_alias") -or -not [bool]$entry.legacy_alias) {
      Add-Failure "legacy alias must set legacy_alias=true: $code"
    }
  }
}

$codeLiterals = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
$observedSeverity = @{}
$sourceFiles = Get-SourceFiles

foreach ($file in $sourceFiles) {
  $content = Get-Content -Raw $file

  foreach ($m in [regex]::Matches($content, '"(E_[A-Z0-9_]+|W_[A-Z0-9_]+|AmbiguousSymbol)"')) {
    [void]$codeLiterals.Add($m.Groups[1].Value)
  }

  foreach ($m in [regex]::Matches($content, 'diagnostics\.Error\s*\(\s*"(E_[A-Z0-9_]+|W_[A-Z0-9_]+|AmbiguousSymbol)"',
                                  [System.Text.RegularExpressions.RegexOptions]::Singleline)) {
    Add-ObservedSeverity $observedSeverity $m.Groups[1].Value "Error"
  }

  foreach ($m in [regex]::Matches($content, 'diagnostics\.Warning\s*\(\s*"(E_[A-Z0-9_]+|W_[A-Z0-9_]+|AmbiguousSymbol)"',
                                  [System.Text.RegularExpressions.RegexOptions]::Singleline)) {
    Add-ObservedSeverity $observedSeverity $m.Groups[1].Value "Warning"
  }
}

foreach ($code in $codeLiterals) {
  if (-not $manifestCodes.Contains($code)) {
    Add-Failure "code present in source but missing in manifest: $code"
  }
}

foreach ($code in $manifestCodes) {
  if (-not $codeLiterals.Contains($code)) {
    Add-Failure "code present in manifest but missing in source: $code"
  }
}

foreach ($code in $observedSeverity.Keys) {
  $observed = @($observedSeverity[$code])
  if ($observed.Count -gt 1) {
    Add-Failure "same diagnostic code observed with multiple severities in source: $code"
    continue
  }
  if (-not $manifestSeverityByCode.ContainsKey($code)) {
    continue
  }
  $manifestSeverity = $manifestSeverityByCode[$code]
  if ($manifestSeverity -ne $observed[0]) {
    Add-Failure "severity mismatch for ${code}: manifest=$manifestSeverity observed=$($observed[0])"
  }
}

if ($failures.Count -gt 0) {
  foreach ($f in $failures) {
    Write-Host "[diag] FAIL: $f" -ForegroundColor Red
  }
  Write-Host "[diag] FAILED ($($failures.Count) issues)" -ForegroundColor Red
  exit 1
}

Write-Host "[diag] PASS"
exit 0
