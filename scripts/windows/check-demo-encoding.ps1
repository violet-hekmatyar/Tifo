$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$sqlPath = Join-Path $root "scripts\sql\check-demo-encoding.sql"
$seedPath = Join-Path $root "scripts\sql\seed-demo.sql"
$auditDir = Join-Path $root "tmp\encoding-audit"
$javaProcess = $null
$port = 8080
$mysqlHostName = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
$mysqlPortNumber = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$mysqlUserName = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
$mysqlPasswordValue = if ($null -ne $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "" }
$mysqlContainerName = if ($env:MYSQL_CONTAINER) { $env:MYSQL_CONTAINER } else { "apihub-mysql" }
$oldMysqlPassword = $env:MYSQL_PASSWORD
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")

function Invoke-Python($arguments) {
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python -and $python.Source -notlike "*WindowsApps*") { & $python.Source @arguments }
    elseif (Get-Command py -ErrorAction SilentlyContinue) { & py -3 @arguments }
    else { throw "Python is required for raw UTF-8 response verification" }
    if ($LASTEXITCODE -ne 0) { throw "Python encoding verification failed" }
}

function Stop-AuditListeners {
    Get-NetTCPConnection -LocalPort 8080,8090 -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.OwningProcess -eq $javaProcess.Id } |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
}

Push-Location $root
try {
    foreach ($path in @($sqlPath, $seedPath, "target\south-stand-server.jar")) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Required file missing: $path" }
    }
    if (-not $env:MYSQL_PASSWORD -and (Get-Command docker -ErrorAction SilentlyContinue)) {
        $containerId = & docker ps --filter "name=^/$mysqlContainerName$" --format "{{.ID}}"
        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $env:MYSQL_PASSWORD = (& docker exec $mysqlContainerName printenv MYSQL_ROOT_PASSWORD).Trim()
            $mysqlPasswordValue = $env:MYSQL_PASSWORD
        }
    }

    Invoke-Python @("scripts/data/check-demo-encoding.py", "seed", "scripts/sql/seed-demo.sql")

    $databaseOutput = Invoke-MySqlSqlFile -Path $sqlPath -HostName $mysqlHostName -Port $mysqlPortNumber `
        -Username $mysqlUserName -Password $mysqlPasswordValue -ContainerName $mysqlContainerName -Capture
    $databaseOutput | ForEach-Object { Write-Host $_ }
    $badDatabaseChecks = $databaseOutput | Where-Object {
        $_ -match '^(league_encoding|team_encoding|player_encoding|profile_encoding|content_title_encoding|content_body_encoding|comment_encoding)\t' -and
        $_ -notmatch '\t0$'
    }
    if ($badDatabaseChecks) { throw "Database encoding checks failed: $($badDatabaseChecks -join '; ')" }

    $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) { $port = 8090 }
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        throw "Ports 8080 and 8090 are both in use"
    }
    New-Item -ItemType Directory -Force -Path $auditDir | Out-Null
    $javaProcess = Start-Process -FilePath "java" -ArgumentList @("-jar", "target/south-stand-server.jar", "--server.port=$port") `
        -WorkingDirectory $root -WindowStyle Hidden -PassThru
    $started = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        if ($javaProcess.HasExited) { throw "Backend exited before encoding checks" }
        try {
            $health = Invoke-RestMethod "http://localhost:$port/api/public/health" -TimeoutSec 3
            if ($health.code -eq 0) { $started = $true; break }
        } catch {}
    }
    if (-not $started) { throw "Backend startup timeout" }

    $requests = @{
        feed = "/api/app/feed?tab=recommend&pageNum=1&pageSize=20"
        news = "/api/app/feed?tab=news&pageNum=1&pageSize=20"
        teams = "/api/app/search/entities?keyword=Demo%20Team&entityType=TEAM&pageNum=1&pageSize=20"
        players = "/api/app/search/entities?keyword=Demo%20Player&entityType=PLAYER&pageNum=1&pageSize=20"
        profile = "/api/app/users/11000000000000001/profile"
    }
    foreach ($name in $requests.Keys) {
        & curl.exe -sS -D (Join-Path $auditDir "$name.headers") -o (Join-Path $auditDir "$name.json") `
            "http://localhost:$port$($requests[$name])"
        if ($LASTEXITCODE -ne 0) { throw "HTTP request failed: $name" }
    }

    Invoke-Python @("scripts/data/check-demo-encoding.py", "http", "tmp/encoding-audit")
    Write-Host "T14 demo encoding check passed"
} finally {
    if ($javaProcess -and -not $javaProcess.HasExited) {
        Stop-Process -Id $javaProcess.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
    Stop-AuditListeners
    if ($null -ne $oldMysqlPassword) { $env:MYSQL_PASSWORD = $oldMysqlPassword }
    else { Remove-Item Env:MYSQL_PASSWORD -ErrorAction SilentlyContinue }
    Pop-Location
}
