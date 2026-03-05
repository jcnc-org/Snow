param(
  [string]$BuildDir = 'build'
)

$ErrorActionPreference = 'Stop'

$checks = @()

function Add-Check([string]$Name, [bool]$Passed, [string]$Detail) {
  $script:checks += [pscustomobject]@{ Name = $Name; Passed = $Passed; Detail = $Detail }
}

function Invoke-Cmd([string]$CommandLine) {
  $out = cmd /c "$CommandLine 2>&1"
  $code = $LASTEXITCODE
  return [pscustomobject]@{ Output = ($out -join "`n"); ExitCode = $code }
}

$root = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $root
. "$PSScriptRoot\enter-snow-cpp-env.ps1"

$tmpRoot = Join-Path $root 'builds\tmp\compliance'
if (Test-Path $tmpRoot) {
  Remove-Item -Recurse -Force $tmpRoot
}
New-Item -ItemType Directory -Force $tmpRoot | Out-Null
Copy-Item -Recurse -Force 'tests\data\project_ok' (Join-Path $tmpRoot 'project_ok')
Copy-Item -Recurse -Force 'tests\data\project_cycle' (Join-Path $tmpRoot 'project_cycle')

$required = @(
  'AGENTS.md',
  'docs/Snow-Compiler-Architecture-v1.md',
  'docs/Snow-SIR-Spec-v1.md',
  'docs/Snow-Runtime-ABI-v1.md',
  'docs/Snow-Migration-Plan-Java-to-CPP.md',
  'docs/knowledge/llvm-21.1.8-authority.md',
  'docs/knowledge/runtime-abi-authority.md',
  'docs/knowledge/third-party-api-authority.md'
)
foreach ($f in $required) {
  $exists = Test-Path $f
  Add-Check "doc:$f" $exists ($(if ($exists) { 'ok' } else { 'missing' }))
}

$build = Invoke-Cmd "powershell -ExecutionPolicy Bypass -File builds\tools\build-snow-cpp.ps1 -BuildDir $BuildDir"
Add-Check 'build+ctest' ($build.ExitCode -eq 0) (($build.Output -split "`n" | Select-Object -Last 6) -join "`n")

$arch = Invoke-Cmd "powershell -ExecutionPolicy Bypass -File tools\arch_check.ps1 -BuildDir $BuildDir"
Add-Check 'architecture-check' ($arch.ExitCode -eq 0) 'expected architecture/style gates'

$format = Invoke-Cmd "powershell -ExecutionPolicy Bypass -File tools\check_clang_format.ps1"
Add-Check 'format-check' ($format.ExitCode -eq 0) 'expected clang-format clean state'

$kb = Invoke-Cmd "powershell -ExecutionPolicy Bypass -File tools\check_knowledge_base.ps1"
Add-Check 'knowledge-check' ($kb.ExitCode -eq 0) 'expected authority docs with required provenance'

$snowc = if ($IsWindows) { Join-Path $BuildDir 'snowc.exe' } else { Join-Path $BuildDir 'snowc' }

$emit = Invoke-Cmd "`"$snowc`" compile --emit-tokens --emit-ast --emit-sema --emit-sir --emit-cfg --emit-llvm -o $tmpRoot\\minimal.exe tests\\data\\minimal.snow"
$emitOk = $emit.ExitCode -eq 0 -and $emit.Output -match 'entry-wrapper = enabled'
Add-Check 'cli-emits' $emitOk 'expected emit pipeline + entry wrapper in llvm output'

$dag = Invoke-Cmd "`"$snowc`" build $tmpRoot\\project_ok"
$dagOk = $dag.ExitCode -eq 0 -and $dag.Output -match 'build modules: 2'
Add-Check 'module-dag' $dagOk 'expected 2 modules in topo build order'

$cycle = Invoke-Cmd "`"$snowc`" build $tmpRoot\\project_cycle"
$cycleOk = $cycle.ExitCode -ne 0 -and $cycle.Output -match 'E_MODULE_CYCLE'
Add-Check 'module-cycle-detect' $cycleOk 'expected E_MODULE_CYCLE on cyclic imports'

$llvm = Invoke-Cmd "`"$snowc`" compile --emit-llvm -o $tmpRoot\\minimal-llvm.exe tests\\data\\minimal.snow"
$mangleOk = $llvm.ExitCode -eq 0 -and $llvm.Output -match '_snow_'
$entryOk = $llvm.ExitCode -eq 0 -and $llvm.Output -match 'snow_runtime_start'
Add-Check 'symbol-mangling' $mangleOk 'expected _snow_ mangled symbols'
Add-Check 'runtime-entry-wrapper' $entryOk 'expected snow_runtime_start wrapper for executable output'

$failed = $checks | Where-Object { -not $_.Passed }
foreach ($c in $checks) {
  $mark = if ($c.Passed) { 'PASS' } else { 'FAIL' }
  Write-Host "[$mark] $($c.Name) :: $($c.Detail)"
}

if ($failed.Count -gt 0) {
  Write-Host "`nCompliance check FAILED: $($failed.Count) item(s)" -ForegroundColor Red
  exit 1
}

Write-Host "`nCompliance check PASSED" -ForegroundColor Green
exit 0
