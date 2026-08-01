$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$javaProcess = $null
$oldPassword = $env:MYSQL_PASSWORD
$outLog = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t14-network.out.log"
$errorLog = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t14-network.err.log"

function Stop-DevelopmentListener([int]$port) {
    foreach ($listener in @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
        if ($process.Name -notmatch "^java(.exe)?$") { throw "Port $port is owned by non-Java PID $($listener.OwningProcess)" }
        Write-Host "Stopping existing development Java listener: port=$port PID=$($listener.OwningProcess)"
        Stop-Process -Id $listener.OwningProcess -Force
        Wait-Process -Id $listener.OwningProcess -Timeout 10 -ErrorAction SilentlyContinue
    }
}

function Assert-NoListener([int]$port) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) {
        throw "Port $port still has a listener"
    }
}

Push-Location $root
try {
    Write-Host "Checking MySQL and Redis"
    $mysqlId = docker ps --filter "name=^/apihub-mysql$" --format "{{.ID}}"
    $redisId = docker ps --filter "name=^/apihub-redis$" --format "{{.ID}}"
    if ([string]::IsNullOrWhiteSpace($mysqlId)) { throw "apihub-mysql is not running" }
    if ([string]::IsNullOrWhiteSpace($redisId)) { throw "apihub-redis is not running" }
    if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim() }
    docker exec apihub-mysql mysqladmin ping -uroot "-p$env:MYSQL_PASSWORD" --silent | Out-Null
    if ((docker exec apihub-redis redis-cli ping).Trim() -ne "PONG") { throw "Redis ping failed" }
    Write-Host "[OK] MySQL and Redis"

    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Jar build failed" }
    Stop-DevelopmentListener 8080
    Stop-DevelopmentListener 8090
    Remove-Item $outLog,$errorLog -Force -ErrorAction SilentlyContinue
    $javaProcess = Start-Process java -ArgumentList @("-jar","target/south-stand-server.jar","--server.port=8080") `
        -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $outLog -RedirectStandardError $errorLog -PassThru

    $started = $false
    for ($i=0; $i -lt 60; $i++) {
        Start-Sleep -Milliseconds 500
        if ($javaProcess.HasExited) { throw "Backend exited during startup" }
        try {
            $health = Invoke-RestMethod "http://127.0.0.1:8080/api/public/health" -TimeoutSec 2
            if ($health.code -eq 0) { $started = $true; break }
        } catch {}
    }
    if (-not $started) { throw "Backend startup timed out" }

    & "$PSScriptRoot\check-backend-connectivity.ps1" -Port 8080
    if ($LASTEXITCODE -ne 0) { throw "Connectivity check failed" }
    & "$PSScriptRoot\benchmark-backend-api.ps1" -Port 8080
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed" }
    & "$PSScriptRoot\smoke-demo-data.ps1" -Port 8080
    if ($LASTEXITCODE -ne 0) { throw "T14 demo smoke failed" }
} finally {
    if ($javaProcess -and -not $javaProcess.HasExited) {
        Stop-Process -Id $javaProcess.Id -Force -ErrorAction SilentlyContinue
        Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
    if ($null -eq $oldPassword) { Remove-Item Env:MYSQL_PASSWORD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PASSWORD = $oldPassword }
    Pop-Location
}

Assert-NoListener 8080
Assert-NoListener 8090
Write-Host "T14 network check passed"
