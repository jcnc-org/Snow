# Dot-source this script in PowerShell to activate Snow C++ toolchain in current session.
# Usage:
#   . .\builds\tools\enter-snow-cpp-env.ps1

$cmakeBin = 'C:\Program Files\CMake\bin'
$llvmBin = 'C:\Program Files\LLVM\bin'

if (Test-Path $cmakeBin) {
  if (-not ($env:Path -split ';' | Where-Object { $_.TrimEnd('\\') -ieq $cmakeBin.TrimEnd('\\') })) {
    $env:Path = "$cmakeBin;$env:Path"
  }
}

if (Test-Path $llvmBin) {
  if (-not ($env:Path -split ';' | Where-Object { $_.TrimEnd('\\') -ieq $llvmBin.TrimEnd('\\') })) {
    $env:Path = "$llvmBin;$env:Path"
  }
}

Write-Host '[snow-env] Session PATH activated.'
try { cmake --version | Select-Object -First 1 } catch { Write-Host '[snow-env] cmake not found.' }
try { clang --version | Select-Object -First 1 } catch { Write-Host '[snow-env] clang not found.' }
try { ninja --version } catch { Write-Host '[snow-env] ninja not found.' }
