$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$snapshot = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t19-preservation-$PID.json"
$backend = $null
$backendLog = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t19-backend-$PID.log"
$backendErr = "$backendLog.err"

function Step([scriptblock]$Action, [string]$Label) {
    Write-Host "Running: $Label"
    & $Action
    if ($LASTEXITCODE -ne 0) { throw "$Label failed" }
    Write-Host "[OK] $Label"
}

Push-Location $root
try {
    if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim() }
    Step {
        foreach ($container in @("apihub-mysql", "apihub-redis")) {
            $running = (docker inspect -f "{{.State.Running}}" $container 2>$null).Trim()
            if ($running -ne "true") { throw "$container is not running" }
        }
        foreach ($port in @(3306, 6379)) {
            if (-not (Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet)) {
                throw "port $port is not reachable"
            }
        }
    } "MySQL/Redis connectivity"
    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshot } "old-data fingerprint"
    Write-Host "[OK] T19 has no migration and no incremental seed"
    Step { py -m pytest -q recommend-service\tests } "Python tests"
    $env:DB_PASSWORD = $env:MYSQL_PASSWORD
    Step { .\scripts\windows\start-recommend-service.ps1 } "start Python CF"
    $health = Invoke-RestMethod "http://127.0.0.1:8100/health" -TimeoutSec 3
    if (-not $health.modelReady) { throw "Python model is not ready" }
    Step { mvn test } "mvn test"
    Step { mvn -DskipTests clean package } "mvn clean package"
    if (Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue) { throw "port 8080 is already in use" }
    $backend = Start-Process java -ArgumentList @("-jar", "target/south-stand-server.jar", "--server.port=8080") `
        -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
    $ready = $false
    for ($i = 0; $i -lt 80; $i++) {
        Start-Sleep -Milliseconds 250
        if ($backend.HasExited) { throw "backend exited during startup; see $backendErr" }
        try { if ((Invoke-RestMethod "http://127.0.0.1:8080/api/public/health" -TimeoutSec 2).code -eq 0) { $ready = $true; break } } catch { }
    }
    if (-not $ready) { throw "backend startup timeout; see $backendErr" }
    foreach ($smoke in @("smoke-auth","smoke-football","smoke-feed","smoke-file-upload","smoke-storage-media","smoke-user-social","smoke-comment-hot","smoke-content-article","smoke-demo-data","smoke-football-ranks","smoke-team-player-detail","smoke-match-lineup-stats-rating")) {
        Step { & ".\scripts\windows\$smoke.ps1" -Port 8080; $global:LASTEXITCODE = 0 } "$smoke.ps1"
    }
    Step { .\scripts\windows\smoke-recommendation.ps1 -Port 8080 -ExpectedMode RULE; $global:LASTEXITCODE = 0 } "T18 RULE smoke"
    Step { .\scripts\windows\smoke-recommendation.ps1 -Port 8080 -ExpectedMode CF; $global:LASTEXITCODE = 0 } "T18 CF smoke"
    Step { .\scripts\windows\smoke-home-card-system.ps1 -Port 8080 -ExpectedMode RULE; $global:LASTEXITCODE = 0 } "T19 RULE smoke"
    Step { .\scripts\windows\smoke-home-card-system.ps1 -Port 8080 -ExpectedMode CF; $global:LASTEXITCODE = 0 } "T19 CF smoke"
    Step { .\scripts\windows\stop-recommend-service.ps1; $global:LASTEXITCODE = 0 } "stop Python for fallback"
    Step { .\scripts\windows\smoke-recommendation.ps1 -Port 8080 -ExpectedMode FALLBACK; $global:LASTEXITCODE = 0 } "T18 fallback smoke"
    Step { .\scripts\windows\smoke-home-card-system.ps1 -Port 8080 -ExpectedMode FALLBACK; $global:LASTEXITCODE = 0 } "T19 fallback smoke"
    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot; $global:LASTEXITCODE = 0 } "final old-data preservation"
    Write-Host "T19 check passed"
} finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force; Wait-Process -Id $backend.Id -Timeout 10 -ErrorAction SilentlyContinue }
    .\scripts\windows\stop-recommend-service.ps1 -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $snapshot -Force -ErrorAction SilentlyContinue
    Pop-Location
    foreach ($port in @(8080,8100)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { Write-Host "[WARN] port $port still listening" } else { Write-Host "[OK] port $port has no listener" }
    }
}
