[CmdletBinding()]
param(
    [string]$Server   = '8.138.118.219',
    [string]$SshUser  = 'root',
    [string]$KeyPath  = (Join-Path $env:USERPROFILE '.ssh\southstand_aliyun'),
    [ValidateRange(1, 65535)]
    [int]$LocalPort   = 8080,
    [ValidateRange(1, 65535)]
    [int]$RemotePort  = 8080,
    [int]$RetrySeconds = 5
)

# ===============================================================
# 反向 SSH 隧道：把本机后端端口暴露到阿里云服务器，供手机 App 访问。
#
#   手机  ->  http://8.138.118.219:8080  ->  阿里云 sshd  ->  本机 127.0.0.1:8080
#
# 只转发这一个端口，不会把本机或局域网的其他服务暴露出去。
# 服务器侧已加固：GatewayPorts clientspecified + PermitListen 8080，
# 因此只允许监听 8080 一个转发端口，其它端口一律拒绝。
#
# 断线自动重连。按 Ctrl+C 停止。
# ===============================================================

$ErrorActionPreference = 'Continue'

if (-not (Test-Path -LiteralPath $KeyPath)) {
    throw "找不到 SSH 私钥：$KeyPath"
}

$sshd = (Get-Command ssh.exe -ErrorAction SilentlyContinue).Source
if (-not $sshd) { $sshd = 'ssh.exe' }

# 用数组传参，避免 PowerShell 解析 "-R 0.0.0.0:8080:127.0.0.1:8080" 时出错
$forward = "0.0.0.0:${RemotePort}:127.0.0.1:${LocalPort}"

$attempt = 0
while ($true) {
    $attempt++
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] 连接隧道（第 $attempt 次）：$Server`:$RemotePort -> 127.0.0.1`:$LocalPort" -ForegroundColor Cyan

    & $sshd -i $KeyPath `
        -o "StrictHostKeyChecking=accept-new" `
        -o "ExitOnForwardFailure=yes" `
        -o "ServerAliveInterval=20" `
        -o "ServerAliveCountMax=3" `
        -o "TCPKeepAlive=yes" `
        -N -R $forward "${SshUser}@${Server}"

    $code = $LASTEXITCODE
    Write-Warning "[$(Get-Date -Format 'HH:mm:ss')] 隧道断开（退出码 $code），${RetrySeconds} 秒后重连…"
    Start-Sleep -Seconds $RetrySeconds
}
