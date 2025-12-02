# Snow 模块自动安装脚本
# 功能: 按依赖顺序编译和安装所有模块到本地 Maven 仓库

param(
    [switch]$Clean = $false, # 是否先执行 clean
    [switch]$SkipTests = $false        # 是否跳过测试
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------------------------------------------------------------
# 0. 复用：确保运行在 PowerShell 7
# ---------------------------------------------------------------
& (Join-Path $PSScriptRoot '..\tools\ensure-pwsh7.ps1')

# ---------------------------------------------------------------
# 1. 复用：检测 JDK
# ---------------------------------------------------------------
$jdkHome = & (Join-Path $PSScriptRoot '..\tools\detect-jdk.ps1')
Write-Host "✓ JDK detected at: $jdkHome" -ForegroundColor Green

$env:JAVA_HOME = $jdkHome
$env:Path = ("{0};{1}" -f (Join-Path $jdkHome 'bin'), $env:Path)

# ---------------------------------------------------------------
# 2. 复用：检测 Maven
# ---------------------------------------------------------------
$mvnPath = & (Join-Path $PSScriptRoot '..\tools\detect-maven.ps1')
Write-Host "✓ Maven detected at: $mvnPath" -ForegroundColor Green

# ---------------------------------------------------------------
# 3. 颜色输出函数
# ---------------------------------------------------------------
function Write-Success
{
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom
{
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info
{
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

# ---------------------------------------------------------------
# 4. 寻找项目根目录 (build/module/ → 上两级)
# ---------------------------------------------------------------
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

Write-Info "项目根目录: $projectRoot"

# ---------------------------------------------------------------
# 5. 定义模块及依赖顺序
# ---------------------------------------------------------------
$modules = @(
    @{ name = "snow-common"; path = "snow-common" },
    @{ name = "snow-lexer"; path = "snow-lexer" },
    @{ name = "snow-parser"; path = "snow-parser" },
    @{ name = "snow-semantic"; path = "snow-semantic" },
    @{ name = "snow-ir"; path = "snow-ir" },
    @{ name = "snow-vm"; path = "snow-vm" },
    @{ name = "snow-backend"; path = "snow-backend" }
)

# ---------------------------------------------------------------
# 6. 构建 Maven 命令
# ---------------------------------------------------------------
$mavenArgs = @()

if ($Clean)
{
    $mavenArgs += "clean"
}
$mavenArgs += "package", "install"
if ($SkipTests)
{
    $mavenArgs += "-DskipTests"
}

Write-Info "开始安装模块 (跳过测试: $SkipTests, 先清理: $Clean)"
Write-Info "执行命令: mvn $( $mavenArgs -join ' ' )"
Write-Host ""

$failedModules = @()
$successCount = 0

# ---------------------------------------------------------------
# 7. 构建与安装
# ---------------------------------------------------------------
foreach ($module in $modules)
{
    Write-Info "处理模块: $( $module.name )"

    $modulePath = Join-Path $projectRoot $module.path
    if (-not (Test-Path $modulePath))
    {
        Write-Error-Custom "模块路径不存在: $modulePath"
        $failedModules += $module.name
        continue
    }

    Push-Location $modulePath

    try
    {
        & mvn $mavenArgs
        if ($LASTEXITCODE -eq 0)
        {
            Write-Success "$( $module.name ) 安装成功"
            $successCount++
        }
        else
        {
            Write-Error-Custom "$( $module.name ) 安装失败 (退出码: $LASTEXITCODE)"
            $failedModules += $module.name
        }
    }
    catch
    {
        Write-Error-Custom "$( $module.name ) 执行异常: $_"
        $failedModules += $module.name
    }
    finally
    {
        Pop-Location
    }

    Write-Host ""
}

# ---------------------------------------------------------------
# 8. 输出总结
# ---------------------------------------------------------------
Write-Host "=" * 60
Write-Info "安装总结"
Write-Host "=" * 60

Write-Success "成功安装: $successCount/$( $modules.Count )"

if ($failedModules.Count -gt 0)
{
    Write-Error-Custom "失败的模块: $( $failedModules -join ', ' )"
    exit 1
}
else
{
    Write-Success "所有模块安装完成！"
    exit 0
}