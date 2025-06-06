$tarName = "Snow.tar"

# 脚本当前目录（build文件夹）
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

# 上一级目录（snow根目录）
$parentDir = Split-Path -Parent $scriptDir

# tar包的完整路径
$tarPath = Join-Path $parentDir $tarName

# 如果存在旧tar包，删除
if (Test-Path $tarPath) {
    Remove-Item $tarPath
}

Write-Output "正在创建新的 $tarName 到 $parentDir ..."

# 打包：先切换到 org\jcnc 目录下再压缩 snow 文件夹
& tar -cf $tarPath -C "$scriptDir\..\src\main\java\org\jcnc" snow

if (Test-Path $tarPath) {
    Write-Output "✅ 成功创建 $tarName"
} else {
    Write-Output "❌ 创建失败，请检查 tar 命令"
}
