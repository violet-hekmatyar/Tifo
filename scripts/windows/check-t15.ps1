param(
    [switch]$ResetDatabase,
    [switch]$ConfirmReset
)

$ErrorActionPreference = "Stop"
$root=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$javaProcess=$null
$tempStorage=Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t15-uploads"
$snapshotPath=Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t15-preservation-$PID.json"
$oldEnv=@{}
foreach($name in @("SPRING_PROFILES_ACTIVE","SERVER_PORT","MYSQL_PASSWORD","JWT_SECRET","APP_FILE_STORAGE_TYPE","APP_FILE_LOCAL_STORAGE_ROOT")){$oldEnv[$name]=[Environment]::GetEnvironmentVariable($name)}
function Run-Step([scriptblock]$action,[string]$label){Write-Host "";Write-Host "Running: $label";& $action;if($LASTEXITCODE -ne 0){throw "$label failed"};Write-Host "[OK] $label"}
function Stop-Port([int]$port){foreach($listener in @(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)){Write-Host "Stopping listener port=$port PID=$($listener.OwningProcess)";Stop-Process -Id $listener.OwningProcess -Force}}
function Restore-Env{foreach($entry in $oldEnv.GetEnumerator()){if($null -eq $entry.Value){Remove-Item "Env:$($entry.Key)" -ErrorAction SilentlyContinue}else{Set-Item "Env:$($entry.Key)" $entry.Value}}}

Push-Location $root
try {
    $required=@("scripts/sql/migrations/V015__season_standings_ranks.sql","scripts/sql/seed-t15-incremental.sql","scripts/sql/validate-t15-incremental.sql","scripts/sql/check-existing-data-preserved.sql","scripts/windows/check-existing-data-preserved.ps1","scripts/windows/smoke-football-ranks.ps1","src/main/java/com/southstand/football/rank/service/FootballRankService.java")
    foreach($file in $required){if(-not(Test-Path -LiteralPath $file)){throw "Required file missing: $file"}}
    Write-Host "[OK] T15 key files"
    if(-not $env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD=(docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim()}
    $env:JWT_SECRET=if($env:JWT_SECRET){$env:JWT_SECRET}else{"dev_only_change_me_jwt_secret_please_override_in_prod_2026"}
    $env:APP_FILE_STORAGE_TYPE="LOCAL";$env:APP_FILE_LOCAL_STORAGE_ROOT=$tempStorage

    if($ResetDatabase){
        if(-not $ConfirmReset){throw "-ResetDatabase also requires -ConfirmReset."}
        Run-Step {.\scripts\windows\init-demo-data.ps1 -Mode ResetDemo -ConfirmReset} "explicit ResetDemo initialization"
    } else {
        Run-Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshotPath} "capture existing-data fingerprints"
        Run-Step {.\scripts\windows\init-demo-data.ps1 -Mode Incremental} "non-destructive T15 migration and seed"
        Run-Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshotPath} "verify existing data preserved"
    }

    Stop-Port 8080;Stop-Port 8090
    Run-Step {mvn test} "mvn test"
    Run-Step {mvn clean package} "mvn clean package"
    $env:SPRING_PROFILES_ACTIVE="dev";$env:SERVER_PORT="8080"
    $javaProcess=Start-Process java -ArgumentList @("-jar","target/south-stand-server.jar","--server.port=8080") -WorkingDirectory $root -WindowStyle Hidden -PassThru
    $started=$false
    for($i=0;$i -lt 60;$i++){Start-Sleep -Milliseconds 500;if($javaProcess.HasExited){throw "Backend exited early"};try{$health=Invoke-RestMethod "http://localhost:8080/api/public/health" -TimeoutSec 2;if($health.code -eq 0){$started=$true;break}}catch{}}
    if(-not $started){throw "Backend startup timeout"}
    foreach($smoke in @("smoke-auth","smoke-football","smoke-feed","smoke-file-upload","smoke-storage-media","smoke-user-social","smoke-comment-hot","smoke-content-article","smoke-demo-data","smoke-football-ranks")){Run-Step {& ".\scripts\windows\$smoke.ps1" -Port 8080} "$smoke.ps1"}
    if(-not $ResetDatabase){Run-Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshotPath} "verify preservation after smoke tests"}
    Write-Host "";Write-Host "T15 incremental check passed"
} catch {Write-Host "[FAIL] $($_.Exception.Message)";Write-Host "T15 check failed";exit 1}
finally {
    if($javaProcess -and -not $javaProcess.HasExited){Stop-Process -Id $javaProcess.Id -Force;Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue}
    Remove-Item -LiteralPath $tempStorage -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $snapshotPath -Force -ErrorAction SilentlyContinue
    Restore-Env;Pop-Location
    foreach($port in @(8080,8090)){if(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue){Write-Host "[WARN] port $port still listening"}else{Write-Host "[OK] port $port has no listener"}}
}
