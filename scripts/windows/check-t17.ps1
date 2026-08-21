$ErrorActionPreference="Stop";$root=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path;$process=$null;$snapshot=Join-Path ([IO.Path]::GetTempPath()) "south-stand-t17-preservation-$PID.json";$log=Join-Path ([IO.Path]::GetTempPath()) "south-stand-t17-$PID.log";$err="$log.err"
function Step([scriptblock]$a,[string]$label){Write-Host "";Write-Host "Running: $label";&$a;if($LASTEXITCODE-ne0){throw "$label failed"};Write-Host "[OK] $label"}
function Stop-Port([int]$p){foreach($x in @(Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue)){Stop-Process -Id $x.OwningProcess -Force}}
Push-Location $root
try{
 if(-not$env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD=(docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim()}
 Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshot} "capture old-data fingerprints"
 Step {.\scripts\windows\init-t17-data.ps1} "T17 incremental migration, seed and validation"
 Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot} "verify old data preserved"
 Step {.\scripts\windows\validate-demo-data.ps1} "validate complete demo dataset"
 Stop-Port 8080;Stop-Port 8090;Step {mvn test} "mvn test";Step {mvn clean package} "mvn clean package"
 $process=Start-Process java -ArgumentList @("-jar","target/south-stand-server.jar","--server.port=8080") -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
 $ready=$false;for($i=0;$i-lt60;$i++){Start-Sleep -Milliseconds 500;if($process.HasExited){throw "backend exited"};try{if((Invoke-RestMethod "http://localhost:8080/api/public/health" -TimeoutSec 2).code-eq0){$ready=$true;break}}catch{}};if(-not$ready){throw "backend startup timeout"}
 foreach($s in @("smoke-auth","smoke-football","smoke-feed","smoke-file-upload","smoke-storage-media","smoke-user-social","smoke-comment-hot","smoke-content-article","smoke-demo-data","smoke-football-ranks","smoke-team-player-detail","smoke-match-lineup-stats-rating")){Step {& ".\scripts\windows\$s.ps1" -Port 8080} "$s.ps1"}
 Step {.\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot} "verify preservation after smoke";Write-Host "T17 check passed"
}catch{Write-Host "[FAIL] $($_.Exception.Message)";if(Test-Path $log){Get-Content -Encoding UTF8 $log|Select-Object -Last 80};if(Test-Path $err){Get-Content -Encoding UTF8 $err|Select-Object -Last 30};exit 1}finally{if($process-and-not$process.HasExited){Stop-Process -Id $process.Id -Force;Wait-Process -Id $process.Id -Timeout 10 -ErrorAction SilentlyContinue};Remove-Item $snapshot -Force -ErrorAction SilentlyContinue;Pop-Location;foreach($p in @(8080,8090)){if(Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue){Write-Host "[WARN] port $p still listening"}else{Write-Host "[OK] port $p has no listener"}}}
