$ErrorActionPreference="Stop"
$root=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$snapshot=Join-Path ([IO.Path]::GetTempPath()) "south-stand-t21-preservation-$PID.json"
$backend=$null;$log=Join-Path ([IO.Path]::GetTempPath()) "south-stand-t21-backend-$PID.log";$err="$log.err"
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")
function Step([scriptblock]$a,[string]$l){Write-Host "Running: $l";&$a;if($LASTEXITCODE-ne0){throw"$l failed"};Write-Host "[OK] $l"}
function Invoke-T21Mysql($path,[switch]$Capture){Invoke-MySqlSqlFile -Path $path -HostName localhost -Port 3306 -Username root -Password $env:MYSQL_PASSWORD -ContainerName apihub-mysql -Capture:$Capture}
Push-Location $root
try{
 if(-not$env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD=(docker exec apihub-mysql printenv MYSQL_ROOT_PASSWORD).Trim()};$env:DB_PASSWORD=$env:MYSQL_PASSWORD
 Step{.\scripts\windows\check-existing-data-preserved.ps1 -Phase Before -SnapshotPath $snapshot}"old-data fingerprint"
 Step{Invoke-T21Mysql "$root\scripts\sql\migrations\V019__notification_center.sql"}"V019 migration"
 Step{$out=Invoke-T21Mysql "$root\scripts\sql\validate-t21-notification.sql" -Capture;$out|ForEach-Object{Write-Host $_};$bad=@($out|Where-Object{$_-match'\t([1-9][0-9]*)$'});if($bad){throw"notification validation failed: $($bad-join'; ')"}}"T21 validation before"
 Step{.\scripts\windows\check-t20.ps1;$global:LASTEXITCODE=0}"T03-T20 full regression"
 if(Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue){throw"port 8080 in use"}
 $backend=Start-Process java -ArgumentList @("-jar","target/south-stand-server.jar","--server.port=8080") -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError $err -PassThru
 $ready=$false;for($i=0;$i-lt80;$i++){Start-Sleep -Milliseconds 250;if($backend.HasExited){throw"backend exited: $err"};try{if((Invoke-RestMethod "http://127.0.0.1:8080/api/public/health" -TimeoutSec 2).code-eq0){$ready=$true;break}}catch{}}
 if(-not$ready){throw"backend startup timeout"}
 Step{.\scripts\windows\smoke-notification-center.ps1 -Port 8080;$global:LASTEXITCODE=0}"T21 notification smoke"
 Step{$out=Invoke-T21Mysql "$root\scripts\sql\validate-t21-notification.sql" -Capture;$out|ForEach-Object{Write-Host $_};$bad=@($out|Where-Object{$_-match'\t([1-9][0-9]*)$'});if($bad){throw"notification validation failed: $($bad-join'; ')"}}"T21 validation after"
 Step{.\scripts\windows\check-existing-data-preserved.ps1 -Phase After -SnapshotPath $snapshot;$global:LASTEXITCODE=0}"final old-data preservation"
 Write-Host "T21 check passed"
}finally{
 if($backend-and-not$backend.HasExited){Stop-Process -Id $backend.Id -Force;Wait-Process -Id $backend.Id -Timeout 10 -ErrorAction SilentlyContinue}
 .\scripts\windows\stop-recommend-service.ps1 -ErrorAction SilentlyContinue;Remove-Item -LiteralPath $snapshot -Force -ErrorAction SilentlyContinue;Pop-Location
 foreach($port in @(8080,8100)){if(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue){Write-Host "[WARN] port $port still listening"}else{Write-Host "[OK] port $port has no listener"}}
}
