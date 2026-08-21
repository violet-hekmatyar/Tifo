param(
    [int]$Port = 8080,
    [string]$Username = "admin",
    [string]$Password = "password"
)

$ErrorActionPreference = "Stop"
$base = "http://localhost:$Port"

function Invoke-Json([string]$method, [string]$path, $body = $null, [string]$token = $null) {
    $params = @{ Uri = "$base$path"; Method = $method; TimeoutSec = 15; Headers = @{} }
    if ($token) { $params.Headers.Authorization = "Bearer $token" }
    if ($null -ne $body) {
        $params.ContentType = "application/json"
        $params.Body = $body | ConvertTo-Json
    }
    Invoke-RestMethod @params
}

function Assert-Ok($response, [string]$label) {
    if ($response.code -ne 0) { throw "$label code=$($response.code)" }
    Write-Host "[OK] $label"
    return $response.data
}

function Get-Sum($rows, [string]$property) {
    return [int](($rows | Measure-Object -Property $property -Sum).Sum)
}

Write-Host "T17 match lineup stats rating smoke on port $Port"
$complete = $null
for ($page = 1; $page -le 20 -and -not $complete; $page++) {
    $matches = Assert-Ok (Invoke-Json GET "/api/app/football/matches?status=FINISHED&pageNum=$page&pageSize=50") "finished matches page $page"
    foreach ($matchCandidate in $matches.records) {
        $candidateLineups = Assert-Ok (Invoke-Json GET "/api/app/football/matches/$($matchCandidate.matchId)/lineups") "lineup candidate $($matchCandidate.matchId)"
        if (@($candidateLineups.home.starters).Count -eq 11 -and
            @($candidateLineups.away.starters).Count -eq 11 -and
            @($candidateLineups.home.bench).Count -ge 5 -and
            @($candidateLineups.away.bench).Count -ge 5) {
            $complete = @{ match = $matchCandidate; lineups = $candidateLineups }
            break
        }
    }
    if ($page -ge $matches.pages) { break }
}

if (-not $complete) { throw "no complete T17 match" }
$match = $complete.match
$lineups = $complete.lineups
$matchId = $match.matchId
foreach ($side in @($lineups.home, $lineups.away)) {
    if (@($side.starters).Count -ne 11) { throw "starter count mismatch" }
    if (@($side.bench).Count -lt 5) { throw "bench count mismatch" }
    if (@($side.starters | Where-Object { $_.position -eq 'GOALKEEPER' }).Count -ne 1) { throw "starter goalkeeper mismatch" }
    if (@($side.bench | Where-Object { $_.position -eq 'GOALKEEPER' }).Count -lt 1) { throw "bench goalkeeper missing" }
    if (@($side.starters | Where-Object { $_.captain }).Count -ne 1) { throw "captain mismatch" }
}
Write-Host "[OK] complete match $matchId has 11 starters and at least 5 bench per team"

$teamStats = Assert-Ok (Invoke-Json GET "/api/app/football/matches/$matchId/stats") "team stats"
$playerPage = Assert-Ok (Invoke-Json GET "/api/app/football/matches/$matchId/player-stats?pageSize=100") "player stats"
$players = @($playerPage.records)
if ($players.Count -ne 22) { throw "appeared player stat count expected=22 actual=$($players.Count)" }
$homePlayers = @($players | Where-Object { $_.teamId -eq $match.homeTeam.teamId })
$awayPlayers = @($players | Where-Object { $_.teamId -eq $match.awayTeam.teamId })
if ((Get-Sum $homePlayers goals) -ne [int]$match.homeTeam.score -or
    (Get-Sum $awayPlayers goals) -ne [int]$match.awayTeam.score) {
    throw "score/player goal mismatch"
}
foreach ($pair in @(@($homePlayers, 'homeValue'), @($awayPlayers, 'awayValue'))) {
    $rows = $pair[0]
    $sideProperty = $pair[1]
    foreach ($item in @(@('SHOTS', 'shots'), @('SHOTS_ON_TARGET', 'shotsOnTarget'), @('PASSES', 'passes'), @('YELLOW_CARDS', 'yellowCards'), @('RED_CARDS', 'redCards'), @('SAVES', 'saves'))) {
        $stat = $teamStats | Where-Object { $_.statType -eq $item[0] }
        if ((Get-Sum $rows $item[1]) -ne [int]$stat.$sideProperty) { throw "$($item[0]) player/team mismatch" }
    }
}
Write-Host "[OK] score, player stats and team stats agree"

$detail = Assert-Ok (Invoke-Json GET "/api/app/football/matches/$matchId") "enhanced match detail"
if (-not $detail.lineupsAvailable -or
    -not $detail.teamStatsAvailable -or
    -not $detail.playerStatsAvailable -or
    -not $detail.ratingsAvailable) {
    throw "detail availability mismatch"
}

$login = Assert-Ok (Invoke-Json POST "/api/auth/login" @{ username = $Username; password = $Password }) "login"
$token = $login.accessToken
$player = $players | Select-Object -First 1
$created = Assert-Ok (Invoke-Json POST "/api/app/football/matches/$matchId/players/$($player.playerId)/ratings" @{ rating = 8.0 } $token) "create rating"
$ratingCount = $created.ratingCount
$updated = Assert-Ok (Invoke-Json POST "/api/app/football/matches/$matchId/players/$($player.playerId)/ratings" @{ rating = 8.5 } $token) "update rating"
if ($updated.ratingCount -ne $ratingCount) { throw "rating update increased count" }
$summary = Assert-Ok (Invoke-Json GET "/api/app/football/matches/$matchId/ratings" $null $token) "rating summary"
$mine = $summary | Where-Object { $_.playerId -eq $player.playerId }
if ([decimal]$mine.currentUserRating -ne 8.5) { throw "current user rating mismatch" }
Assert-Ok (Invoke-Json DELETE "/api/app/football/matches/$matchId/players/$($player.playerId)/ratings" $null $token) "cancel rating" | Out-Null

$bench = $lineups.home.bench | Select-Object -First 1
$bad = Invoke-Json POST "/api/app/football/matches/$matchId/players/$($bench.playerId)/ratings" @{ rating = 8.0 } $token
if ($bad.code -ne 40901) { throw "bench rating expected 40901 actual=$($bad.code)" }
$scheduled = Assert-Ok (Invoke-Json GET "/api/app/football/matches?status=SCHEDULED&pageSize=1") "scheduled match"
$bad = Invoke-Json POST "/api/app/football/matches/$($scheduled.records[0].matchId)/players/$($player.playerId)/ratings" @{ rating = 8.0 } $token
if ($bad.code -ne 40901) { throw "unfinished rating expected 40901 actual=$($bad.code)" }
Write-Host "T17 match lineup stats rating smoke passed"
