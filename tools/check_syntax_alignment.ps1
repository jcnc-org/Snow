param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)

$manifestPath = "docs/Snow-Language-Syntax-v1.manifest.json"
$tokenPath = "include/snow/frontend/token.h"
$lexerPath = "src/frontend/lexer.cpp"
$astPath = "include/snow/frontend/ast.h"
$semaPath = "src/sema/sema.cpp"
$cliManifestPath = "tests/data/cli_cases.tsv"

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
  $mismatch = $false
  for ($i = 0; $i -lt $max; $i++) {
    $lhs = if ($i -lt $Expected.Count) { $Expected[$i] } else { "<missing>" }
    $rhs = if ($i -lt $Actual.Count) { $Actual[$i] } else { "<missing>" }
    if ($lhs -ne $rhs) {
      $mismatch = $true
      Add-Failure "${FieldName} mismatch at index ${i}: manifest='$lhs' code='$rhs'"
    }
  }

  if (-not $mismatch) {
    return
  }

  $missing = $Expected | Where-Object { $_ -notin $Actual }
  $extra = $Actual | Where-Object { $_ -notin $Expected }
  if ($missing.Count -gt 0) {
    Add-Failure "$FieldName missing in code: $($missing -join ', ')"
  }
  if ($extra.Count -gt 0) {
    Add-Failure "$FieldName extra in code: $($extra -join ', ')"
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
$cliLines = Get-Content $cliManifestPath | Select-Object -Skip 1

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
  $keywordEntries = [regex]::Matches($keywordMatch.Groups["body"].Value, "\{\s*""([^""]+)""\s*,\s*TokenType::([A-Za-z0-9_]+)\s*\}")
  foreach ($entry in $keywordEntries) {
    $keywordsFromCode += $entry.Groups[1].Value
  }
}

$tokenTypesFromManifest = Get-ManifestArray $manifest "token_types"
$statementKindsFromManifest = Get-ManifestArray $manifest "statement_kinds"
$exprKindsFromManifest = Get-ManifestArray $manifest "expr_kinds"
$binaryOpsFromManifest = Get-ManifestArray $manifest "binary_ops"
$keywordsFromManifest = Get-ManifestArray $manifest "keywords"

Compare-ExactArray "token_types" $tokenTypesFromManifest $tokenTypesFromCode
Compare-ExactArray "keywords" $keywordsFromManifest $keywordsFromCode
Compare-ExactArray "statement_kinds" $statementKindsFromManifest $statementKindsFromCode
Compare-ExactArray "expr_kinds" $exprKindsFromManifest $exprKindsFromCode
Compare-ExactArray "binary_ops" $binaryOpsFromManifest $binaryOpsFromCode

foreach ($item in $manifest.unsupported_or_gated_ops) {
  if (-not ($item.PSObject.Properties.Name -contains "op")) {
    Add-Failure "unsupported_or_gated_ops item missing 'op'"
    continue
  }
  if (-not ($item.PSObject.Properties.Name -contains "diagnostic_code")) {
    Add-Failure "unsupported_or_gated_ops item missing 'diagnostic_code'"
    continue
  }

  $opText = [string]$item.op
  $diagCode = [string]$item.diagnostic_code
  if (-not $semaContent.Contains($diagCode)) {
    Add-Failure "sema alignment mismatch: diagnostic code '$diagCode' not found in $semaPath"
  }
  if (-not [string]::IsNullOrWhiteSpace($opText) -and -not $semaContent.Contains($opText)) {
    Add-Failure "sema alignment mismatch: op marker '$opText' not found in $semaPath"
  }
}

foreach ($case in $manifest.required_cases) {
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
  $matchingRows = @($cliLines | Where-Object { $_.Contains($caseName) })
  if ($matchingRows.Count -eq 0) {
    Add-Failure "required_cases missing CLI registration for: $caseName"
    continue
  }

  if (-not [string]::IsNullOrWhiteSpace($expected)) {
    $expectedRows = @($matchingRows | Where-Object { $_.Contains($expected) })
    if ($expectedRows.Count -eq 0) {
      Add-Failure "required_cases expected text not found in cli_cases.tsv for ${caseName}: $expected"
    }
  }

  if (-not [string]::IsNullOrWhiteSpace($diagCode)) {
    $diagRows = @($matchingRows | Where-Object { $_.Contains($diagCode) })
    if ($diagRows.Count -eq 0) {
      Add-Failure "required_cases diag_code not found in cli_cases.tsv for ${caseName}: $diagCode"
    }
  }
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
