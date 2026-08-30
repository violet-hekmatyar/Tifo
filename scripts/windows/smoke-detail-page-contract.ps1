param([int]$Port = 8080)
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:$Port/api/app/football"

function Get-Ok([string]$Path, [string]$Label) {
    $response = Invoke-RestMethod -Uri "$base$Path" -Method Get -TimeoutSec 20
    if ($response.code -ne 0) { throw "$Label code=$($response.code)" }
    Write-Host "[OK] $Label"
    return $response.data
}
function Require($Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
function Has-Property($Object, [string]$Name) { return $null -ne $Object.PSObject.Properties[$Name] }

Write-Host "T20 detail page contract smoke on port $Port"
$leagues = Get-Ok "/leagues" "leagues"
$teamOverview = $null; $teamId = $null; $seasonId = $null
foreach ($league in $leagues) {
    $seasons = Get-Ok "/leagues/$($league.leagueId)/seasons" "seasons $($league.leagueId)"
    foreach ($season in $seasons) {
        $table = Get-Ok "/standings?leagueId=$($league.leagueId)&seasonId=$($season.seasonId)" "standings $($season.seasonId)"
        foreach ($row in @($table.records)) {
            $candidate = Get-Ok "/teams/$($row.teamId)/overview?seasonId=$($season.seasonId)" "team overview candidate $($row.teamId)"
            if (@($candidate.recentContents).Count -gt 0 -and @($candidate.leaderboards).Count -ge 4) {
                $teamOverview = $candidate; $teamId = $row.teamId; $seasonId = $season.seasonId; break
            }
        }
        if ($teamOverview) { break }
    }
    if ($teamOverview) { break }
}
Require ($null -ne $teamOverview) "no complete team detail contract candidate"
Require (@($teamOverview.competitionStandings).Count -gt 0) "team competition standings missing"
Require (@($teamOverview.honors).Count -gt 0) "team honors missing"
Require ((@($teamOverview.leaderboards | ForEach-Object rankType) -join ',') -match 'GOALS') "team goals leaderboard missing"
Require ((@($teamOverview.leaderboards | ForEach-Object rankType) -join ',') -match 'ASSISTS') "team assists leaderboard missing"

$teamMatches = Get-Ok "/teams/$teamId/matches?pageSize=50" "team matches"
Require (@($teamMatches.records).Count -gt 0) "team matches empty"
Require (Has-Property $teamMatches.records[0] "homeTeamLogoUrl") "team match VO incomplete"
$teamContents = Get-Ok "/teams/$teamId/contents?pageSize=20" "team contents"
Require (@($teamContents.records).Count -gt 0) "team contents empty"
Require (-not (@($teamContents.records | Where-Object { $_.contentType -notin @('POST','ARTICLE','NEWS','REPORT') }).Count)) "unsupported team content type"

$roster = Get-Ok "/teams/$teamId/players?seasonId=$seasonId&pageSize=100" "team roster"
$playerOverview = $null; $playerMatches = $null; $playerId = $null
foreach ($player in @($roster.records)) {
    $candidateMatches = Get-Ok "/players/$($player.playerId)/matches?pageSize=20" "player matches candidate $($player.playerId)"
    if (@($candidateMatches.records).Count -gt 0) {
        $playerId = $player.playerId; $playerMatches = $candidateMatches
        $playerOverview = Get-Ok "/players/$playerId/overview?seasonId=$seasonId" "player overview"
        break
    }
}
if (-not $playerOverview) {
    for ($page = 1; $page -le 20 -and -not $playerOverview; $page++) {
        $finished = Get-Ok "/matches?status=FINISHED&pageNum=$page&pageSize=50" "finished matches page $page"
        foreach ($match in @($finished.records)) {
            $stats = Get-Ok "/matches/$($match.matchId)/player-stats?pageSize=1" "player stats candidate $($match.matchId)"
            if (@($stats.records).Count -eq 0) { continue }
            $playerId = $stats.records[0].playerId
            $playerMatches = Get-Ok "/players/$playerId/matches?pageSize=20" "player matches $playerId"
            if (@($playerMatches.records).Count -eq 0) { continue }
            $playerOverview = Get-Ok "/players/$playerId/overview" "player overview"
            break
        }
        if ($page -ge $finished.pages) { break }
    }
}
Require ($null -ne $playerOverview) "no player with match records"
Require (Has-Property $playerOverview "retired") "player retired status missing"
Require (Has-Property $playerOverview "playerStatus") "player status missing"
Require (Has-Property $playerOverview "club") "player club contract missing"
Require (Has-Property $playerOverview "nationalTeam") "player national team compatibility field missing"
Require (@($playerOverview.seasonStats).Count -gt 0) "player stats missing"
Require ($null -ne $playerOverview.career) "player career missing"
$playerRecord = $playerMatches.records[0]
foreach ($field in @('matchId','matchTime','homeTeamId','awayTeamId','playerTeamId','starter','minutes','goals','assists','officialRating')) {
    Require (Has-Property $playerRecord $field) "player match field $field missing"
}
$playerContents = Get-Ok "/players/$playerId/contents?pageSize=20" "player contents"
Require ($null -ne $playerContents.records) "player contents must return an array"

$matchOverview = Get-Ok "/matches/$($playerRecord.matchId)/overview" "match overview"
Require ($matchOverview.match.matchId -eq $playerRecord.matchId) "match overview id mismatch"
Require ($null -ne $matchOverview.lineups) "match lineup contract missing"
Require ($null -ne $matchOverview.teamStats) "match stats contract missing"
Require ($null -ne $matchOverview.playerStats.records) "match player stats contract missing"
Require ($null -ne $matchOverview.ratings) "match rating contract missing"
Require ($null -ne $matchOverview.match.eventList) "match events contract missing"
Require ($null -ne $matchOverview.ranking) "match ranking contract missing"
Require ($null -ne $matchOverview.ranking.home -and $null -ne $matchOverview.ranking.away) "both team ranking snapshots required"
Require ($matchOverview.ranking.snapshotType -in @('CURRENT_STANDING','UNAVAILABLE')) "invalid ranking snapshot type"

Write-Host "T20 detail page contract smoke passed"
