# Snow 模块自动安装脚本 (Windows PowerShell)
# 功能: 按依赖顺序编译和安装所有模块到本地 Maven 仓库

param(
    [switch]$Clean = $false,           # 是否先执行 clean
    [switch]$SkipTests = $false,       # 是否跳过测试
    [string]$MavenHome = ""            # Maven 主目录 (可选，自动检测)
)

# 颜色输出函数
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

# 获取项目根目录 (脚本在 build/module/ 下，需要往上两级)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)

# 验证 Maven 是否可用
Write-Info "正在检查 Maven..."
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Error-Custom "未找到 Maven，请确保 Maven 已安装并配置到 PATH 中"
    exit 1
}

Write-Success "Maven 可用"

# 定义模块及其依赖顺序（依赖必须先安装）
$modules = @(
    @{name="snow-common"; path="snow-common"},
    @{name="snow-lexer"; path="snow-lexer"},
    @{name="snow-parser"; path="snow-parser"},
    @{name="snow-semantic"; path="snow-semantic"},
    @{name="snow-ir"; path="snow-ir"},
    @{name="snow-vm"; path="snow-vm"},
    @{name="snow-backend"; path="snow-backend"}
)

# 构建 Maven 命令参数
$mavenArgs = @()
if ($Clean) {
    $mavenArgs += "clean"
}
$mavenArgs += "package", "install"
if ($SkipTests) {
    $mavenArgs += "-DskipTests"
}

Write-Info "开始安装模块 (跳过测试: $SkipTests, 先清理: $Clean)"
Write-Info "命令: mvn $($mavenArgs -join ' ')"
Write-Host ""

$failedModules = @()
$successCount = 0

# 按顺序安装每个模块
foreach ($module in $modules) {
    Write-Info "处理模块: $($module.name)"
    $modulePath = Join-Path $projectRoot $module.path

    if (-not (Test-Path $modulePath)) {
        Write-Error-Custom "模块路径不存在: $modulePath"
        $failedModules += $module.name
        continue
    }

    # 进入模块目录并执行 Maven 命令
    Push-Location $modulePath

    try {
        & mvn $mavenArgs
        if ($LASTEXITCODE -eq 0) {
            Write-Success "$($module.name) 安装成功"
            $successCount++
        } else {
            Write-Error-Custom "$($module.name) 安装失败 (退出码: $LASTEXITCODE)"
            $failedModules += $module.name
        }
    } catch {
        Write-Error-Custom "$($module.name) 执行异常: $_"
        $failedModules += $module.name
    } finally {
        Pop-Location
    }

    Write-Host ""
}

# 输出总结
Write-Host "=" * 60
Write-Info "安装总结"
Write-Host "=" * 60
Write-Success "成功安装: $successCount/$($modules.Count)"

if ($failedModules.Count -gt 0) {
    Write-Error-Custom "失败的模块: $($failedModules -join ', ')"
    exit 1
} else {
    Write-Success "所有模块安装完成！"
    exit 0
}