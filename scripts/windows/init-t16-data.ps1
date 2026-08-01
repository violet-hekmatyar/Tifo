$ErrorActionPreference = "Stop"
$root=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$hostName=if($env:MYSQL_HOST){$env:MYSQL_HOST}else{"localhost"}
$port=if($env:MYSQL_PORT){$env:MYSQL_PORT}else{"3306"}
$username=if($env:MYSQL_USERNAME){$env:MYSQL_USERNAME}else{"root"}
$password=if($null-ne $env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD}else{""}
$container=if($env:MYSQL_CONTAINER){$env:MYSQL_CONTAINER}else{"apihub-mysql"}
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")
$migration=Join-Path $root "scripts\sql\migrations\V016__team_roster_player_career.sql"
$seed=Join-Path $root "scripts\sql\seed-t16-incremental.sql"
$validation=Join-Path $root "scripts\sql\validate-t16-incremental.sql"
foreach($path in @($migration,$seed,$validation)){if(-not(Test-Path -LiteralPath $path)){throw "Required file missing: $path"}}
Invoke-MySqlSqlFile -Path $migration -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container
Invoke-MySqlSqlFile -Path $seed -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container
$output=Invoke-MySqlSqlFile -Path $validation -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container -Capture
$output|ForEach-Object{Write-Host $_}
$bad=$output|Where-Object{$_ -match '^[^\t]+\t([1-9][0-9]*)$'}
if($bad){throw "T16 validation found anomalies: $($bad -join '; ')"}
Write-Host "T16 incremental data initialized and validated"
