param([int]$Port = 8080, [int]$WarmRuns = 5)

$ErrorActionPreference = "Stop"
$culture = [System.Globalization.CultureInfo]::InvariantCulture
$baseUrl = "http://127.0.0.1:$Port"
$loginFile = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-benchmark-login.json"
[System.IO.File]::WriteAllText($loginFile, '{"username":"demo_user_01","password":"Demo123456!"}', [System.Text.UTF8Encoding]::new($false))

$endpoints = @(
    [pscustomobject]@{ Name="health"; Method="GET"; Path="/api/public/health"; Body=$null },
    [pscustomobject]@{ Name="login"; Method="POST"; Path="/api/auth/login"; Body=$loginFile },
    [pscustomobject]@{ Name="feed"; Method="GET"; Path="/api/app/feed?tab=recommend&pageNum=1&pageSize=20"; Body=$null },
    [pscustomobject]@{ Name="matches"; Method="GET"; Path="/api/app/football/matches?pageNum=1&pageSize=20"; Body=$null },
    [pscustomobject]@{ Name="team-search"; Method="GET"; Path="/api/app/search/entities?keyword=Demo%20Team&entityType=TEAM&pageNum=1&pageSize=20"; Body=$null },
    [pscustomobject]@{ Name="player-search"; Method="GET"; Path="/api/app/search/entities?keyword=Demo%20Player&entityType=PLAYER&pageNum=1&pageSize=20"; Body=$null },
    [pscustomobject]@{ Name="content-detail"; Method="GET"; Path="/api/app/contents/16000000000000001"; Body=$null },
    [pscustomobject]@{ Name="comments"; Method="GET"; Path="/api/app/comments?contentId=16000000000000001&sort=hot&pageNum=1&pageSize=20"; Body=$null },
    [pscustomobject]@{ Name="user-profile"; Method="GET"; Path="/api/app/users/11000000000000001/profile"; Body=$null }
)

function Invoke-TimedRequest($endpoint) {
    $arguments = @("--noproxy", "*", "-sS", "--connect-timeout", "5", "--max-time", "15", "-o", "NUL",
        "-w", "%{http_code}|%{time_namelookup}|%{time_connect}|%{time_starttransfer}|%{time_total}|%{size_download}",
        "-X", $endpoint.Method)
    if ($endpoint.Body) { $arguments += @("-H", "Content-Type: application/json", "--data-binary", "@$($endpoint.Body)") }
    $arguments += "$baseUrl$($endpoint.Path)"
    $raw = & curl.exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "$($endpoint.Name) curl failed" }
    $parts = $raw -split "\|"
    if ($parts.Count -ne 6 -or $parts[0] -ne "200") { throw "$($endpoint.Name) returned: $raw" }
    return [pscustomobject]@{
        Code=[int]$parts[0]; Dns=[double]::Parse($parts[1],$culture); Connect=[double]::Parse($parts[2],$culture)
        Ttfb=[double]::Parse($parts[3],$culture); Total=[double]::Parse($parts[4],$culture); Bytes=[int64]$parts[5]
    }
}

try {
    Write-Host "Backend API benchmark: 1 cold request + $WarmRuns warm requests"
    foreach ($endpoint in $endpoints) {
        $cold = Invoke-TimedRequest $endpoint
        $warm = @(for ($i=0; $i -lt $WarmRuns; $i++) { Invoke-TimedRequest $endpoint })
        $stats = $warm.Total | Measure-Object -Minimum -Maximum -Average
        Write-Host (("{0,-15} cold: code={1} dns={2:N4}s connect={3:N4}s ttfb={4:N4}s total={5:N4}s bytes={6}" -f `
            $endpoint.Name,$cold.Code,$cold.Dns,$cold.Connect,$cold.Ttfb,$cold.Total,$cold.Bytes))
        Write-Host (("{0,-15} warm: min={1:N4}s avg={2:N4}s max/p95={3:N4}s runs={4}" -f `
            "",$stats.Minimum,$stats.Average,$stats.Maximum,$WarmRuns))
    }
    Write-Host "Backend API benchmark passed"
} finally {
    Remove-Item -LiteralPath $loginFile -Force -ErrorAction SilentlyContinue
}
