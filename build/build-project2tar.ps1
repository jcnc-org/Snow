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

# Execute tar: change to org\jcnc directory and compress the snow folder
try {
    # Build the command and run it
    $tarCommandArgs = "-cf", $tarPath, "-C", "$scriptDir\..\src\main\java\org\jcnc", "snow"
    Write-Output "Running tar command: tar $tarCommandArgs"

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
