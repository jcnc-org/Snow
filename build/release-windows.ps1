# release-windows.ps1

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------- 0. Ensure running in PowerShell 7 (external script) ----------
& (Join-Path $PSScriptRoot 'tools/ensure-pwsh7.ps1')

# ---------- 1. Detect JDK ----------
$jdkHome = & (Join-Path $PSScriptRoot 'tools/detect-jdk.ps1')
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

# ---------- 2. Detect Maven ----------
$mvnPath = & (Join-Path $PSScriptRoot 'tools/detect-maven.ps1')
Write-Host "Maven found: $mvnPath"
Write-Host (& mvn -v)

# ---------- Import dotenv ----------
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

    # Step 4: Write VERSION
    $versionFilePath = Join-Path $outDir 'VERSION'
    Set-Content $versionFilePath $snowVersion

    # Step 5: Zip
    $zipPath = Join-Path $releaseRoot "$outDirName.zip"
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

    # Ensure parent directory exists
    $null = New-Item -ItemType Directory -Path $releaseRoot -Force

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    [System.IO.Compression.ZipFile]::CreateFromDirectory(
            $outDir,
            $zipPath,
            [System.IO.Compression.CompressionLevel]::Optimal,
            $false
    )

} finally {
    Pop-Location
}

Write-Host ">>> Package ready!" -ForegroundColor Green
Write-Host "Version : $snowVersion"
Write-Host "Output  : $outDir"
Write-Host "Zip     : $zipPath"