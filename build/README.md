### 使用 `build-project2tar.ps1` 脚本

在执行 `build-project2tar.ps1` 脚本之前，您需要确保 PowerShell 的执行策略允许运行脚本。默认情况下，PowerShell 可能阻止未签名的脚本执行。因此，您需要设置适当的执行策略。

#### 步骤 1: 以管理员身份打开 PowerShell

* 在 Windows 系统中，搜索 **PowerShell**，右键点击 **Windows PowerShell**，并选择 **以管理员身份运行**。

#### 步骤 2: 设置 PowerShell 执行策略

为了允许执行 PowerShell 脚本，您需要调整当前用户的执行策略。输入以下命令并按 Enter: 

```powershell
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```

#### 解释: 

* `-Scope CurrentUser`: 此参数指定该执行策略仅对当前用户有效，而不会影响系统范围内的其他用户。
* `-ExecutionPolicy RemoteSigned`: 此策略表示: 

    * 本地创建的脚本可以直接运行。
    * 从互联网下载的脚本必须具备有效的数字签名才能运行。没有签名的脚本将无法执行，除非您先解除阻止该脚本。

#### 步骤 3: 运行 `build-project2tar.ps1` 脚本

设置完成后，您可以在 PowerShell 中运行 `build-project2tar.ps1` 脚本。确保您已经切换到包含该脚本的目录，或提供完整的文件路径来执行它。

```powershell
.\build-project2tar.ps1
```