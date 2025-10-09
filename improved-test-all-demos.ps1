# Snow 批量测试脚本(1)
# 用于测试 playground/Demo 目录下的所有示例

param(
    [switch]$NoRun = $false,
    [switch]$Verbose = $false,
    [switch]$StopOnFailure = $false,
    [switch]$Help = $false
)

# 显示帮助信息
if ($Help) {
    Write-Host "Snow 批量测试脚本(1)" -ForegroundColor Green
    Write-Host "========================" -ForegroundColor Green
    Write-Host "用法:" -ForegroundColor Yellow
    Write-Host "  .\improved-test-all-demos.ps1          # 编译并运行所有示例" -ForegroundColor White
    Write-Host "  .\improved-test-all-demos.ps1 -NoRun   # 仅编译所有示例，不运行" -ForegroundColor White
    Write-Host "  .\improved-test-all-demos.ps1 -Verbose # 显示详细测试信息" -ForegroundColor White
    Write-Host "  .\improved-test-all-demos.ps1 -StopOnFailure # 遇到第一个失败时停止测试" -ForegroundColor White
    Write-Host "  .\improved-test-all-demos.ps1 -Help    # 显示此帮助信息" -ForegroundColor White
    exit 0
}

Write-Host "Snow 批量测试脚本(1)" -ForegroundColor Green
Write-Host "========================" -ForegroundColor Green

# 检查Java环境
if (!(Get-Command "java" -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未找到Java环境，请安装JDK 24或更高版本" -ForegroundColor Red
    exit 1
}

# 检查Maven环境
if (!(Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    Write-Host "警告: 未找到Maven，将跳过自动编译步骤" -ForegroundColor Yellow
    $needCompile = $false
} else {
    $needCompile = $true
}

# 自动编译项目
if ($needCompile) {
    Write-Host "正在编译项目..." -ForegroundColor Cyan
    $compileOutput = mvn compile 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "编译失败，退出测试" -ForegroundColor Red
        Write-Host $compileOutput -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "编译完成" -ForegroundColor Green
}

# 构造命令行参数
$cmdArgs = @()
if ($NoRun) {
    $cmdArgs += "--no-run"
}
if ($Verbose) {
    $cmdArgs += "--verbose"
}
if ($StopOnFailure) {
    $cmdArgs += "--stop-on-failure"
}

# 运行测试命令
Write-Host "开始批量测试所有Demo..." -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

# 创建输出目录
if (!(Test-Path "target")) {
    New-Item -ItemType Directory -Path "target" | Out-Null
}

java -cp "target/classes" org.jcnc.snow.cli.SnowCLI test-all @cmdArgs
$exitCode = $LASTEXITCODE

Write-Host "========================" -ForegroundColor Cyan

if ($exitCode -eq 0) {
    Write-Host "所有测试完成!" -ForegroundColor Green
} else {
    Write-Host "测试完成，但有错误发生" -ForegroundColor Yellow
}

exit $exitCode