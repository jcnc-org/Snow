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
$outDir    = Join-Path $targetDir "snow-v$version-linux-x64"
$tgzPath   = Join-Path $targetDir "snow-v$version-linux-x64.tgz"

# ===== Step 5: Package to .tgz (no extra top-level dir, max compression) =====
Write-Host "Step 5: Package to .tgz..."

if (-not (Test-Path -LiteralPath $outDir)) {
    Write-Error "Output directory not found: $outDir"
    exit 1
}

# Ensure target directory exists
if (-not (Test-Path -LiteralPath $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

# Remove old package if exists
if (Test-Path -LiteralPath $tgzPath) {
    Write-Host "→ Removing existing tgz: $tgzPath"
    Remove-Item -LiteralPath $tgzPath -Force
}

function Invoke-TarGz {
    param(
        [Parameter(Mandatory = $true)][string]$SourceDir,
        [Parameter(Mandatory = $true)][string]$DestTgz
    )
    $tarExe = "tar"

    $isWindows = $env:OS -eq 'Windows_NT'

    if ($isWindows) {
        $psi = @{
            FilePath    = $tarExe
            ArgumentList= @("-C", $SourceDir, "-czf", $DestTgz, ".")
            NoNewWindow = $true
            Wait        = $true
        }
        try {
            $p = Start-Process @psi -PassThru -ErrorAction Stop
            $p.WaitForExit()
            if ($p.ExitCode -ne 0) {
                throw "tar exited with code $($p.ExitCode)"
            }
        } catch {
            throw "Packaging failed (Windows tar): $($_.Exception.Message)"
        }
    } else {
        try {
            $psi = @{
                FilePath    = $tarExe
                ArgumentList= @("-C", $SourceDir, "-c", "-f", $DestTgz, "-I", "gzip -9", ".")
                NoNewWindow = $true
                Wait        = $true
            }
            $p = Start-Process @psi -PassThru -ErrorAction Stop
            $p.WaitForExit()
            if ($p.ExitCode -eq 0) { return }
        } catch { }

        try {
            $psi = @{
                FilePath    = $tarExe
                ArgumentList= @("-C", $SourceDir, "-c", "-z", "-f", $DestTgz, ".")
                NoNewWindow = $true
                Wait        = $true
            }
            $p = Start-Process @psi -PassThru -ErrorAction Stop
            $p.WaitForExit()
            if ($p.ExitCode -ne 0) {
                throw "tar exited with code $($p.ExitCode)"
            }
        } catch {
            throw "Packaging failed (Linux tar): $($_.Exception.Message)"
        }
    }
}
try {
    Invoke-TarGz -SourceDir $outDir -DestTgz $tgzPath
} catch {
    Write-Error $_.Exception.Message
    exit 1
}

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host "Version    : $version"
Write-Host "Output Dir : $outDir"
Write-Host "Tgz File   : $tgzPath"

# ===== Step 6: Create VERSION file in SDK root directory =====
Write-Host "Step 6: Create VERSION file in SDK root directory..."
$versionFilePath = Join-Path $outDir "VERSION"
try {
    Set-Content -Path $versionFilePath -Value $version -Force
    # Verify the file was created and has content
    if (Test-Path $versionFilePath) {
        $versionContent = Get-Content -Path $versionFilePath -Raw
        if ($versionContent.Trim() -eq $version) {
            Write-Host ">>> Created VERSION file at $versionFilePath with content: $version"
        } else {
            Write-Warning "VERSION file was created but content does not match. Expected: $version, Actual: $($versionContent.Trim())"
        }
    } else {
        Write-Warning "Failed to create VERSION file at $versionFilePath"
    }
} catch {
    Write-Warning "Failed to create VERSION file: $($_.Exception.Message)"
}"
