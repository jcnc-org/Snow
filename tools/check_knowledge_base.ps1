param(
  [string]$Root = "."
)

$ErrorActionPreference = "Stop"

Set-Location (Resolve-Path $Root)

$requiredDocs = @(
  "docs/knowledge/llvm-21.1.8-authority.md",
  "docs/knowledge/runtime-abi-authority.md",
  "docs/knowledge/third-party-api-authority.md"
)

$requiredFields = @(
  "Source:",
  "Version:",
  "Retrieved:",
  "Why it matters:"
)

$failures = @()

foreach ($doc in $requiredDocs) {
  if (-not (Test-Path $doc)) {
    $failures += "missing knowledge doc: $doc"
    continue
  }

  $content = Get-Content -Raw $doc
  foreach ($field in $requiredFields) {
    if ($content -notmatch [regex]::Escape($field)) {
      $failures += "missing field '$field' in $doc"
    }
  }
}

$llvmDoc = "docs/knowledge/llvm-21.1.8-authority.md"
if (Test-Path $llvmDoc) {
  $llvmContent = Get-Content -Raw $llvmDoc
  if ($llvmContent -notmatch "21\.1\.8") {
    $failures += "LLVM authority doc must pin 21.1.8"
  }
}

$detectLlvm = "cmake/DetectLLVM.cmake"
if (Test-Path $detectLlvm) {
  $detectContent = Get-Content -Raw $detectLlvm
  if ($detectContent -notmatch "find_package\(LLVM 21\.1\.8 EXACT CONFIG REQUIRED\)") {
    $failures += "DetectLLVM.cmake must require LLVM 21.1.8 EXACT"
  }
} else {
  $failures += "missing cmake/DetectLLVM.cmake"
}

if ($failures.Count -gt 0) {
  foreach ($failure in $failures) {
    Write-Host "[knowledge] FAIL: $failure" -ForegroundColor Red
  }
  exit 1
}

Write-Host "[knowledge] PASS"
exit 0
