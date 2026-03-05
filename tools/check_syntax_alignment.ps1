param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)

$manifestPath = "docs/Snow-Language-Syntax-v1.manifest.json"
$syntaxDocPath = "docs/Snow-Language-Syntax-v1-zh.md"
$tokenPath = "include/snow/frontend/token.h"
$lexerPath = "src/frontend/lexer.cpp"
$astPath = "include/snow/frontend/ast.h"
$semaPath = "src/sema/sema.cpp"
$ownershipPath = "src/ownership/ownership.cpp"
$frontendPath = "src/frontend"
$cliManifestPath = "tests/data/cli_cases.tsv"
$unitTestDir = "tests/unit"

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

function Read-Raw([string]$Path) {
  return Get-Content -Raw $Path
}

function Get-ManifestArray([object]$Manifest, [string]$FieldName) {
  if (-not ($Manifest.PSObject.Properties.Name -contains $FieldName)) {
    Add-Failure "manifest missing field: $FieldName"
    return @()
  }

  $values = @()
  foreach ($value in $Manifest.$FieldName) {
    $values += [string]$value
  }
  return $values
}

function Compare-ExactArray([string]$FieldName, [string[]]$Expected, [string[]]$Actual) {
  if ($Expected.Count -ne $Actual.Count) {
    Add-Failure "$FieldName count mismatch: manifest=$($Expected.Count) code=$($Actual.Count)"
  }

  $max = [Math]::Max($Expected.Count, $Actual.Count)
  for ($i = 0; $i -lt $max; $i++) {
    $lhs = if ($i -lt $Expected.Count) { $Expected[$i] } else { "<missing>" }
    $rhs = if ($i -lt $Actual.Count) { $Actual[$i] } else { "<missing>" }
    if ($lhs -ne $rhs) {
      Add-Failure "${FieldName} mismatch at index ${i}: manifest='$lhs' code='$rhs'"
    }
  }
}

function Compare-ExactSet([string]$FieldName, [string[]]$Expected, [string[]]$Actual) {
  $expectedSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
  $actualSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)

  foreach ($item in $Expected) { [void]$expectedSet.Add($item) }
  foreach ($item in $Actual) { [void]$actualSet.Add($item) }

  foreach ($item in $expectedSet) {
    if (-not $actualSet.Contains($item)) {
      Add-Failure "$FieldName missing in code: $item"
    }
  }
  foreach ($item in $actualSet) {
    if (-not $expectedSet.Contains($item)) {
      Add-Failure "$FieldName extra in code: $item"
    }
  }
}

function Extract-EnumMembers([string]$Content, [string]$RegexPattern, [string]$Label) {
  $enumMatch = [regex]::Match($Content, $RegexPattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if (-not $enumMatch.Success) {
    Add-Failure "cannot locate enum body for $Label"
    return @()
  }

  $members = @()
  $memberMatches = [regex]::Matches($enumMatch.Groups["body"].Value, "(?m)^\s*([A-Za-z_][A-Za-z0-9_]*)\s*(?:,|$)")
  foreach ($m in $memberMatches) {
    $name = $m.Groups[1].Value
    if (-not [string]::IsNullOrWhiteSpace($name)) {
      $members += $name
    }
  }
  return $members
}

function Get-SourceFiles([string]$PathOrDir) {
  if (-not (Test-Path $PathOrDir)) {
    return @()
  }
  $item = Get-Item $PathOrDir
  if ($item.PSIsContainer) {
    return @(Get-ChildItem $PathOrDir -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName })
  }
  return @($item.FullName)
}

function Extract-DiagnosticCodesFromFiles([string[]]$Files) {
  $set = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
  foreach ($file in $Files) {
    $raw = Get-Content -Raw $file
    foreach ($match in [regex]::Matches($raw, "\b(?:E|W)_[A-Z0-9_]+\b|\bAmbiguousSymbol\b")) {
      [void]$set.Add($match.Value)
    }
  }
  return @($set)
}

function Parse-CliManifest([string]$Path) {
  $rows = @()
  $lines = Get-Content $Path | Select-Object -Skip 1
  foreach ($line in $lines) {
    if ([string]::IsNullOrWhiteSpace($line)) {
      continue
    }
    $cols = $line -split "`t", -1
    if ($cols.Count -lt 11) {
      Add-Failure "malformed cli_cases.tsv row (expected 11 columns): $line"
      continue
    }
    $rows += [pscustomobject]@{
      type = $cols[0]
      name = $cols[1]
      exe = $cols[2]
      exit = $cols[3]
      args = $cols[4].Trim('"')
      expect = $cols[5].Trim('"')
      reject = $cols[6].Trim('"')
    }
  }
  return $rows
}

function Parse-RestrictedFeatureRows([string]$DocContent) {
  $rows = @()
  $allLines = $DocContent -split "`r?`n"
  foreach ($line in $allLines) {
    if (-not $line.TrimStart().StartsWith("|")) {
      continue
    }
    $parts = @($line.Split("|") | ForEach-Object { $_.Trim() })
    if ($parts.Count -lt 8) {
      continue
    }

    # split keeps empty leading/trailing segments
    $op = $parts[1]
    $kind = $parts[2]
    $parser = $parts[3]
    $sema = $parts[4]
    $diag = $parts[5]
    $status = $parts[6]

    if ([string]::IsNullOrWhiteSpace($op) -or $op -eq "op" -or $op -match "^-+$") {
      continue
    }

    $rows += [pscustomobject]@{
      op = $op
      kind = $kind
      parser = $parser
      sema = $sema
      diagnostic_code = $diag
      status = $status
    }
  }
  return $rows
}

if (-not (Require-File $manifestPath)) {
  foreach ($f in $failures) {
    Write-Host "[syntax] FAIL: $f" -ForegroundColor Red
  }
  exit 1
}

$requiredFiles = @(
  $tokenPath,
  $lexerPath,
  $astPath,
  $semaPath,
  $ownershipPath,
  $syntaxDocPath,
  $cliManifestPath
)
foreach ($file in $requiredFiles) {
  [void](Require-File $file)
}

try {
  $manifest = Get-Content -Raw $manifestPath | ConvertFrom-Json
} catch {
  Add-Failure "failed to parse manifest JSON: $manifestPath"
}

$requiredManifestFields = @(
  "token_types",
  "keywords",
  "statement_kinds",
  "expr_kinds",
  "binary_ops",
  "parse_diagnostics",
  "sema_diagnostics",
  "ownership_diagnostics",
  "unit_only_diagnostics",
  "unsupported_or_gated_ops",
  "required_cases"
)
foreach ($field in $requiredManifestFields) {
  if ($manifest -and -not ($manifest.PSObject.Properties.Name -contains $field)) {
    Add-Failure "manifest missing field: $field"
  }
}

if ($failures.Count -gt 0) {
  foreach ($f in $failures) {
    Write-Host "[syntax] FAIL: $f" -ForegroundColor Red
  }
  exit 1
}

$tokenContent = Read-Raw $tokenPath
$lexerContent = Read-Raw $lexerPath
$astContent = Read-Raw $astPath
$semaContent = Read-Raw $semaPath
$ownershipContent = Read-Raw $ownershipPath
$syntaxDocContent = Read-Raw $syntaxDocPath
$cliRows = Parse-CliManifest $cliManifestPath

$tokenTypesFromCode = Extract-EnumMembers $tokenContent "enum\s+class\s+TokenType\s*\{(?<body>.*?)\};" "TokenType"
$statementKindsFromCode = Extract-EnumMembers $astContent "struct\s+Statement\s*\{.*?enum\s+class\s+Kind\s*\{(?<body>.*?)\};" "Statement::Kind"
$exprKindsFromCode = Extract-EnumMembers $astContent "struct\s+Expr\s*\{.*?enum\s+class\s+Kind\s*\{(?<body>.*?)\};" "Expr::Kind"
$binaryOpsFromCode = Extract-EnumMembers $astContent "enum\s+class\s+BinaryOp\s*\{(?<body>.*?)\};" "BinaryOp"

$keywordMatch = [regex]::Match($lexerContent, "kKeywords\s*=\s*\{(?<body>.*?)\};", [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (-not $keywordMatch.Success) {
  Add-Failure "cannot locate lexer keyword map"
  $keywordsFromCode = @()
} else {
  $keywordsFromCode = @()
  $keywordEntries = [regex]::Matches($keywordMatch.Groups["body"].Value, '\{\s*"([^"]+)"\s*,\s*TokenType::([A-Za-z0-9_]+)\s*\}')
  foreach ($entry in $keywordEntries) {
    $keywordsFromCode += $entry.Groups[1].Value
  }
}

$tokenTypesFromManifest = Get-ManifestArray $manifest "token_types"
$statementKindsFromManifest = Get-ManifestArray $manifest "statement_kinds"
$exprKindsFromManifest = Get-ManifestArray $manifest "expr_kinds"
$binaryOpsFromManifest = Get-ManifestArray $manifest "binary_ops"
$keywordsFromManifest = Get-ManifestArray $manifest "keywords"
$parseDiagnosticsManifest = Get-ManifestArray $manifest "parse_diagnostics"
$semaDiagnosticsManifest = Get-ManifestArray $manifest "sema_diagnostics"
$ownershipDiagnosticsManifest = Get-ManifestArray $manifest "ownership_diagnostics"
$unitOnlyDiagnosticsManifest = Get-ManifestArray $manifest "unit_only_diagnostics"

Compare-ExactArray "token_types" $tokenTypesFromManifest $tokenTypesFromCode
Compare-ExactArray "keywords" $keywordsFromManifest $keywordsFromCode
Compare-ExactArray "statement_kinds" $statementKindsFromManifest $statementKindsFromCode
Compare-ExactArray "expr_kinds" $exprKindsFromManifest $exprKindsFromCode
Compare-ExactArray "binary_ops" $binaryOpsFromManifest $binaryOpsFromCode

$frontendFiles = Get-SourceFiles $frontendPath
$semaFiles = Get-SourceFiles $semaPath
$ownershipFiles = Get-SourceFiles $ownershipPath

$parseDiagnosticsFromCode = @(Extract-DiagnosticCodesFromFiles $frontendFiles | Where-Object {
  $_ -like "E_PARSE_*" -or $_ -like "E_LEX_*"
} | Sort-Object -Unique)
$semaDiagnosticsFromCode = @(Extract-DiagnosticCodesFromFiles $semaFiles | Where-Object {
  $_ -eq "AmbiguousSymbol" -or $_ -like "E_SEMA_*" -or $_ -like "W_*"
} | Sort-Object -Unique)
$ownershipDiagnosticsFromCode = @(Extract-DiagnosticCodesFromFiles $ownershipFiles | Where-Object {
  $_ -like "E_OWNERSHIP_*"
} | Sort-Object -Unique)

Compare-ExactSet "parse_diagnostics" $parseDiagnosticsManifest $parseDiagnosticsFromCode
Compare-ExactSet "sema_diagnostics" $semaDiagnosticsManifest $semaDiagnosticsFromCode
Compare-ExactSet "ownership_diagnostics" $ownershipDiagnosticsManifest $ownershipDiagnosticsFromCode

$requiredDiagCodes = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
$knownDiagnosticCodes = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($d in $parseDiagnosticsManifest) { [void]$knownDiagnosticCodes.Add($d) }
foreach ($d in $semaDiagnosticsManifest) { [void]$knownDiagnosticCodes.Add($d) }
foreach ($d in $ownershipDiagnosticsManifest) { [void]$knownDiagnosticCodes.Add($d) }

foreach ($case in @($manifest.required_cases)) {
  if (-not ($case.PSObject.Properties.Name -contains "case_path")) {
    Add-Failure "required_cases item missing 'case_path'"
    continue
  }
  if (-not ($case.PSObject.Properties.Name -contains "expected")) {
    Add-Failure "required_cases item missing 'expected': $($case.case_path)"
    continue
  }

  $casePath = [string]$case.case_path
  $expected = [string]$case.expected
  $diagCode = if ($case.PSObject.Properties.Name -contains "diag_code") { [string]$case.diag_code } else { "" }

  if (-not (Test-Path $casePath)) {
    Add-Failure "required_cases missing file: $casePath"
    continue
  }

  $caseName = [System.IO.Path]::GetFileName($casePath)
  $matchingRows = @($cliRows | Where-Object { $_.args.Contains($caseName) })
  if ($matchingRows.Count -eq 0) {
    Add-Failure "required_cases missing CLI registration for: $caseName"
    continue
  }

  if (-not [string]::IsNullOrWhiteSpace($expected)) {
    $expectedRows = @($matchingRows | Where-Object { $_.expect.Contains($expected) -or $_.reject.Contains($expected) })
    if ($expectedRows.Count -eq 0) {
      Add-Failure "required_cases expected text not found in cli_cases.tsv for ${caseName}: $expected"
    }
  }

  if (-not [string]::IsNullOrWhiteSpace($diagCode)) {
    [void]$requiredDiagCodes.Add($diagCode)
    $diagRows = @($matchingRows | Where-Object { $_.expect.Contains($diagCode) -or $_.reject.Contains($diagCode) })
    if ($diagRows.Count -eq 0) {
      Add-Failure "required_cases diag_code not found in cli_cases.tsv for ${caseName}: $diagCode"
    }
  }
}

foreach ($code in $requiredDiagCodes) {
  if (-not $knownDiagnosticCodes.Contains($code)) {
    Add-Failure "required_cases diag_code is not in parse/sema/ownership diagnostics: $code"
  }
}

$coverageSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
foreach ($code in $knownDiagnosticCodes) {
  [void]$coverageSet.Add($code)
}
foreach ($unitOnly in $unitOnlyDiagnosticsManifest) {
  if (-not $knownDiagnosticCodes.Contains($unitOnly)) {
    Add-Failure "unit_only_diagnostics code not in known diagnostics: $unitOnly"
  }
  [void]$coverageSet.Remove($unitOnly)
}

foreach ($code in $coverageSet) {
  if (-not $requiredDiagCodes.Contains($code)) {
    Add-Failure "required_cases missing diagnostic coverage for: $code"
  }
}

foreach ($code in $unitOnlyDiagnosticsManifest) {
  if ($requiredDiagCodes.Contains($code)) {
    Add-Failure "unit_only diagnostic must not be in required_cases: $code"
  }
}

$unitTestFiles = Get-ChildItem $unitTestDir -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName }
$unitCombined = ""
foreach ($file in $unitTestFiles) {
  $unitCombined += (Get-Content -Raw $file)
  $unitCombined += "`n"
}

foreach ($code in $unitOnlyDiagnosticsManifest) {
  if (-not $unitCombined.Contains($code)) {
    Add-Failure "unit_only diagnostic missing unit-test assertion text: $code"
  }
}

foreach ($item in @($manifest.unsupported_or_gated_ops)) {
  foreach ($field in @("op", "kind", "diagnostic_code", "status")) {
    if (-not ($item.PSObject.Properties.Name -contains $field)) {
      Add-Failure "unsupported_or_gated_ops item missing '$field'"
    }
  }

  if (-not ($item.PSObject.Properties.Name -contains "diagnostic_code")) {
    continue
  }

  $opText = [string]$item.op
  $diagCode = [string]$item.diagnostic_code
  $status = [string]$item.status
  $kind = [string]$item.kind

  if (-not [string]::IsNullOrWhiteSpace($diagCode) -and -not $semaContent.Contains($diagCode)) {
    Add-Failure "sema alignment mismatch: diagnostic code '$diagCode' not found in $semaPath"
  }
  if (-not [string]::IsNullOrWhiteSpace($opText) -and -not $semaContent.Contains($opText)) {
    Add-Failure "sema alignment mismatch: op marker '$opText' not found in $semaPath"
  }
  if (-not $requiredDiagCodes.Contains($diagCode)) {
    Add-Failure "unsupported_or_gated_ops diagnostic code not covered by required_cases: $diagCode"
  }

  $rowsWithDiag = @($cliRows | Where-Object { $_.expect.Contains($diagCode) -or $_.reject.Contains($diagCode) })
  if ($rowsWithDiag.Count -eq 0) {
    Add-Failure "unsupported_or_gated_ops diagnostic code missing in cli_cases.tsv: $diagCode"
  }

  $restrictedRows = Parse-RestrictedFeatureRows $syntaxDocContent
  $matchingRestricted = @($restrictedRows | Where-Object { $_.op -eq $opText })
  if ($matchingRestricted.Count -eq 0) {
    Add-Failure "syntax doc restricted feature row missing for op: $opText"
  } else {
    $row = $matchingRestricted[0]
    if ($row.kind -ne $kind) {
      Add-Failure "syntax doc restricted feature kind mismatch for ${opText}: doc='$($row.kind)' manifest='$kind'"
    }
    if ($row.diagnostic_code -ne $diagCode) {
      Add-Failure "syntax doc restricted feature diagnostic mismatch for ${opText}: doc='$($row.diagnostic_code)' manifest='$diagCode'"
    }
    if ($row.status -ne $status) {
      Add-Failure "syntax doc restricted feature status mismatch for ${opText}: doc='$($row.status)' manifest='$status'"
    }
    if ([string]::IsNullOrWhiteSpace($row.parser) -or [string]::IsNullOrWhiteSpace($row.sema)) {
      Add-Failure "syntax doc restricted feature parser/sema state missing for op: $opText"
    }
  }
}

if ($syntaxDocContent -notmatch "Snow-Diagnostics-v1\.manifest\.json") {
  Add-Failure "syntax doc missing diagnostics manifest reference: docs/Snow-Diagnostics-v1.manifest.json"
}

if ($failures.Count -gt 0) {
  foreach ($f in $failures) {
    Write-Host "[syntax] FAIL: $f" -ForegroundColor Red
  }
  Write-Host "[syntax] FAILED ($($failures.Count) issues)" -ForegroundColor Red
  exit 1
}

Write-Host "[syntax] PASS"
exit 0
