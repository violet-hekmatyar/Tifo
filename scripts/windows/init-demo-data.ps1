param(
    [ValidateSet("Incremental", "ResetDemo")][string]$Mode = "Incremental",
    [switch]$ConfirmReset,
    [switch]$SkipGenerate
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$seedDemo = Join-Path $root "scripts\sql\seed-demo.sql"
$migrationT15 = Join-Path $root "scripts\sql\migrations\V015__season_standings_ranks.sql"
$incrementalT15 = Join-Path $root "scripts\sql\seed-t15-incremental.sql"
$validateSql = Join-Path $root "scripts\sql\validate-demo-data.sql"
$validateT15Sql = Join-Path $root "scripts\sql\validate-t15-incremental.sql"
$mysqlHostName = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
$mysqlPortNumber = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$mysqlUserName = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
$mysqlPasswordValue = if ($null -ne $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "" }
$mysqlContainerName = if ($env:MYSQL_CONTAINER) { $env:MYSQL_CONTAINER } else { "apihub-mysql" }
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")

function Invoke-Python($arguments) {
    $python = Get-Command python -ErrorAction SilentlyContinue
    if ($python -and $python.Source -notlike "*WindowsApps*") {
        & $python.Source @arguments
    } elseif (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3 @arguments
    } else {
        throw "Python is unavailable. The committed seed-demo.sql can still be imported with -SkipGenerate."
    }
    if ($LASTEXITCODE -ne 0) { throw "Python command failed" }
}

function Invoke-MysqlFile($path, [switch]$Capture) {
    return Invoke-MySqlSqlFile -Path $path -HostName $mysqlHostName -Port $mysqlPortNumber -Username $mysqlUserName `
        -Password $mysqlPasswordValue -ContainerName $mysqlContainerName -Capture:$Capture
}

Push-Location $root
try {
    foreach ($path in @($migrationT15, $incrementalT15, $validateSql, $validateT15Sql)) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Required file missing: $path" }
    }
    if ($Mode -eq "ResetDemo") {
        if (-not $ConfirmReset) { throw "ResetDemo requires -ConfirmReset." }
        if (-not $SkipGenerate) { Invoke-Python @("scripts/data/generate-demo-data.py") }
        & (Join-Path $root "scripts\windows\reset-dev-db.ps1") -ConfirmReset -ConfirmationText "RESET south_stand"
        if ($LASTEXITCODE -ne 0) { throw "reset-dev-db.ps1 failed" }
        Write-Host "Importing deterministic full demo dataset..."
        Invoke-MysqlFile $seedDemo
    } else {
        Write-Host "Applying non-destructive T15 migration and incremental seed..."
        Invoke-MysqlFile $migrationT15
        Invoke-MysqlFile $incrementalT15
    }
    Write-Host "Validating demo dataset..."
    $validationPath = if ($Mode -eq "ResetDemo") { $validateSql } else { $validateT15Sql }
    $validation = Invoke-MysqlFile $validationPath -Capture
    $validation | ForEach-Object { Write-Host $_ }
    $bad = $validation | Where-Object { $_ -match '^[^\t]+\t([1-9][0-9]*)$' -and $_ -notmatch '^demo_' }
    if ($bad) { throw "Demo validation found anomalies: $($bad -join '; ')" }
    Write-Host "Demo data mode $Mode completed and validated"
} finally {
    Pop-Location
}
