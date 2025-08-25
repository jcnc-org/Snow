# run-linux-snow-export.ps1
# Build and package linux-snow-export, version read from SNOW_VERSION in .env

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Import shared dotenv parser function
. "$PSScriptRoot\tools\dotenv.ps1"

Write-Host "Step 0: Generate .env..."
try {
    & "$PSScriptRoot\tools\generate-dotenv.ps1" -ErrorAction Stop
} catch {
    Write-Error "Failed to generate .env: $( $_.Exception.Message )"
    exit 1
}

Write-Host "Step 1: Build and run linux-snow-export..."
docker compose run --build --rm linux-snow-export
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build & Run failed, exiting script."
    exit $LASTEXITCODE
}

Write-Host "Step 2: Run linux-snow-export without rebuild..."
docker compose run --rm linux-snow-export
if ($LASTEXITCODE -ne 0) {
    Write-Error "Run without rebuild failed, exiting script."
    exit $LASTEXITCODE
}

# ===== Step 3: Read version from .env =====
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$dotenvPath  = Join-Path $projectRoot ".env"

if (-not (Test-Path -LiteralPath $dotenvPath)) {
    Write-Error ".env not found at: $dotenvPath"
    exit 1
}

$version = Read-DotEnvValue -FilePath $dotenvPath -Key 'SNOW_VERSION'
if (-not $version) {
    Write-Error "SNOW_VERSION not found in .env"
    exit 1
}

# ===== Step 4: Define output paths =====
$targetDir = Join-Path $projectRoot "target\release"
$outDir    = Join-Path $targetDir "Snow-v$version-linux-x64"
$tgzPath   = Join-Path $targetDir "Snow-v$version-linux-x64.tgz"

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host "Version    : $version"
Write-Host "Output Dir : $outDir"
Write-Host "Tgz File   : $tgzPath"