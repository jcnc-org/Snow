# release-linux.ps1
# Build and package linux-snow-export, version read from SNOW_VERSION in .env

# force to use PowerShell 7
if ($PSVersionTable.PSEdition -ne "Core") {
    Write-Host "Switching to PowerShell 7..."
    $pwsh = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $pwsh) {
        throw "PowerShell 7 (pwsh.exe) not installed."
    }
    & $pwsh.Source -NoLogo -NoProfile -File $PSCommandPath @args
    exit $LASTEXITCODE
}

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Import shared dotenv parser function
. "$PSScriptRoot\tools\dotenv.ps1"

Write-Host "Step 0: Generate .env..."
try {
    & "$PSScriptRoot\tools\generate-dotenv.ps1" -ErrorAction Stop
} catch {
    Write-Error ("Failed to generate .env: {0}" -f $_.Exception.Message)
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

# ===== Step 3: Fix target directory permissions BEFORE packaging =====
$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
if (-not $IsWindows) {
    $targetRoot = Join-Path $projectRoot "target"
    Write-Host ("Step 3: Fix permissions for {0} ..." -f $targetRoot)
    try {
        # 只用 id -un 获取当前用户，避免 $env:USER 为空的问题
        $userName = (& id -un 2>$null).Trim()
        if (-not $userName) {
            # 兜底：如果拿不到用户名，就用你机器上的用户名（比如 x）
            $userName = "x"
        }

        # 只设置 owner，不显式设置 group，避免出现多余的 ':x' 参数
        & sudo chown -R $userName $targetRoot

        Write-Host (">>> Permissions fixed for {0} (owner={1})" -f $targetRoot, $userName)
    } catch {
        Write-Warning ("Failed to fix permissions for {0}: {1}" -f $targetRoot, $_.Exception.Message)
    }
} else {
    Write-Host "Step 3: Windows environment detected, skipping chown."
}

# ===== Step 4: Read version from .env =====
Write-Host "Step 4: Read SNOW_VERSION from .env..."
$dotenvPath  = Join-Path $projectRoot ".env"

if (-not (Test-Path -LiteralPath $dotenvPath)) {
    Write-Error (" .env not found at: {0}" -f $dotenvPath)
    exit 1
}

$version = Read-DotEnvValue -FilePath $dotenvPath -Key 'SNOW_VERSION'
if (-not $version) {
    Write-Error "SNOW_VERSION not found in .env"
    exit 1
}

# ===== Step 5: Define output paths =====
Write-Host "Step 5: Define output paths..."
$targetDir = Join-Path $projectRoot "target/release"
$outDir    = Join-Path $targetDir "snow-v$version-linux-x64"
$tgzPath   = Join-Path $targetDir "snow-v$version-linux-x64.tgz"

# ===== Step 6: Create VERSION file in SDK root directory =====
Write-Host "Step 6: Create VERSION file in SDK root directory..."
$versionFilePath = Join-Path $outDir "VERSION"
try {
    Set-Content -Path $versionFilePath -Value $version -Force
    if (Test-Path $versionFilePath) {
        $versionContent = Get-Content -Path $versionFilePath -Raw
        if ($versionContent.Trim() -eq $version) {
            Write-Host (">>> Created VERSION file at {0} with content: {1}" -f $versionFilePath, $version)
        } else {
            Write-Warning ("VERSION file content mismatch. Expected: {0}, Actual: {1}" -f $version, $versionContent.Trim())
        }
    } else {
        Write-Warning ("Failed to create VERSION file at {0}" -f $versionFilePath)
    }
} catch {
    Write-Warning ("Failed to create VERSION file: {0}" -f $_.Exception.Message)
}

# ===== Step 7: Package to .tgz (Linux-compatible) =====
Write-Host "Step 7: Package to .tgz..."

if (-not (Test-Path -LiteralPath $outDir)) {
    Write-Error ("Output directory not found: {0}" -f $outDir)
    exit 1
}

# Ensure target directory exists
if (-not (Test-Path -LiteralPath $targetDir)) {
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
}

# Remove old package if exists
if (Test-Path -LiteralPath $tgzPath) {
    Write-Host ("→ Removing existing tgz: {0}" -f $tgzPath)
    try {
        Remove-Item -LiteralPath $tgzPath -Force
    } catch {
        Write-Warning ("Failed to remove existing tgz: {0}" -f $_.Exception.Message)
    }
}

function Invoke-TarGz {
    param(
        [Parameter(Mandatory = $true)][string]$SourceDir,
        [Parameter(Mandatory = $true)][string]$DestTgz
    )

    $tarExe = "tar"

    if ($IsWindows) {
        # Windows 用 tar
        $args = @("-C", $SourceDir, "-czf", $DestTgz, ".")
    } else {
        # Linux/macOS
        # 使用 GNU tar + gzip -9
        $args = @("-C", $SourceDir, "-cf", $DestTgz, "--use-compress-program=gzip", ".")
    }

    $psi = @{
        FilePath     = $tarExe
        ArgumentList = $args
        NoNewWindow  = $true
        Wait         = $true
    }

    try {
        $p = Start-Process @psi -PassThru -ErrorAction Stop
        $p.WaitForExit()
        if ($p.ExitCode -ne 0) {
            throw "tar exited with code $($p.ExitCode)"
        }
    } catch {
        throw ("Packaging failed: {0}" -f $_.Exception.Message)
    }
}

try {
    Invoke-TarGz -SourceDir $outDir -DestTgz $tgzPath
} catch {
    Write-Error $_.Exception.Message
    exit 1
}

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host ("Version    : {0}" -f $version)
Write-Host ("Output Dir : {0}" -f $outDir)
Write-Host ("Tgz File   : {0}" -f $tgzPath)