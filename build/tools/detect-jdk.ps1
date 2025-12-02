# tools/detect-jdk.ps1
Set-StrictMode -Version Latest

function Detect-JDK {
    Write-Host "🔍 Detecting JDK..."

    # A. Prefixed JAVA_HOME
    if ($env:JAVA_HOME) {
        $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $javaExe) { return $env:JAVA_HOME }
    }

    # B. jabba environment
    $jabbaScript = Join-Path $HOME '.jabba\jabba.ps1'
    if (Test-Path $jabbaScript) {
        . $jabbaScript
        if ($env:JAVA_HOME) {
            $javaExe = Join-Path $env:JAVA_HOME 'bin\java.exe'
            if (Test-Path $javaExe) { return $env:JAVA_HOME }
        }
    }

    # C. scan jabba jdk folder
    $jdkRoot = Join-Path $HOME '.jabba\jdk'
    if (Test-Path $jdkRoot) {
        $jdks = Get-ChildItem $jdkRoot -Directory | Sort-Object LastWriteTime -Descending
        foreach ($d in $jdks) {
            $javaExe = Join-Path $d.FullName 'bin\java.exe'
            if (Test-Path $javaExe) { return $d.FullName }
        }
    }

    # D. java in PATH
    $javaCmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCmd) {
        return (Split-Path (Split-Path $javaCmd.Source))
    }

    throw "❌ No JDK found (JAVA_HOME, jabba, PATH). Please install JDK."
}

$jdkHome = Detect-JDK
Write-Output $jdkHome