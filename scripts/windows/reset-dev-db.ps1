param(
    [switch]$ConfirmReset,
    [string]$ConfirmationText = ""
)

$ErrorActionPreference = "Stop"

$mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
$mysqlPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$mysqlUsername = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
$mysqlPassword = if ($null -ne $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "" }
$mysqlContainer = if ($env:MYSQL_CONTAINER) { $env:MYSQL_CONTAINER } else { "apihub-mysql" }

$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$schemaPath = Join-Path $root "scripts\sql\schema.sql"
$seedPath = Join-Path $root "scripts\sql\seed.sql"
$resetPath = Join-Path $root "scripts\sql\reset-dev.sql"
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")

function Require-File($path) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required SQL file missing: $path"
    }
}

function Invoke-MysqlFile($path) {
    Invoke-MySqlSqlFile -Path $path -HostName $mysqlHost -Port $mysqlPort -Username $mysqlUsername `
        -Password $mysqlPassword -ContainerName $mysqlContainer
}

function Invoke-MysqlQuery($query) {
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysql) {
        $oldPwd = $env:MYSQL_PWD
        try {
            if ($mysqlPassword) {
                $env:MYSQL_PWD = $mysqlPassword
            } else {
                Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
            }
            & $mysql.Source --default-character-set=utf8mb4 -h $mysqlHost -P $mysqlPort -u $mysqlUsername -D south_stand -e $query
            if ($LASTEXITCODE -ne 0) {
                throw "mysql query failed"
            }
        } finally {
            if ($null -ne $oldPwd) {
                $env:MYSQL_PWD = $oldPwd
            } else {
                Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
            }
        }
        return
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if ($docker) {
        $containerId = (& $docker.Source ps --filter "name=^/$mysqlContainer$" --format "{{.ID}}")
        if (-not [string]::IsNullOrWhiteSpace($containerId)) {
            $query | & $docker.Source exec -i $mysqlContainer sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot -D south_stand'
            if ($LASTEXITCODE -ne 0) {
                throw "docker mysql query failed"
            }
            return
        }
    }

    throw "mysql command not found. Install MySQL client or set MYSQL_CONTAINER to an available MySQL container."
}

Require-File $resetPath
Require-File $schemaPath
Require-File $seedPath

if (-not $ConfirmReset -or $ConfirmationText -ne "RESET south_stand") {
    throw "Destructive reset refused. Re-run with -ConfirmReset -ConfirmationText 'RESET south_stand'."
}

Write-Warning "Destructive operation confirmed: all data in south_stand will be replaced."
Invoke-MysqlFile $resetPath
Invoke-MysqlFile $schemaPath
Invoke-MysqlFile $seedPath

Write-Host "Checking core table counts..."
Invoke-MysqlQuery "SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user UNION ALL SELECT 'football_team', COUNT(*) FROM football_team UNION ALL SELECT 'content', COUNT(*) FROM content;"

Write-Host "Development database reset completed."
