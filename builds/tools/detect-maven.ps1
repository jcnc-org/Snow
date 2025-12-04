# tools/detect-maven.ps1
Set-StrictMode -Version Latest

function Detect-Maven {
    Write-Host "🔍 Detecting Maven..."

    $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvnCmd) {
        throw "❌ Maven not found in PATH. Please install Maven."
    }

    Write-Host "✓ Maven found: $($mvnCmd.Source)"
    Write-Host (& mvn -v)

    return $mvnCmd.Source
}

$mavenPath = Detect-Maven
Write-Output $mavenPath