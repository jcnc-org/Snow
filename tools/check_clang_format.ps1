param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $true

Set-Location (Resolve-Path $Root)

function Resolve-ClangFormat {
  $cmd = Get-Command clang-format -ErrorAction SilentlyContinue
  if ($cmd) {
    return $cmd
  }

  $envScript = "builds/tools/enter-snow-cpp-env.ps1"
  if (Test-Path $envScript) {
    . (Resolve-Path $envScript)
    $cmd = Get-Command clang-format -ErrorAction SilentlyContinue
    if ($cmd) {
      return $cmd
    }
  }

  return $null
}

$clangFormat = Resolve-ClangFormat
if (-not $clangFormat) {
  Write-Host "[format] FAIL: clang-format not found in PATH." -ForegroundColor Red
  exit 1
}

$files = @()
if (Get-Command rg -ErrorAction SilentlyContinue) {
  $files = rg --files src include tests tools | Where-Object { $_ -match "\.(cpp|h|hpp)$" }
} else {
  $files = Get-ChildItem src,include,tests,tools -Recurse -File -Include *.cpp,*.h,*.hpp | ForEach-Object { $_.FullName }
}

if (-not $files -or $files.Count -eq 0) {
  Write-Host "[format] PASS: no C++ files found."
  exit 0
}

$batchSize = 50
for ($i = 0; $i -lt $files.Count; $i += $batchSize) {
  $batch = $files[$i..([Math]::Min($i + $batchSize - 1, $files.Count - 1))]
  & $clangFormat.Source --dry-run --Werror @batch
  if ($LASTEXITCODE -ne 0) {
    Write-Host "[format] FAIL: clang-format mismatch detected." -ForegroundColor Red
    exit 1
  }
}

Write-Host "[format] PASS"
exit 0
