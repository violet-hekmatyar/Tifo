[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080,

    # 只监听本机回环地址，避免同局域网其他设备直接访问后端。
    # Android 模拟器用 10.0.2.2 访问宿主机时会映射到 127.0.0.1，因此不影响模拟器调试。
    # 若确实需要用局域网 IP 调试真机，改为 '0.0.0.0'，但公网穿透场景请保持默认值。
    [string]$BindAddress = '127.0.0.1',

    # 接口文档（/doc.html、/swagger-ui、/v3/api-docs）是匿名可读的。
    # 公网暴露时必须关闭，否则等于把完整 API 清单交给攻击者。本地调试可用 -AllowApiDocs 打开。
    [switch]$AllowApiDocs
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

# ---------------------------------------------------------------
# JWT 密钥：首次运行随机生成并持久化，之后复用。
# 复用是为了让后端重启后已发放的 Token 仍然有效。
# 绝不能使用 application.yml 里那个公开的默认占位串——任何人都能用它伪造管理员令牌。
# ---------------------------------------------------------------
$secretDir = Join-Path $root '.secrets'
$secretFile = Join-Path $secretDir 'jwt_secret.txt'
if (-not (Test-Path -LiteralPath $secretFile)) {
    New-Item -ItemType Directory -Force -Path $secretDir | Out-Null
    $bytes = New-Object byte[] 64
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    [Convert]::ToBase64String($bytes) | Set-Content -LiteralPath $secretFile -NoNewline -Encoding ascii
    Write-Host "[安全] 已生成新的 JWT 密钥：$secretFile" -ForegroundColor Green
    Write-Host "[安全] 请勿提交到 Git，也勿外传。" -ForegroundColor Yellow
}
$env:JWT_SECRET = (Get-Content -LiteralPath $secretFile -Raw).Trim()

if ($env:JWT_SECRET -eq 'dev_only_change_me_jwt_secret_please_override_in_prod_2026') {
    throw 'JWT_SECRET 仍是公开的默认值，拒绝启动。请删除 .secrets\jwt_secret.txt 后重跑以重新生成。'
}

# ---------------------------------------------------------------
# 其余安全与运行参数
# ---------------------------------------------------------------
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:SERVER_ADDRESS = $BindAddress

# 关闭 DEBUG 级别的 SQL 日志洪水（每条 SQL 都打日志，公网有流量时会拖慢并撑大日志文件）
$env:LOGGING_LEVEL_COM_SOUTHSTAND = 'INFO'

if (-not $AllowApiDocs) {
    $env:KNIFE4J_ENABLE = 'false'
    $env:SPRINGDOC_API_DOCS_ENABLED = 'false'
    $env:SPRINGDOC_SWAGGER_UI_ENABLED = 'false'
    Write-Host '[安全] 已关闭公网可读的接口文档（本地调试可加 -AllowApiDocs）' -ForegroundColor Green
} else {
    Write-Host '[警告] 接口文档处于开启状态，请勿在公网暴露时使用' -ForegroundColor Yellow
}

$env:APP_FILE_STORAGE_TYPE = 'LOCAL'
$env:APP_FILE_LOCAL_STORAGE_ROOT = Join-Path $root 'runtime-uploads'

# 本机 MySQL 密码沿用既有脚本的做法，从容器里取，避免明文写死在脚本里
if (-not $env:MYSQL_PASSWORD) {
    try {
        $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim()
    } catch {
        Write-Warning '无法从 apihub-mysql 容器读取 MySQL 密码，将使用环境变量或配置默认值。'
    }
}

Write-Host "启动后端：绑定 $BindAddress`:$Port" -ForegroundColor Cyan
Set-Location -LiteralPath $root
& (Get-Command java).Source '-jar' (Join-Path $root 'target\south-stand-server.jar') "--server.port=$Port"
exit $LASTEXITCODE
