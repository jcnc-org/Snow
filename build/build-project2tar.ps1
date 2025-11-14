# Set the tar package name
$tarName = "snow.tar"

# Get the script's current directory (build folder)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# Get the parent directory (the project root)
$parentDir = Split-Path -Parent $scriptDir

# Set the full path to the tar package
$tarPath = Join-Path $parentDir $tarName

# Output message: starting to create tar package
Write-Output "Starting to create tar package: $tarName in $parentDir ..."

# Remove old tar package if it exists
if (Test-Path $tarPath) {
    Write-Output "Found an old $tarName, removing it..."
    Remove-Item $tarPath -Force
}

# Make sure the tar command is available
$tarCommand = "tar"
if (-not (Get-Command $tarCommand -ErrorAction SilentlyContinue)) {
    Write-Error "❌ 'tar' command is not available. Please make sure 'tar' is installed and can be run from PowerShell."
    exit 1
}

# Locate lib directory (try common locations)
$libPathCandidate1 = Join-Path $parentDir "lib"
$libPathCandidate2 = Join-Path $scriptDir "..\lib"

$libPath = $null
if (Test-Path $libPathCandidate1) {
    $libPath = (Resolve-Path $libPathCandidate1).ProviderPath
} elseif (Test-Path $libPathCandidate2) {
    $libPath = (Resolve-Path $libPathCandidate2).ProviderPath
}

if ($libPath) {
    Write-Output "Found lib directory at: $libPath"
} else {
    Write-Output "No lib directory found at $libPathCandidate1 or $libPathCandidate2 — will package only 'snow'."
}

# Prepare tar command arguments
# We always include the 'snow' folder from: $scriptDir\..\src\main\java\org\jcnc
$snowDirParent = Join-Path $scriptDir "..\src\main\java\org\jcnc"
$snowDirParent = (Resolve-Path $snowDirParent -ErrorAction SilentlyContinue)
if (-not $snowDirParent) {
    Write-Error "❌ Cannot find source directory: $scriptDir\..\src\main\java\org\jcnc"
    exit 1
}
$snowDirParent = $snowDirParent.ProviderPath

$tarCommandArgs = @("-cf", $tarPath, "-C", $snowDirParent, "snow")

# If lib exists, add it: change to lib's parent and add the lib directory name
if ($libPath) {
    $libParent = Split-Path -Parent $libPath
    $libName = Split-Path -Leaf $libPath
    $tarCommandArgs += ("-C", $libParent, $libName)
}

Write-Output "Running tar command: tar $($tarCommandArgs -join ' ')"

# Execute tar
try {
    & $tarCommand @tarCommandArgs
} catch {
    Write-Error "❌ Failed to create tar package. Error: $_"
    exit 1
}

# Check if tar package was created successfully
if (Test-Path $tarPath) {
    Write-Output "✅ Successfully created $tarName"
    exit 0
} else {
    Write-Error "❌ Creation failed. Please check the tar command and paths."
    exit 1
}
