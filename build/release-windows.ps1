# release-windows.ps1

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------- 0. Ensure running in PowerShell 7 ----------
if ($PSVersionTable.PSEdition -ne 'Core') {
    Write-Host "Detected Windows PowerShell 5.x — switching to PowerShell 7..." -ForegroundColor Yellow

    $pwshCmd = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $pwshCmd) {
        throw "PowerShell 7 (pwsh) not found. Install from https://github.com/PowerShell/PowerShell"
    }

    & $pwshCmd.Source -NoLogo -NoProfile -File $PSCommandPath @args
    exit $LASTEXITCODE
}

# ---------- 1. Detect JDK (no system env modifications) ----------
function Detect-JDK {
    Write-Host "🔍 Detecting JDK..."

    # A. Prefixed JAVA_HOME
    if ($env:JAVA_HOME) {
        $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $javaExe) {
            return $env:JAVA_HOME
        }
    }

    # B. jabba environment
    $jabbaScript = Join-Path $HOME '.jabba\jabba.ps1'
    if (Test-Path $jabbaScript) {
        . $jabbaScript
        if ($env:JAVA_HOME) {
            $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
            if (Test-Path $javaExe) {
                return $env:JAVA_HOME
            }
        }
    }

    # C. scan jabba jdk folder (pick most recently modified JDK)
    $jdkRoot = Join-Path $HOME '.jabba\jdk'
    if (Test-Path $jdkRoot) {
        $jdks = Get-ChildItem $jdkRoot -Directory | Sort-Object LastWriteTime -Descending
        foreach ($d in $jdks) {
            $javaExe = Join-Path $d.FullName 'bin\java.exe'
            if (Test-Path $javaExe) {
                return $d.FullName
            }
        }
    }

    # D. java in PATH (compatible with PS5)
    $javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaBinDir = Split-Path $javaCmd.Source
        $javaHomeGuess = Split-Path $javaBinDir
        return $javaHomeGuess
    }

    throw "❌ No JDK found (JAVA_HOME, jabba, PATH). Please install JDK."
}

$jdkHome = Detect-JDK
Write-Host "✓ JDK detected at: $jdkHome"

# temp JAVA_HOME (session only)
$env:JAVA_HOME = $jdkHome
$env:Path      = ("{0};{1}" -f (Join-Path $jdkHome 'bin'), $env:Path)

# ---------- Java check ----------
try {
    $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
    $javaVerOutput = & $javaExe -version 2>&1
    Write-Host $javaVerOutput
} catch {
    throw "Failed to execute java.exe from temporary JAVA_HOME"
}

# ---------- Maven check ----------
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCmd) {
    throw "❌ Maven not found in PATH. Please install Maven."
}

Write-Host "Maven found: $($mvnCmd.Source)"
Write-Host (& mvn -v)

# Import dotenv
. (Join-Path $PSScriptRoot 'tools/dotenv.ps1')

# ---------- pom locator ----------
function Find-PomUpwards([string]$startDir) {
    $dir = (Resolve-Path $startDir).Path
    while ($true) {
        $pomPath = Join-Path $dir 'pom.xml'
        if (Test-Path $pomPath) { return $pomPath }
        $parent = Split-Path -Path $dir -Parent
        if ($parent -eq $dir) { return $null }
        $dir = $parent
    }
}

# ---------- Step 0: Generate .env ----------
Write-Host "Step 0: Generating .env..."
& (Join-Path $PSScriptRoot 'tools/generate-dotenv.ps1')

# ---------- Step 1: Build ----------
$pom = Find-PomUpwards $PSScriptRoot
if (-not $pom) { throw "pom.xml not found" }

$projectRoot = Split-Path $pom -Parent
Push-Location $projectRoot

try {
    Write-Host "Running mvn clean package..."
    mvn -q clean package

    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE."
    }

    # Step 2: Read version
    $envFilePath = Join-Path $projectRoot '.env'
    $snowVersion = Read-DotEnvValue -FilePath $envFilePath -Key "SNOW_VERSION"
    if (-not $snowVersion) { $snowVersion = "0.0.0" }

    Write-Host "SNOW_VERSION = $snowVersion"

    # Step 3: Prepare release package
    $targetDir = Join-Path $projectRoot 'target'
    $exePath   = Join-Path $targetDir 'snow.exe'
    if (-not (Test-Path $exePath)) {
        throw "snow.exe not found."
    }

    $releaseRoot = Join-Path $targetDir 'release'
    $outDirName  = "snow-v$snowVersion-windows-x64"
    $outDir      = Join-Path $releaseRoot $outDirName
    $binDir      = Join-Path $outDir 'bin'
    $libDir      = Join-Path $outDir 'lib'

    if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }

    New-Item $binDir -ItemType Directory -Force | Out-Null
    Copy-Item $exePath (Join-Path $binDir 'snow.exe')

    $projectLibDir = Join-Path $projectRoot 'lib'
    if (Test-Path $projectLibDir) {
        New-Item $libDir -ItemType Directory -Force | Out-Null
        Copy-Item (Join-Path $projectLibDir '*') $libDir -Recurse -Force
    }

    # Step 4: Write VERSION (before zipping so it's included)
    $versionFilePath = Join-Path $outDir 'VERSION'
    Set-Content $versionFilePath $snowVersion

    # Step 5: Zip
    New-Item $releaseRoot -ItemType Directory -Force | Out-Null
    $zipPath = Join-Path $releaseRoot "$outDirName.zip"

    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

    Compress-Archive -Path (Join-Path $outDir '*') -DestinationPath $zipPath -CompressionLevel Optimal

} finally {
    Pop-Location
}

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host "Version : $snowVersion"
Write-Host "Output  : $outDir"
Write-Host "Zip     : $zipPath"