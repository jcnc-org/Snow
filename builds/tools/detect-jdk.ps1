Set-StrictMode -Version Latest

function Get-Platform {
    # Windows PowerShell 有 OS=Windows_NT
    if ($env:OS -eq "Windows_NT") { return "Windows" }

    # pwsh on Linux/Mac
    if ($PSVersionTable.Platform -eq "Unix") {
        try {
            if ((uname) -eq "Darwin") { return "macOS" }
            return "Linux"
        } catch { return "Linux" }
    }

    return "Unknown"
}

function Detect-JDK {
    Write-Host "🔍 Detecting JDK..."

    $platform = Get-Platform
    $javaFile = if ($platform -eq "Windows") { "java.exe" } else { "java" }

    function Test-JavaHome([string]$path) {
        if (-not $path) { return $false }
        return Test-Path (Join-Path $path "bin/$javaFile")
    }

    # A. JAVA_HOME
    if (Test-JavaHome $env:JAVA_HOME) {
        return $env:JAVA_HOME
    }

    # B. jabba environment
    $jabbaScript = if ($platform -eq "Windows") {
        Join-Path $HOME ".jabba/jabba.ps1"
    } else {
        Join-Path $HOME ".jabba/jabba.sh"
    }

    if (Test-Path $jabbaScript) {
        . $jabbaScript 2>$null
        if (Test-JavaHome $env:JAVA_HOME) {
            return $env:JAVA_HOME
        }
    }

    # C. jabba jdk folder
    $jdkRoot = Join-Path $HOME ".jabba/jdk"
    if (Test-Path $jdkRoot) {
        $dirs = Get-ChildItem $jdkRoot -Directory | Sort-Object LastWriteTime -Descending
        foreach ($d in $dirs) {
            if (Test-JavaHome $d.FullName) {
                return $d.FullName
            }
        }
    }

    # D. PATH java
    $javaCmd = Get-Command $javaFile -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $binPath = Split-Path $javaCmd.Source
        return (Split-Path $binPath)
    }

    throw "❌ No JDK found via JAVA_HOME, jabba, or PATH."
}

$jdkHomeFound = Detect-JDK
Write-Output $jdkHomeFound