Set-StrictMode -Version Latest

# ======================================================
# Snow VM - 发布到 Maven Central PowerShell 脚本
# Author: Snow Language Team
# ======================================================

# -------------------------------
# 彩色输出函数
# -------------------------------
function Write-Info($msg)
{
    Write-Host "🔵 [INFO]  $msg" -ForegroundColor Cyan
}
function Write-Warn($msg)
{
    Write-Host "🟡 [WARN]  $msg" -ForegroundColor Yellow
}
function Write-ErrorMsg($msg)
{
    Write-Host "🔴 [ERROR] $msg" -ForegroundColor Red
}
function Write-Success($msg)
{
    Write-Host "🟢 [OK]    $msg" -ForegroundColor Green
}

# -------------------------------
# 加载 detect-jdk.ps1
# -------------------------------
$detectScript = Join-Path $PSScriptRoot "../builds/tools/detect-jdk.ps1"

if (-not (Test-Path $detectScript))
{
    Write-ErrorMsg "detect-jdk.ps1 未找到：$detectScript"
    exit 1
}

# dot-source 加载以导入 Detect-JDK 函数
. $detectScript

try
{
    $jdkHome = Detect-JDK
}
catch
{
    Write-ErrorMsg $_
    exit 1
}

Write-Info "使用的 JDK: $jdkHome"
$env:JAVA_HOME = $jdkHome
$env:Path = "$jdkHome/bin;$env:Path"

# -------------------------------
# 检查 Maven & GPG
# -------------------------------
if (-not (Get-Command mvn -ErrorAction SilentlyContinue))
{
    Write-ErrorMsg "未找到 Maven，请先安装并配置到 PATH。"
    exit 1
}

if (-not (Get-Command gpg -ErrorAction SilentlyContinue))
{
    Write-ErrorMsg "未找到 GPG，请先安装 GnuPG。"
    exit 1
}

# -------------------------------
# 环境打印
# -------------------------------
Write-Info "当前 Maven 版本："
mvn -v

Write-Info "当前 JAVA_HOME：$env:JAVA_HOME"

# -------------------------------
# Maven 执行包装器（实时输出 + 捕获失败）
# -------------------------------
function Run-Maven([string]$cmd, [string]$errorMsg)
{
    Write-Info "执行命令：mvn $cmd"
    $process = Start-Process mvn -ArgumentList $cmd -NoNewWindow -Wait -PassThru

    if ($process.ExitCode -ne 0)
    {
        Write-ErrorMsg $errorMsg
        exit $process.ExitCode
    }
}

# -------------------------------
# Step 1: clean + install
# -------------------------------
Run-Maven "clean install -P release" "构建失败，请检查代码或构建配置。"

# -------------------------------
# Step 2: deploy
# -------------------------------
Run-Maven "clean deploy -P release" "发布失败，请检查 GPG、Sonatype Token 或网络连接。"