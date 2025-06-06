使用 build——project2tar.ps1 需要在管理员权限下的 PowerShell 输入下面的内容

Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned

Tip:RemoteSigned 表示：本地创建的脚本可以运行，从互联网下载的脚本必须有签名。