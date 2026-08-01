param([switch]$SkipGenerate)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$seedDemo = Join-Path $root "scripts\sql\seed-demo.sql"
$validateSql = Join-Path $root "scripts\sql\validate-demo-data.sql"
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
    if (-not $SkipGenerate) { Invoke-Python @("scripts/data/generate-demo-data.py") }
    foreach ($path in @($seedDemo, $validateSql)) {
        if (-not (Test-Path -LiteralPath $path)) { throw "Required file missing: $path" }
    }
    & (Join-Path $root "scripts\windows\reset-dev-db.ps1")
    if ($LASTEXITCODE -ne 0) { throw "reset-dev-db.ps1 failed" }
    Write-Host "Importing deterministic demo dataset..."
    Invoke-MysqlFile $seedDemo
    Write-Host "Validating demo dataset..."
    $validation = Invoke-MysqlFile $validateSql -Capture
    $validation | ForEach-Object { Write-Host $_ }
    $bad = $validation | Where-Object { $_ -match '^[^\t]+\t([1-9][0-9]*)$' -and $_ -notmatch '^demo_' }
    if ($bad) { throw "Demo validation found anomalies: $($bad -join '; ')" }
    Write-Host "T14 demo data initialized and validated"
} finally {
    Pop-Location
}
