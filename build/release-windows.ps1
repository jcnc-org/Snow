# release-windows.ps1

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------- 0. Ensure running in PowerShell 7 ----------
if ($PSVersionTable.PSEdition -ne 'Core') {
    Write-Host "Detected Windows PowerShell 5.x — switching to PowerShell 7..." -ForegroundColor Yellow

    $pwshCmd = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $pwshCmd) {
        Write-Error "PowerShell 7 (pwsh) not found. Install from https://github.com/PowerShell/PowerShell"
        exit 1
    }

    & $pwshCmd.Source -NoLogo -NoProfile -File $PSCommandPath @args
    exit $LASTEXITCODE
}

# ---------- 1. Detect JDK (no system env modifications) ----------
function Detect-JDK {
    Write-Host "🔍 Detecting JDK..."

    # A. Prefixed JAVA_HOME
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/java.exe")) {
        return $env:JAVA_HOME
    }

    # B. jabba environment
    $jabbaScript = "$HOME\.jabba\jabba.ps1"
    if (Test-Path $jabbaScript) {
        . $jabbaScript
        if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/java.exe")) {
            return $env:JAVA_HOME
        }
    }

    # C. scan jabba jdk folder
    $jdkRoot = "$HOME\.jabba\jdk"
    if (Test-Path $jdkRoot) {
        $jdks = Get-ChildItem $jdkRoot -Directory | Sort-Object Name -Descending
        foreach ($d in $jdks) {
            if (Test-Path "$($d.FullName)/bin/java.exe") {
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

    Write-Error "❌ No JDK found (JAVA_HOME, jabba, PATH). Please install JDK."
    exit 1
}

$jdkHome = Detect-JDK
Write-Host "✓ JDK detected at: $jdkHome"

# temp JAVA_HOME (session only)
$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome\bin;$env:Path"

# ---------- Java check ----------
try {
    $javaVerOutput = & "$env:JAVA_HOME/bin/java.exe" -version 2>&1
    Write-Host $javaVerOutput
} catch {
    Write-Error "Failed to execute java.exe from temporary JAVA_HOME"
    exit 1
}

# ---------- Maven check ----------
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCmd) {
    Write-Error "❌ Maven not found in PATH. Please install Maven."
    exit 1
}

Write-Host "Maven found: $($mvnCmd.Source)"
Write-Host (& mvn -v)

# Import dotenv
. "$PSScriptRoot/tools/dotenv.ps1"

# ---------- pom locator ----------
function Find-PomUpwards([string]$startDir) {
    $dir = Resolve-Path $startDir
    while ($true) {
        if (Test-Path (Join-Path $dir 'pom.xml')) { return (Join-Path $dir 'pom.xml') }
        $parent = Split-Path $dir -Parent
        if ($parent -eq $dir) { return $null }
        $dir = $parent
    }
}

# ---------- Step 0: Generate .env ----------
Write-Host "Step 0: Generating .env..."
& "$PSScriptRoot/tools/generate-dotenv.ps1"

# ---------- Step 1: Build ----------
$pom = Find-PomUpwards $PSScriptRoot
if (-not $pom) { Write-Error "pom.xml not found"; exit 1 }

$projectRoot = Split-Path $pom -Parent
Push-Location $projectRoot

try {
    Write-Host "Running mvn clean package..."
    mvn -q clean package

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven build failed."
        exit $LASTEXITCODE
    }

    # Step 2: Read version
    $snowVersion = Read-DotEnvValue -FilePath "$projectRoot/.env" -Key "SNOW_VERSION"
    if (-not $snowVersion) { $snowVersion = "0.0.0" }

    Write-Host "SNOW_VERSION = $snowVersion"

    # Step 3: Prepare release package
    $targetDir = "$projectRoot/target"
    $exePath = "$targetDir/snow.exe"
    if (-not (Test-Path $exePath)) {
        Write-Error "snow.exe not found."
        exit 1
    }

    $releaseRoot = "$targetDir/release"
    $outDir = "$releaseRoot/snow-v$snowVersion-windows-x64"
    $binDir = "$outDir/bin"
    $libDir = "$outDir/lib"

    if (Test-Path $outDir) { Remove-Item $outDir -Recurse -Force }

    New-Item $binDir -ItemType Directory -Force | Out-Null
    Copy-Item $exePath "$binDir/snow.exe"

    if (Test-Path "$projectRoot/lib") {
        New-Item $libDir -ItemType Directory -Force | Out-Null
        Copy-Item "$projectRoot/lib/*" $libDir -Recurse -Force
    }

    # Step 4: Zip
    New-Item $releaseRoot -ItemType Directory -Force | Out-Null
    $zipPath = "$releaseRoot/snow-v$snowVersion-windows-x64.zip"

    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

    Compress-Archive -Path "$outDir/*" -DestinationPath $zipPath -CompressionLevel Optimal

    # Step 5: Write VERSION
    Set-Content "$outDir/VERSION" $snowVersion

} finally {
    Pop-Location
}

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host "Version    : $snowVersion"
Write-Host "Output: $outDir"
Write-Host "Zip:    $zipPath"