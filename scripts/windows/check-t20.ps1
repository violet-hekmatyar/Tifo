$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$snapshot = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t20-preservation-$PID.json"
$backend = $null
$backendLog = Join-Path ([IO.Path]::GetTempPath()) "south-stand-t20-backend-$PID.log"
$backendErr = "$backendLog.err"

function Step([scriptblock]$Action, [string]$Label) {
    Write-Host "Running: $Label"; & $Action
    if ($LASTEXITCODE -ne 0) { throw "$Label failed" }
    Write-Host "[OK] $Label"
}

Push-Location $root
try {
    Step { .\scripts\windows\check-t19.ps1; $global:LASTEXITCODE = 0 } "T03-T19 full regression"
    if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = (docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim() }
    $env:DB_PASSWORD = $env:MYSQL_PASSWORD
    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshot } "T20 old-data fingerprint"
    Write-Host "[OK] T20 has no migration and no seed"
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
    Step { .\scripts\windows\smoke-detail-page-contract.ps1 -Port 8080; $global:LASTEXITCODE = 0 } "T20 detail contract smoke"
    Step { .\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot; $global:LASTEXITCODE = 0 } "T20 final old-data preservation"
    Write-Host "T20 check passed"
} finally {
    if ($backend -and -not $backend.HasExited) { Stop-Process -Id $backend.Id -Force; Wait-Process -Id $backend.Id -Timeout 10 -ErrorAction SilentlyContinue }
    .\scripts\windows\stop-recommend-service.ps1 -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $snapshot -Force -ErrorAction SilentlyContinue
    Pop-Location
    foreach ($port in @(8080,8100)) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { Write-Host "[WARN] port $port still listening" } else { Write-Host "[OK] port $port has no listener" }
    }
}
