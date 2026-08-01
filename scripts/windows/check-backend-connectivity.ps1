param([int]$Port = 8080)

$ErrorActionPreference = "Stop"

function Test-HttpHealth([string]$hostName) {
    $url = "http://${hostName}:$Port/api/public/health"
    $result = & curl.exe --noproxy "*" -sS --connect-timeout 3 -o NUL `
        -w "code=%{http_code} connect=%{time_connect}s total=%{time_total}s" $url
    if ($LASTEXITCODE -ne 0 -or $result -notmatch "code=200") {
        throw "Health check failed for ${hostName}: $result"
    }
    Write-Host "[OK] $hostName $result"
}

$listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
if ($listeners.Count -eq 0) { throw "Port $Port is not listening" }

Write-Host "Backend listeners"
$listeners | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize
foreach ($processId in ($listeners.OwningProcess | Sort-Object -Unique)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId=$processId"
    $safeCommand = if ($process.CommandLine -match "(?i)-jar\s+([^\s]+)") { "java -jar $($Matches[1])" } else { $process.Name }
    Write-Host "PID=$processId command=$safeCommand started=$($process.CreationDate)"
}

Test-HttpHealth "127.0.0.1"
Test-HttpHealth "localhost"

$lanAddresses = @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254*" -and $_.PrefixOrigin -ne "WellKnown" } |
    Select-Object -ExpandProperty IPAddress -Unique)
foreach ($address in $lanAddresses) {
    try { Test-HttpHealth $address } catch { Write-Warning $_.Exception.Message }
}

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($adb) {
    $devices = & $adb.Source devices
    Write-Host "adb devices:"
    $devices | ForEach-Object { Write-Host $_ }
    Write-Host "adb reverse mappings:"
    (& $adb.Source reverse --list) | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "[INFO] adb is not available in PATH; adb reverse was not inspected."
}

Write-Host "Android emulator recommendation: http://10.0.2.2:$Port"
Write-Host "Do not use localhost in the emulator unless adb reverse tcp:$Port tcp:$Port is configured."
Write-Host "Backend connectivity check passed"
