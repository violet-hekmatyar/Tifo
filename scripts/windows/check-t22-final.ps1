param([switch]$ResumeAfterTests)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$snapshot = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t22-preservation-$PID.json"
$backend = $null
$backendLog = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t22-backend-$PID.log"
$backendErr = "$backendLog.err"
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")

function Step([scriptblock]$action, [string]$label) {
    Write-Host "Running: $label"
    & $action
    if ($LASTEXITCODE -ne 0) { throw "$label failed" }
    Write-Host "[OK] $label"
}
function Assert-NoListener([int]$port) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "port $port is still listening" }
    Write-Host "[OK] port $port has no listener"
}

Push-Location $root
try {
    if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim() }
    $env:DB_PASSWORD = $env:MYSQL_PASSWORD
    $mysql = docker inspect -f '{{.State.Running}}' apihub-mysql
    $redis = docker inspect -f '{{.State.Running}}' apihub-redis
    if ($mysql -ne "true" -or $redis -ne "true") { throw "MySQL or Redis is not running" }
    Write-Host "[OK] MySQL and Redis are running"

    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshot; $global:LASTEXITCODE = 0 } "old-data fingerprint"
    Write-Host "[OK] T22 has no migration and no seed"
    Step { .\scripts\windows\validate-demo-data.ps1; $global:LASTEXITCODE = 0 } "existing demo validators"
    Step {
        $out = Invoke-MySqlSqlFile -Path "$root\scripts\sql\validate-t21-notification.sql" -HostName localhost -Port 3306 -Username root -Password $env:MYSQL_PASSWORD -ContainerName apihub-mysql -Capture
        $out | ForEach-Object { Write-Host $_ }
        if ($out | Where-Object { $_ -match '\t([1-9][0-9]*)$' }) { throw "notification validation failed" }
    } "notification relations"
    if (-not $ResumeAfterTests) {
        Step { mvn test } "Java full tests"
        Step { py -m pytest -q recommend-service\tests } "Python recommendation tests"
        Step { mvn -DskipTests clean package } "package backend"
    } else {
        if (-not (Test-Path -LiteralPath "$root\target\south-stand-server.jar")) { throw "resume requested but packaged backend is missing" }
        Write-Host "[OK] resuming after previously successful Java/Python tests and packaging"
    }

    Assert-NoListener 8080
    Assert-NoListener 8100
    Step { .\scripts\windows\start-recommend-service.ps1; $global:LASTEXITCODE = 0 } "start Python recommendation"
    $pythonHealth = Invoke-RestMethod "http://127.0.0.1:8100/health" -TimeoutSec 5
    if (-not $pythonHealth.modelReady) { throw "Python recommendation model is not ready" }
    Write-Host "[OK] Python recommendation online: modelVersion=$($pythonHealth.modelVersion)"

    $backend = Start-Process java -ArgumentList @("-jar", "target/south-stand-server.jar", "--server.port=8080") `
        -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
    $ready = $false
    for ($i = 0; $i -lt 80; $i++) {
        Start-Sleep -Milliseconds 250
        if ($backend.HasExited) { throw "backend exited during startup; see $backendErr" }
        try { if ((Invoke-RestMethod "http://127.0.0.1:8080/api/public/health" -TimeoutSec 2).code -eq 0) { $ready = $true; break } } catch { }
    }
    if (-not $ready) { throw "backend startup timeout; see $backendErr" }
    Write-Host "[OK] Java backend started"

    Step { .\scripts\windows\smoke-backend-v1-final.ps1 -Port 8080; $global:LASTEXITCODE = 0 } "Backend V1 core smoke"
    Step { .\scripts\windows\smoke-recommendation.ps1 -Port 8080 -ExpectedMode CF; $global:LASTEXITCODE = 0 } "Python online recommendation"
    Step { .\scripts\windows\stop-recommend-service.ps1; $global:LASTEXITCODE = 0 } "stop Python for fallback"
    Step { .\scripts\windows\smoke-recommendation.ps1 -Port 8080 -ExpectedMode FALLBACK; $global:LASTEXITCODE = 0 } "Python down fallback"
    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot; $global:LASTEXITCODE = 0 } "final old-data preservation"

    $risky = @(git status --porcelain | ForEach-Object { $_.Substring(3) } | Where-Object {
        $_ -match '(^|[\\/])\.env$|application-prod\.ya?ml$|(^|[\\/])(logs?|target|venv|__pycache__)([\\/]|$)|\.(pem|key|p12|jks)$'
    })
    if ($risky.Count -gt 0) { throw "sensitive/generated paths in Git status: $($risky -join ', ')" }
    Write-Host "[OK] no sensitive/generated path is pending for Git"
    Write-Host "T22 Backend V1 final check passed"
} finally {
    if ($backend -and -not $backend.HasExited) {
        Stop-Process -Id $backend.Id -Force
        Wait-Process -Id $backend.Id -Timeout 10 -ErrorAction SilentlyContinue
    }
    .\scripts\windows\stop-recommend-service.ps1 -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $snapshot -Force -ErrorAction SilentlyContinue
    Pop-Location
    foreach ($port in @(8080, 8100)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { Write-Host "[WARN] port $port still listening" }
        else { Write-Host "[OK] port $port has no listener" }
    }
}
