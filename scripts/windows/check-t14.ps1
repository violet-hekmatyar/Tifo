$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$javaProcess = $null
$tempStorage = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t14-uploads"
$oldEnv = @{}
foreach ($name in @("SPRING_PROFILES_ACTIVE","SERVER_PORT","MYSQL_HOST","MYSQL_PORT","MYSQL_DATABASE","MYSQL_USERNAME","MYSQL_PASSWORD","REDIS_HOST","REDIS_PORT","REDIS_PASSWORD","JWT_SECRET","APP_FILE_STORAGE_TYPE","APP_FILE_LOCAL_STORAGE_ROOT")) {
    $oldEnv[$name] = [Environment]::GetEnvironmentVariable($name)
}

function Invoke-Step($command, $label) {
    Write-Host ""
    Write-Host "Running: $label"
    Invoke-Expression $command
    if ($LASTEXITCODE -ne 0) { throw "$label failed with exit code $LASTEXITCODE" }
    Write-Host "[OK] $label"
}
function Stop-PortListener([int]$port) {
    $listeners = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Write-Host "Stopping existing listener on port $port, PID $($listener.OwningProcess)"
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction Stop
    }
}
function Restore-Environment {
    foreach ($entry in $oldEnv.GetEnumerator()) {
        if ($null -eq $entry.Value) { Remove-Item "Env:$($entry.Key)" -ErrorAction SilentlyContinue }
        else { Set-Item "Env:$($entry.Key)" $entry.Value }
    }
}

Push-Location $root
try {
    Write-Host "T14 deterministic demo dataset check"
    $required = @(
        "scripts/data/generate-demo-data.py", "scripts/data/validate-demo-data.py", "scripts/data/demo-config.json", "scripts/data/demo-names.json",
        "scripts/sql/seed-demo.sql", "scripts/sql/validate-demo-data.sql", "scripts/windows/init-demo-data.ps1", "scripts/windows/smoke-demo-data.ps1",
        "scripts/windows/smoke-auth.ps1", "scripts/windows/smoke-football.ps1", "scripts/windows/smoke-feed.ps1", "scripts/windows/smoke-file-upload.ps1",
        "scripts/windows/smoke-storage-media.ps1", "scripts/windows/smoke-user-social.ps1", "scripts/windows/smoke-comment-hot.ps1", "scripts/windows/smoke-content-article.ps1"
    )
    foreach ($path in $required) { if (-not (Test-Path -LiteralPath $path)) { throw "Required file missing: $path" } }
    Write-Host "[OK] required files"

    $env:MYSQL_HOST = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
    $env:MYSQL_PORT = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
    $env:MYSQL_DATABASE = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "south_stand" }
    $env:MYSQL_USERNAME = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
    $env:REDIS_HOST = if ($env:REDIS_HOST) { $env:REDIS_HOST } else { "localhost" }
    $env:REDIS_PORT = if ($env:REDIS_PORT) { $env:REDIS_PORT } else { "6379" }
    $env:JWT_SECRET = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "dev_only_change_me_jwt_secret_please_override_in_prod_2026" }
    $env:APP_FILE_STORAGE_TYPE = "LOCAL"
    $env:APP_FILE_LOCAL_STORAGE_ROOT = $tempStorage
    if (-not $env:MYSQL_PASSWORD -and (Get-Command docker -ErrorAction SilentlyContinue)) {
        $containerId = docker ps --filter "name=^/apihub-mysql$" --format "{{.ID}}"
        if (-not [string]::IsNullOrWhiteSpace($containerId)) { $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim() }
    }

    Invoke-Step "py -3 scripts/data/validate-demo-data.py" "offline generator validation"
    Invoke-Step ".\scripts\windows\init-demo-data.ps1 -SkipGenerate" "demo database initialization"
    Invoke-Step "mvn test" "mvn test"
    Invoke-Step "mvn clean package" "mvn clean package"

    Stop-PortListener 8080
    Stop-PortListener 8090
    $port = 8080
    $env:SPRING_PROFILES_ACTIVE = "dev"
    $env:SERVER_PORT = "$port"
    $javaProcess = Start-Process -FilePath "java" -ArgumentList @("-jar","target/south-stand-server.jar","--server.port=$port") -WorkingDirectory $root -WindowStyle Hidden -PassThru
    $started = $false
    for ($i=0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        if ($javaProcess.HasExited) { throw "application exited early with code $($javaProcess.ExitCode)" }
        try { $health = Invoke-RestMethod "http://localhost:$port/api/public/health" -TimeoutSec 5; if ($health.code -eq 0) { $started=$true; break } } catch {}
    }
    if (-not $started) { throw "application startup timeout" }

    foreach ($smoke in @("smoke-auth","smoke-football","smoke-feed","smoke-file-upload","smoke-storage-media","smoke-user-social","smoke-comment-hot","smoke-content-article","smoke-demo-data")) {
        Invoke-Step ".\scripts\windows\$smoke.ps1 -Port $port" "$smoke.ps1"
    }
    Write-Host ""
    Write-Host "T14 check passed"
} catch {
    Write-Host "[FAIL] $($_.Exception.Message)"
    Write-Host "T14 check failed"
    exit 1
} finally {
    if ($javaProcess -and -not $javaProcess.HasExited) {
        Stop-Process -Id $javaProcess.Id -Force
        Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $tempStorage -Recurse -Force -ErrorAction SilentlyContinue
    Restore-Environment
    Pop-Location
    foreach ($port in @(8080,8090)) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { Write-Host "[WARN] port $port still has a listener" }
        else { Write-Host "[OK] port $port has no listener" }
    }
}
