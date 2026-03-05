param(
  [switch]$Force
)

$ErrorActionPreference = "Stop"

$tags = @("M0","M1","M2","M3","M4","M5","M6","M7")

foreach ($tag in $tags) {
  if ($Force) {
    git tag -f $tag
  } else {
    git tag $tag
  }
}

Write-Host "[milestones] tags created: $($tags -join ', ')"
