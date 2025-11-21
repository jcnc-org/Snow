# Docker Registry 检测

$ErrorActionPreference = "Stop"

# ==== 自动切换到 PowerShell 7 ====
if ($PSVersionTable.PSEdition -ne "Core") {
    Write-Host "检测到 Windows PowerShell 5.x，正在切换到 PowerShell 7..." -ForegroundColor Yellow
    $pwshCmd = Get-Command pwsh.exe -ErrorAction SilentlyContinue

    if (-not $pwshCmd) {
        throw "❌ PowerShell 7 未安装。请安装：https://github.com/PowerShell/PowerShell"
    }

    & $pwshCmd.Source -NoLogo -NoProfile -ExecutionPolicy Bypass -File $PSCommandPath @args
    exit $LASTEXITCODE
}

# ==== 读取配置 ====
$configFile = Join-Path $PSScriptRoot "daemon.json"

if (-not (Test-Path $configFile)) {
    throw "❌ 未找到 daemon.json：$configFile"
}

$config = Get-Content $configFile -Raw | ConvertFrom-Json
$mirrors = $config."registry-mirrors"

if (-not $mirrors) {
    throw "❌ daemon.json 未设置 registry-mirrors"
}

Write-Host "读取到 $($mirrors.Count) 个镜像源：" -ForegroundColor Cyan
$mirrors | ForEach-Object { Write-Host "  - $_" }


Write-Host "`n开始检测 Docker Registry..." -ForegroundColor Cyan

# ==== 并行运行 ====
$jobs = foreach ($m in $mirrors) {
    Start-Job -ScriptBlock {
        param($mirror)

        $res = [ordered]@{
            Mirror      = $mirror
            Registry    = "NO"
            Status      = "FAIL"
            Code        = ""
            ConnectMs   = ""
            ContentType = ""
            PullSeconds = ""
        }

        # ---- 1. 检测 /v2/ ----
        $url = "$mirror/v2/"

        try {
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            $resp = Invoke-WebRequest -Method Get -Uri $url -TimeoutSec 5 -ErrorAction Stop
            $sw.Stop()

            $res.Code        = $resp.StatusCode
            $res.ConnectMs   = $sw.ElapsedMilliseconds
            $res.ContentType = $resp.Headers["Content-Type"]

            if ($resp.StatusCode -in 200,401) {
                $res.Registry = "YES"
            }
        }
        catch {
            return $res
        }

        # ---- 2. 使用 Docker 引擎 pull 测试 ----
        try {
            $sw2 = [System.Diagnostics.Stopwatch]::StartNew()
            docker pull busybox:latest --quiet
            $sw2.Stop()

            $res.PullSeconds = [math]::Round($sw2.Elapsed.TotalSeconds, 2)
            $res.Status = "OK"
        }
        catch {
            $res.PullSeconds = "FAIL"
        }

        return $res

    } -ArgumentList $m
}

Wait-Job $jobs | Out-Null
$results = $jobs | Receive-Job
$jobs | Remove-Job -Force

Write-Host "`n=== Docker Registry 检测结果 ===" -ForegroundColor Yellow
$results | Format-Table -AutoSize

Write-Host "`n✔ 测试完成" -ForegroundColor Green