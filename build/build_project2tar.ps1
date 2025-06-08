# 设定 tar 包的名称
$tarName = "Snow.tar"

# 获取脚本当前目录（build文件夹）
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# 获取上一级目录（snow 根目录）
$parentDir = Split-Path -Parent $scriptDir

# 设置 tar 包的完整路径
$tarPath = Join-Path $parentDir $tarName

# 输出开始创建 tar 包的消息
Write-Output "开始创建 tar 包：$tarName 到 $parentDir ..."

# 如果存在旧 tar 包，先删除它
if (Test-Path $tarPath) {
    Write-Output "发现旧的 $tarName，正在删除..."
    Remove-Item $tarPath -Force
}

# 确保 tar 命令可用
$tarCommand = "tar"
if (-not (Get-Command $tarCommand -ErrorAction SilentlyContinue)) {
    Write-Error "❌ tar 命令不可用。请确保 tar 已安装并可在 PowerShell 中执行。"
    exit 1
}

# 执行打包操作：切换到 org\jcnc 目录下再压缩 snow 文件夹
try {
    # 构建命令并执行
    $tarCommandArgs = "-cf", $tarPath, "-C", "$scriptDir\..\src\main\java\org\jcnc", "snow"
    Write-Output "执行 tar 命令: tar $tarCommandArgs"

    & $tarCommand @tarCommandArgs
} catch {
    Write-Error "❌ 创建 tar 包失败。错误信息：$_"
    exit 1
}

# 检查 tar 包是否创建成功
if (Test-Path $tarPath) {
    Write-Output "✅ 成功创建 $tarName"
} else {
    Write-Error "❌ 创建失败，请检查 tar 命令和路径是否正确。"
    exit 1
}
