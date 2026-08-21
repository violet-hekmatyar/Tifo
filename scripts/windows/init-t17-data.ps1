$ErrorActionPreference="Stop"
$root=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$hostName=if($env:MYSQL_HOST){$env:MYSQL_HOST}else{"localhost"};$port=if($env:MYSQL_PORT){$env:MYSQL_PORT}else{"3306"};$username=if($env:MYSQL_USERNAME){$env:MYSQL_USERNAME}else{"root"};$password=if($null-ne $env:MYSQL_PASSWORD){$env:MYSQL_PASSWORD}else{""};$container=if($env:MYSQL_CONTAINER){$env:MYSQL_CONTAINER}else{"apihub-mysql"}
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")
$files=@("scripts\sql\migrations\V017__match_lineups_stats_ratings.sql","scripts\sql\seed-t17-roster-expansion.sql","scripts\sql\seed-t17-incremental.sql","scripts\sql\validate-t17-incremental.sql")|ForEach-Object{Join-Path $root $_}
foreach($path in $files){if(-not(Test-Path -LiteralPath $path)){throw "Required file missing: $path"}}
Invoke-MySqlSqlFile -Path $files[0] -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container
Invoke-MySqlSqlFile -Path $files[1] -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container
Invoke-MySqlSqlFile -Path $files[2] -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container
$output=Invoke-MySqlSqlFile -Path $files[3] -HostName $hostName -Port $port -Username $username -Password $password -ContainerName $container -Capture
$output|ForEach-Object{Write-Host $_};$bad=$output|Where-Object{$_ -match '^[^\t]+\t([1-9][0-9]*)$'};if($bad){throw "T17 validation found anomalies: $($bad -join '; ')"}
Write-Host "T17 incremental data initialized and validated"
