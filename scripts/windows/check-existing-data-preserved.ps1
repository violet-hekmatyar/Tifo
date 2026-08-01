param(
    [Parameter(Mandatory = $true)][ValidateSet("Before", "After")][string]$Phase,
    [string]$SnapshotPath = (Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t15-preservation.json")
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$sqlPath = Join-Path $root "scripts\sql\check-existing-data-preserved.sql"
$mysqlHostName = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
$mysqlPortNumber = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$mysqlUserName = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
$mysqlPasswordValue = if ($null -ne $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "" }
$mysqlContainerName = if ($env:MYSQL_CONTAINER) { $env:MYSQL_CONTAINER } else { "apihub-mysql" }
. (Join-Path $PSScriptRoot "mysql-file-utils.ps1")

function Read-Fingerprints {
    $lines = Invoke-MySqlSqlFile -Path $sqlPath -HostName $mysqlHostName -Port $mysqlPortNumber `
        -Username $mysqlUserName -Password $mysqlPasswordValue -ContainerName $mysqlContainerName -Capture
    $tables = [ordered]@{}
    foreach ($line in $lines) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line -match '^table_name\s') { continue }
        $parts = $line -split "`t", 3
        if ($parts.Count -ne 3) { throw "Unexpected preservation row: $line" }
        if (-not $tables.Contains($parts[0])) { $tables[$parts[0]] = [ordered]@{} }
        $tables[$parts[0]][$parts[1]] = $parts[2]
    }
    return $tables
}

$current = Read-Fingerprints
if ($Phase -eq "Before") {
    $snapshot = [ordered]@{ capturedAt = (Get-Date).ToString("o"); tables = $current }
    $snapshot | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $SnapshotPath -Encoding UTF8
    $total = ($current.Values | ForEach-Object { $_.Count } | Measure-Object -Sum).Sum
    Write-Host "[OK] preservation snapshot: tables=$($current.Count), rows=$total, path=$SnapshotPath"
    exit 0
}

if (-not (Test-Path -LiteralPath $SnapshotPath)) { throw "Preservation snapshot missing: $SnapshotPath" }
$before = Get-Content -Raw -Encoding UTF8 -LiteralPath $SnapshotPath | ConvertFrom-Json
$errors = [System.Collections.Generic.List[string]]::new()
foreach ($tableProperty in $before.tables.PSObject.Properties) {
    $tableName = $tableProperty.Name
    if (-not $current.Contains($tableName)) {
        $errors.Add("table missing or empty: $tableName")
        continue
    }
    $oldRows = $tableProperty.Value
    foreach ($rowProperty in $oldRows.PSObject.Properties) {
        $rowId = $rowProperty.Name
        if (-not $current[$tableName].Contains($rowId)) { $errors.Add("row missing: $tableName/$rowId") }
        elseif ($current[$tableName][$rowId] -ne $rowProperty.Value) { $errors.Add("row changed: $tableName/$rowId") }
        if ($errors.Count -ge 20) { break }
    }
    $oldCount = @($oldRows.PSObject.Properties).Count
    if ($current[$tableName].Count -lt $oldCount) { $errors.Add("row count decreased: $tableName ($oldCount -> $($current[$tableName].Count))") }
    if ($errors.Count -ge 20) { break }
}
if ($errors.Count -gt 0) { throw "Existing data preservation failed: $($errors -join '; ')" }
$total = ($before.tables.PSObject.Properties | ForEach-Object { @($_.Value.PSObject.Properties).Count } | Measure-Object -Sum).Sum
Write-Host "[OK] all $total pre-existing rows still exist with unchanged critical fields"
