param(
    [string]$LogDir = (Join-Path $PSScriptRoot 'target\parallel-logs')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$winScript = Join-Path $PSScriptRoot 'release-windows.ps1'
$linScript = Join-Path $PSScriptRoot 'release-linux.ps1'

if (-not (Test-Path $winScript)) { throw "File not found: $winScript" }
if (-not (Test-Path $linScript)) { throw "File not found: $linScript" }

$winLogOut = [System.IO.Path]::GetTempFileName()
$winLogErr = [System.IO.Path]::GetTempFileName()
$linLogOut = [System.IO.Path]::GetTempFileName()
$linLogErr = [System.IO.Path]::GetTempFileName()

# Use only -NoNewWindow, remove -WindowStyle
$winProc = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$winScript`"") `
    -RedirectStandardOutput $winLogOut -RedirectStandardError $winLogErr -NoNewWindow -PassThru
$linProc = Start-Process powershell.exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$linScript`"") `
    -RedirectStandardOutput $linLogOut -RedirectStandardError $linLogErr -NoNewWindow -PassThru

$winPosOut = 0
$winPosErr = 0
$linPosOut = 0
$linPosErr = 0

Write-Host "===== Build Started ====="
while (-not $winProc.HasExited -or -not $linProc.HasExited) {
    # windows-release stdout
    if (Test-Path $winLogOut) {
        $size = (Get-Item $winLogOut).Length
        if ($size -gt $winPosOut) {
            $fs = [System.IO.File]::Open($winLogOut, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $fs.Position = $winPosOut
            $sr = New-Object System.IO.StreamReader($fs)
            while (!$sr.EndOfStream) {
                $line = $sr.ReadLine()
                if ($line) { Write-Host "[windows-release][OUT] $line" }
            }
            $winPosOut = $fs.Position
            $sr.Close()
            $fs.Close()
        }
    }
    # windows-release stderr
    if (Test-Path $winLogErr) {
        $size = (Get-Item $winLogErr).Length
        if ($size -gt $winPosErr) {
            $fs = [System.IO.File]::Open($winLogErr, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $fs.Position = $winPosErr
            $sr = New-Object System.IO.StreamReader($fs)
            while (!$sr.EndOfStream) {
                $line = $sr.ReadLine()
                if ($line) { Write-Host "[windows-release][ERR] $line" -ForegroundColor Red }
            }
            $winPosErr = $fs.Position
            $sr.Close()
            $fs.Close()
        }
    }
    # linux-release stdout
    if (Test-Path $linLogOut) {
        $size = (Get-Item $linLogOut).Length
        if ($size -gt $linPosOut) {
            $fs = [System.IO.File]::Open($linLogOut, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $fs.Position = $linPosOut
            $sr = New-Object System.IO.StreamReader($fs)
            while (!$sr.EndOfStream) {
                $line = $sr.ReadLine()
                if ($line) { Write-Host "[linux-release][OUT] $line" }
            }
            $linPosOut = $fs.Position
            $sr.Close()
            $fs.Close()
        }
    }
    # linux-release stderr
    if (Test-Path $linLogErr) {
        $size = (Get-Item $linLogErr).Length
        if ($size -gt $linPosErr) {
            $fs = [System.IO.File]::Open($linLogErr, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $fs.Position = $linPosErr
            $sr = New-Object System.IO.StreamReader($fs)
            while (!$sr.EndOfStream) {
                $line = $sr.ReadLine()
                if ($line) { Write-Host "[linux-release][ERR] $line" -ForegroundColor Red }
            }
            $linPosErr = $fs.Position
            $sr.Close()
            $fs.Close()
        }
    }
    Start-Sleep -Milliseconds 200
}

# After processes exit, print any remaining output
$tasks = @(
    @{proc=$winProc; log=$winLogOut; tag='windows-release'; type='OUT'; skip=$winPosOut},
    @{proc=$winProc; log=$winLogErr; tag='windows-release'; type='ERR'; skip=$winPosErr},
    @{proc=$linProc; log=$linLogOut; tag='linux-release'; type='OUT'; skip=$linPosOut},
    @{proc=$linProc; log=$linLogErr; tag='linux-release'; type='ERR'; skip=$linPosErr}
)
foreach ($item in $tasks) {
    if (Test-Path $item.log) {
        $fs = [System.IO.File]::Open($item.log, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
        $fs.Position = $item.skip
        $sr = New-Object System.IO.StreamReader($fs)
        while (!$sr.EndOfStream) {
            $line = $sr.ReadLine()
            if ($line) {
                if ($item.type -eq 'ERR') {
                    Write-Host "[$($item.tag)][ERR] $line" -ForegroundColor Red
                } else {
                    Write-Host "[$($item.tag)][OUT] $line"
                }
            }
        }
        $sr.Close()
        $fs.Close()
    }
}

Write-Host ""
Write-Host "All tasks completed successfully." -ForegroundColor Green

Remove-Item $winLogOut, $winLogErr, $linLogOut, $linLogErr -Force
exit 0
