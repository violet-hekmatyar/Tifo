[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080,
    [string]$TaskName = "SouthStand-Backend-$([guid]::NewGuid().ToString('N').Substring(0, 12))",
    [switch]$RunServer
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$powershell = (Get-Command powershell.exe).Source
$scriptPath = $MyInvocation.MyCommand.Path
$arguments = @(
    '-NoProfile'
    '-ExecutionPolicy'
    'Bypass'
    '-File'
    $scriptPath
    '-RunServer'
    '-Port'
    $Port
) -join ' '

if ($RunServer) {
    Set-Location -LiteralPath $root
    $env:SPRING_PROFILES_ACTIVE = 'dev'
    $env:SERVER_PORT = [string]$Port
    $env:APP_FILE_STORAGE_TYPE = 'LOCAL'
    $env:APP_FILE_LOCAL_STORAGE_ROOT = Join-Path $root 'runtime-uploads'
    if (-not $env:MYSQL_PASSWORD) {
        $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim()
    }
    if (-not $env:JWT_SECRET) {
        $env:JWT_SECRET = 'dev_only_change_me_jwt_secret_please_override_in_prod_2026'
    }
    & (Get-Command java).Source '-jar' (Join-Path $root 'target\south-stand-server.jar') "--server.port=$Port"
    exit $LASTEXITCODE
}

$action = New-ScheduledTaskAction -Execute $powershell -Argument $arguments
$principal = New-ScheduledTaskPrincipal `
    -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive `
    -RunLevel Limited
Register-ScheduledTask -TaskName $TaskName -Action $action -Principal $principal -Force | Out-Null
Start-ScheduledTask -TaskName $TaskName
Write-Output $TaskName
