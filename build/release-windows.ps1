# release-windows.ps1

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-StrictMode -Version Latest

# Import shared dotenv parser function
. "$PSScriptRoot\tools\dotenv.ps1"

# ===== Utility Functions =====
function Find-PomUpwards([string]$startDir) {
    $dir = Resolve-Path $startDir
    while ($true) {
        $pom = Join-Path $dir "pom.xml"
        if (Test-Path $pom) { return $pom }
        $parent = Split-Path $dir -Parent
        if ($parent -eq $dir -or [string]::IsNullOrEmpty($parent)) { return $null }
        $dir = $parent
    }
}

# ===== Step 0: Generate .env =====
Write-Host "Step 0: Generate .env..."
try {
    & "$PSScriptRoot\tools\generate-dotenv.ps1" -ErrorAction Stop
} catch {
    Write-Error "Failed to generate .env: $($_.Exception.Message)"
    exit 1
}

# ===== Step 1: Locate project root & build =====
Write-Host "Step 1: Locate project root and build..."
$pom = Find-PomUpwards -startDir $PSScriptRoot
if (-not $pom) {
    Write-Error "pom.xml not found. Please run this script within the project."
    exit 1
}

$projectRoot = Split-Path $pom -Parent
Push-Location $projectRoot
try {
    Write-Host "→ Running: mvn clean package"
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed, exiting script."
        exit $LASTEXITCODE
    }

    # ===== Step 2: Read SNOW_VERSION =====
    Write-Host "Step 2: Read SNOW_VERSION from .env..."
    $dotenvPath = Join-Path $projectRoot ".env"
    $snowVersion = Read-DotEnvValue -FilePath $dotenvPath -Key "SNOW_VERSION"
    if (-not $snowVersion) {
        Write-Host "SNOW_VERSION not found in .env, using placeholder 0.0.0." -ForegroundColor Yellow
        $snowVersion = "0.0.0"
    }
    Write-Host "SNOW_VERSION = $snowVersion"

    # ===== Step 3: Prepare release directory structure =====
    Write-Host "Step 3: Prepare release directory structure..."
    $targetDir   = Join-Path $projectRoot "target"
    $exePath     = Join-Path $targetDir "Snow.exe"
    if (-not (Test-Path $exePath)) {
        Write-Error "Expected build artifact not found: $exePath"
        exit 1
    }

    $verName     = "Snow-v${snowVersion}-windows-x64"
    $releaseRoot = Join-Path $targetDir "release"
    $outDir      = Join-Path $releaseRoot $verName
    $binDir      = Join-Path $outDir "bin"
    $libDir      = Join-Path $outDir "lib"

    # Clean old directory
    if (Test-Path $outDir) {
        Write-Host "→ Cleaning previous output directory..."
        Remove-Item $outDir -Recurse -Force
    }

    New-Item -ItemType Directory -Force -Path $binDir | Out-Null
    Copy-Item -Path $exePath -Destination (Join-Path $binDir "Snow.exe") -Force
    Write-Host ">>> Collected Snow.exe"

    # Optional lib
    $projectLib = Join-Path $projectRoot "lib"
    if (Test-Path $projectLib) {
        New-Item -ItemType Directory -Force -Path $libDir | Out-Null
        Copy-Item -Path (Join-Path $projectLib "*") -Destination $libDir -Recurse -Force
        Write-Host ">>> Copied lib directory"
    } else {
        Write-Host ">>> lib directory not found, skipping." -ForegroundColor Yellow
    }

    # ===== Step 4: Create zip =====
    Write-Host "Step 4: Create release zip..."
    New-Item -ItemType Directory -Force -Path $releaseRoot | Out-Null
    $zipPath = Join-Path $releaseRoot ("{0}.zip" -f $verName)
    if (Test-Path $zipPath) {
        Write-Host "→ Removing existing zip: $zipPath"
        Remove-Item $zipPath -Force
    }

    try {
        Compress-Archive -Path $outDir -DestinationPath $zipPath -Force
    } catch {
        Write-Error "Failed to create zip: $($_.Exception.Message)"
        exit 1
    }

    Write-Host ">>> Package ready!" -ForegroundColor Green
    Write-Host "Version    : $snowVersion"
    Write-Host "Output Dir : $outDir"
    Write-Host "Zip File   : $zipPath"
}
finally {
    Pop-Location
}
