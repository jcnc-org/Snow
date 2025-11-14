param(
    [Parameter(Mandatory = $false)]
    [string]$Branch = "",

    [Parameter(Mandatory = $false)]
    [string]$OutputFile = "pr_commits.txt"
)

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
if (-not $Branch) {
    $Branch = git rev-parse --abbrev-ref HEAD
    Write-Host "ℹ️ 未指定分支，使用当前分支: $Branch"
}

# 去掉可能的换行/空格
$Branch = $Branch.Trim()

# ---- 自动检测远程名称 ----
$Remote = (git remote) | Select-Object -First 1
if (-not $Remote) {
    Write-Host "❌ 未检测到任何远程，请确认仓库已配置远程。" -ForegroundColor Red
    exit 1
}
Write-Host "🔍 检测到远程: $Remote"

# ---- 自动检测 main/master ----
$MainBranch = "main"
if (-not (git show-ref "refs/remotes/$Remote/main" 2>$null)) {
    if (git show-ref "refs/remotes/$Remote/master" 2>$null) {
        $MainBranch = "master"
    }
}
Write-Host "🔍 主分支检测结果: $Remote/$MainBranch"

Write-Host "🔄 正在更新远程分支信息..."
git fetch $Remote | Out-Null

# ---- 检查分支存在性（本地分支或可解析的 ref 都算）----
$branchExists = $false

# 本地分支
if (git show-ref "refs/heads/$Branch" 2>$null) {
    $branchExists = $true
}

# 远程跟踪分支（例如只拉了 remote 分支）
if (-not $branchExists -and (git show-ref "refs/remotes/$Remote/$Branch" 2>$null)) {
    # 让 $Branch 指向远程分支
    $Branch = "$Remote/$Branch"
    $branchExists = $true
}

if (-not $branchExists) {
    Write-Host "❌ 分支 '$Branch' 不存在（本地或远程都未找到）。" -ForegroundColor Red
    exit 1
}

# ---- 导出日志 ----
Write-Host "📝 正在导出 $Branch 相对于 $Remote/$MainBranch 的提交日志..."
git log "$Remote/$MainBranch..$Branch" --pretty=format:'%s%n%n%b%n---' > $OutputFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ 导出完成：$OutputFile" -ForegroundColor Green
    Write-Host "📄 路径: $(Resolve-Path $OutputFile)"
} else {
    Write-Host "❌ 导出失败，请检查分支名称或远程是否存在。" -ForegroundColor Red
}
