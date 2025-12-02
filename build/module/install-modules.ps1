# Snow Module Auto Installation Script
# Purpose: Compile and install all modules into the local Maven repository in the correct dependency order.

param(
    [switch]$Clean = $false,      # Whether to run "clean" before build
    [switch]$SkipTests = $false   # Whether to skip tests
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# 0. Ensure running under PowerShell 7
& (Join-Path $PSScriptRoot '..\tools\ensure-pwsh7.ps1')

# 1. Detect JDK
$jdkHome = & (Join-Path $PSScriptRoot '..\tools\detect-jdk.ps1')
Write-Host "✓ JDK detected at: $jdkHome" -ForegroundColor Green

$env:JAVA_HOME = $jdkHome
$env:Path = ("{0};{1}" -f (Join-Path $jdkHome 'bin'), $env:Path)

# 2. Detect Maven
$mvnPath = & (Join-Path $PSScriptRoot '..\tools\detect-maven.ps1')
Write-Host "✓ Maven detected at: $mvnPath" -ForegroundColor Green

# 3. Colored output helpers
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

# 4. Locate project root (script is inside build/module/, so go up two levels)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

Write-Info "Project root: $projectRoot"

# 5. Define module list and dependency order
$modules = @(
    @{ name = "snow-common";   path = "snow-common" },
    @{ name = "snow-lexer";    path = "snow-lexer" },
    @{ name = "snow-parser";   path = "snow-parser" },
    @{ name = "snow-semantic"; path = "snow-semantic" },
    @{ name = "snow-ir";       path = "snow-ir" },
    @{ name = "snow-vm";       path = "snow-vm" },
    @{ name = "snow-backend";  path = "snow-backend" }
)

# 6. Build Maven argument list
$mavenArgs = @()

if ($Clean) {
    $mavenArgs += "clean"
}
$mavenArgs += "package", "install"

if ($SkipTests) {
    $mavenArgs += "-DskipTests"
}

Write-Info "Starting module installation (SkipTests: $SkipTests, Clean: $Clean)"
Write-Info "Command: mvn $( $mavenArgs -join ' ' )"
Write-Host ""

$failedModules = @()
$successCount = 0

# 7. Build and install each module
foreach ($module in $modules) {
    Write-Info "Processing module: $( $module.name )"

    $modulePath = Join-Path $projectRoot $module.path
    if (-not (Test-Path $modulePath)) {
        Write-Error-Custom "Module path does not exist: $modulePath"
        $failedModules += $module.name
        continue
    }

    Push-Location $modulePath

    try {
        & mvn $mavenArgs

        if ($LASTEXITCODE -eq 0) {
            Write-Success "$( $module.name ) installed successfully"
            $successCount++
        }
        else {
            Write-Error-Custom "$( $module.name ) installation failed (exit code: $LASTEXITCODE)"
            $failedModules += $module.name
        }
    }
    catch {
        Write-Error-Custom "$( $module.name ) execution error: $_"
        $failedModules += $module.name
    }
    finally {
        Pop-Location
    }

    Write-Host ""
}

# 8. Summary output
Write-Host "=" * 60
Write-Info "Installation Summary"
Write-Host "=" * 60

Write-Success "Successfully installed: $successCount / $( $modules.Count )"

if ($failedModules.Count -gt 0) {
    Write-Error-Custom "Failed modules: $( $failedModules -join ', ' )"
    exit 1
}
else {
    Write-Success "All modules installed successfully!"
    exit 0
}