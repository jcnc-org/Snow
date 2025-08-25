# run-linux-snow-export.ps1

Write-Host "Step 0: Generate .env..."
try {
    & "$PSScriptRoot\tools\generate-dotenv.ps1" -ErrorAction Stop
}
catch {
    Write-Error "Failed to generate .env: $($_.Exception.Message)"
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

Write-Host "All steps completed successfully!"
