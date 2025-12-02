# tools/ensure-pwsh7.ps1
Set-StrictMode -Version Latest

function Ensure-PowerShell7 {
    if ($PSVersionTable.PSEdition -ne 'Core') {
        Write-Host "Detected Windows PowerShell 5.x — switching to PowerShell 7..." -ForegroundColor Yellow

        $pwshCmd = Get-Command pwsh.exe -ErrorAction SilentlyContinue
        if (-not $pwshCmd) {
            throw "PowerShell 7 (pwsh) not found. Install from https://github.com/PowerShell/PowerShell"
        }

        & $pwshCmd.Source -NoLogo -NoProfile -File $PSCommandPath @args
        exit $LASTEXITCODE
    }
}

Ensure-PowerShell7