# Dot-source this script in PowerShell to activate Snow C++ toolchain in current session.
# Usage:
#   . .\builds\tools\enter-snow-cpp-env.ps1

$cmakeBin = 'C:\Program Files\CMake\bin'
$llvmSdkRoot = 'C:\Users\LukeK\.snow\toolchains\llvm-21.1.8\clang+llvm-21.1.8-x86_64-pc-windows-msvc'
$llvmBinCandidates = @(
  (Join-Path $llvmSdkRoot 'bin'),
  'C:\Program Files\LLVM\bin'
)

$llvmBin = $null
foreach ($candidate in $llvmBinCandidates) {
  if (Test-Path $candidate) {
    $llvmBin = $candidate
    break
  }
}

if (Test-Path $cmakeBin) {
  if (-not ($env:Path -split ';' | Where-Object { $_.TrimEnd('\\') -ieq $cmakeBin.TrimEnd('\\') })) {
    $env:Path = "$cmakeBin;$env:Path"
  }
}

if ($llvmBin -and (Test-Path $llvmBin)) {
  if (-not ($env:Path -split ';' | Where-Object { $_.TrimEnd('\\') -ieq $llvmBin.TrimEnd('\\') })) {
    $env:Path = "$llvmBin;$env:Path"
  }
}

$llvmRoot = if ($llvmBin) { Split-Path -Parent $llvmBin } else { $null }
if ($llvmRoot -and (Test-Path $llvmRoot)) {
  $env:SNOW_LLVM_SDK_ROOT = $llvmRoot
  $env:LLVM_ROOT = $llvmRoot

  $llvmDir = Join-Path $llvmRoot 'lib\cmake\llvm'
  if (Test-Path (Join-Path $llvmDir 'LLVMConfig.cmake')) {
    $env:LLVM_DIR = $llvmDir
    if ([string]::IsNullOrWhiteSpace($env:CMAKE_PREFIX_PATH)) {
      $env:CMAKE_PREFIX_PATH = $llvmDir
    } elseif (-not ($env:CMAKE_PREFIX_PATH -split ';' | Where-Object { $_.TrimEnd('\\') -ieq $llvmDir.TrimEnd('\\') })) {
      $env:CMAKE_PREFIX_PATH = "$llvmDir;$env:CMAKE_PREFIX_PATH"
    }
  }
}

Write-Host '[snow-env] Session PATH activated.'
if ($llvmRoot) { Write-Host "[snow-env] LLVM root: $llvmRoot" }
if ($env:LLVM_DIR) { Write-Host "[snow-env] LLVM_DIR: $($env:LLVM_DIR)" }
try { cmake --version | Select-Object -First 1 } catch { Write-Host '[snow-env] cmake not found.' }
try { clang --version | Select-Object -First 1 } catch { Write-Host '[snow-env] clang not found.' }
try { ninja --version } catch { Write-Host '[snow-env] ninja not found.' }
