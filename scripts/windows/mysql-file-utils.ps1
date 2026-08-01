function Invoke-MySqlSqlFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$HostName,
        [Parameter(Mandatory = $true)][string]$Port,
        [Parameter(Mandatory = $true)][string]$Username,
        [AllowEmptyString()][string]$Password = "",
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [switch]$Capture
    )

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysql) {
        $oldPassword = $env:MYSQL_PWD
        $outputPath = if ($Capture) { [System.IO.Path]::GetTempFileName() } else { $null }
        try {
            if ($Password) { $env:MYSQL_PWD = $Password } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
            $arguments = @(
                "--default-character-set=utf8mb4",
                "--host=$HostName",
                "--port=$Port",
                "--user=$Username"
            )
            $start = @{
                FilePath = $mysql.Source
                ArgumentList = $arguments
                RedirectStandardInput = $resolvedPath
                NoNewWindow = $true
                PassThru = $true
                Wait = $true
            }
            if ($Capture) { $start.RedirectStandardOutput = $outputPath }
            $process = Start-Process @start
            if ($process.ExitCode -ne 0) { throw "mysql command failed for $resolvedPath" }
            if ($Capture) { return Get-Content -Encoding UTF8 -LiteralPath $outputPath }
            return
        } finally {
            if ($null -ne $oldPassword) { $env:MYSQL_PWD = $oldPassword } else { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
            if ($outputPath) { Remove-Item -LiteralPath $outputPath -Force -ErrorAction SilentlyContinue }
        }
    }

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    $containerId = if ($docker) { & $docker.Source ps --filter "name=^/$ContainerName$" --format "{{.ID}}" } else { $null }
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "mysql command not found and MySQL container is unavailable"
    }

    $containerPath = "/tmp/south-stand-sql-$([Guid]::NewGuid().ToString('N')).sql"
    & $docker.Source cp -- $resolvedPath "${ContainerName}:$containerPath" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "docker cp failed for $resolvedPath" }
    try {
        $mysqlCommand = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 --batch --raw -uroot < "' + $containerPath + '"'
        if ($Capture) {
            $output = & $docker.Source exec $ContainerName sh -c $mysqlCommand
            if ($LASTEXITCODE -ne 0) { throw "docker mysql command failed for $resolvedPath" }
            return $output
        }
        & $docker.Source exec $ContainerName sh -c $mysqlCommand
        if ($LASTEXITCODE -ne 0) { throw "docker mysql command failed for $resolvedPath" }
    } finally {
        & $docker.Source exec $ContainerName rm -f -- $containerPath | Out-Null
    }
}
