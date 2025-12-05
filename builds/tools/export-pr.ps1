# 用于对比的目标分支
# 当前分支将自动检测，然后与该分支比较
$TargetBranch = "dev"

# 输出文件
$OutputFile = "pr_commits.txt"

# ---- 切换到项目根目录：/snow ----
$ScriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path   # .../build/tools
$BuildDir    = Split-Path -Parent $ScriptDir                     # .../build
$ProjectRoot = Split-Path -Parent $BuildDir                      # .../snow

Set-Location -Path $ProjectRoot

# 检查 Git 仓库
if (-not (Test-Path ".git")) {
    Write-Host "❌ 当前目录不是 Git 仓库，请在项目根目录运行。" -ForegroundColor Red
    exit 1
}

# ---- 获取当前分支 ----
$CurrentBranch = git rev-parse --abbrev-ref HEAD
$CurrentBranch = $CurrentBranch.Trim()
Write-Host "ℹ️ 当前分支: $CurrentBranch"

# ---- 检查远程 ----
$Remote = (git remote) | Select-Object -First 1
if (-not $Remote) {
    Write-Host "❌ 未检测到任何远程，请确认仓库已配置远程。" -ForegroundColor Red
    exit 1
}
Write-Host "🔍 检测到远程: $Remote"

Write-Host "🔄 正在更新远程分支信息..."
git fetch $Remote | Out-Null

# ---- 检查配置的目标分支 ----
$targetExists = $false

if (git show-ref "refs/heads/$TargetBranch" 2>$null) {
    $targetExists = $true
}

if (-not $targetExists -and (git show-ref "refs/remotes/$Remote/$TargetBranch" 2>$null)) {
    $TargetBranch = "$Remote/$TargetBranch"
    $targetExists = $true
}

if (-not $targetExists) {
    Write-Host "❌ 目标分支 '$TargetBranch' 不存在（本地或远程）。" -ForegroundColor Red
    exit 1
}

Write-Host "🎯 目标分支: $TargetBranch"

# ---- 导出日志：TargetBranch..CurrentBranch ----
Write-Host "📝 正在导出 $CurrentBranch 相对于 $TargetBranch 的提交日志..."
git log "$TargetBranch..$CurrentBranch" --pretty=format:'%s%n%n%b%n---' > $OutputFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 导出完成：$OutputFile" -ForegroundColor Green
    Write-Host "📄 路径: $(Resolve-Path $OutputFile)"
} else {
    Write-Host "❌ 导出失败，请检查分支名称或远程是否存在。" -ForegroundColor Red
}