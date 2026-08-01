param([int]$Port = 8080)
$ErrorActionPreference = "Stop"
$base = "http://localhost:$Port"

function Invoke-Json([string]$path) {
    try { return Invoke-RestMethod -Uri "$base$path" -Method GET -TimeoutSec 15 }
    catch { if ($_.ErrorDetails.Message) { return $_.ErrorDetails.Message | ConvertFrom-Json }; throw }
}
function Assert-Ok($result,[string]$label) {
    if ($result.code -ne 0) { throw "$label failed: code=$($result.code) message=$($result.message)" }
    Write-Host "[OK] $label"
}
function Assert-Monotonic($records,[string]$direction,[string]$label) {
    for($i=1;$i -lt $records.Count;$i++) {
        $previous=[decimal]$records[$i-1].value; $current=[decimal]$records[$i].value
        if(($direction -eq "DESC" -and $previous -lt $current) -or ($direction -eq "ASC" -and $previous -gt $current)) {
            throw "$label order is invalid at index $i"
        }
    }
    Write-Host "[OK] $label ordering"
}

Write-Host "T15 football ranks smoke on port $Port"
$leagues=Invoke-Json "/api/app/football/leagues"; Assert-Ok $leagues "leagues"
$league=$leagues.data | Where-Object { $_.leagueId -ge 12000000000000001 } | Select-Object -First 1
if(-not $league){throw "No T15 demo league"}
$seasons=Invoke-Json "/api/app/football/leagues/$($league.leagueId)/seasons"; Assert-Ok $seasons "seasons"
$season=$seasons.data | Where-Object {$_.current -eq $true} | Select-Object -First 1
$seasonWord=([string][char]0x8D5B)+([string][char]0x5B63)
$seasonName=$season.seasonName
if($seasonName -and -not $seasonName.Contains($seasonWord)) {
    $seasonName=[System.Text.Encoding]::UTF8.GetString([System.Text.Encoding]::GetEncoding(28591).GetBytes($seasonName))
}
if(-not $season -or -not $seasonName.Contains($seasonWord)){throw "Current Chinese seasonName missing"}
Write-Host "[OK] Chinese current season"
$stages=Invoke-Json "/api/app/football/leagues/$($league.leagueId)/seasons/$($season.seasonId)/stages"; Assert-Ok $stages "stages"
$stage=$stages.data | Select-Object -First 1; if(-not $stage){throw "Stage missing"}

$scope="leagueId=$($league.leagueId)&seasonId=$($season.seasonId)&stageId=$($stage.stageId)"
$standing=Invoke-Json "/api/app/football/standings?$scope"; Assert-Ok $standing "standings"
if($standing.data.records.Count -lt 8){throw "Standing requires at least 8 teams"}
for($i=0;$i -lt $standing.data.records.Count;$i++) {
    $row=$standing.data.records[$i]
    if($row.rank -ne $i+1){throw "Standing rank is not continuous"}
    if($row.played -ne $row.won+$row.drawn+$row.lost){throw "Standing played formula mismatch"}
    if($row.goalDifference -ne $row.goalsFor-$row.goalsAgainst){throw "Standing goal difference mismatch"}
    if($row.points -ne $row.won*3+$row.drawn-$row.deductionPoints){throw "Standing points mismatch"}
}
Write-Host "[OK] standing rank and formulas"

foreach($type in @("GOALS","ASSISTS","RATING","SAVES")) {
    $rank=Invoke-Json "/api/app/football/player-ranks?$scope&rankType=$type&pageNum=1&pageSize=20"
    Assert-Ok $rank "player rank $type"
    if($rank.data.records.Count -lt 1){throw "$type returned no players"}
    Assert-Monotonic $rank.data.records "DESC" "player rank $type"
}
$page2=Invoke-Json "/api/app/football/player-ranks?$scope&rankType=GOALS&pageNum=2&pageSize=20"; Assert-Ok $page2 "player rank page 2"
if($page2.data.records.Count -lt 1 -or $page2.data.records[0].rank -ne 21){throw "Player rank pagination mismatch"}

$goalsFor=Invoke-Json "/api/app/football/team-ranks?$scope&rankType=GOALS_FOR&pageNum=1&pageSize=20"; Assert-Ok $goalsFor "team GOALS_FOR"
Assert-Monotonic $goalsFor.data.records "DESC" "team GOALS_FOR"
if(($goalsFor.data.records | Where-Object {$_.sortDirection -ne "DESC"}).Count){throw "GOALS_FOR direction mismatch"}
$goalsAgainst=Invoke-Json "/api/app/football/team-ranks?$scope&rankType=GOALS_AGAINST&pageNum=1&pageSize=20"; Assert-Ok $goalsAgainst "team GOALS_AGAINST"
Assert-Monotonic $goalsAgainst.data.records "ASC" "team GOALS_AGAINST"
if(($goalsAgainst.data.records | Where-Object {$_.sortDirection -ne "ASC"}).Count){throw "GOALS_AGAINST direction mismatch"}

$invalid=Invoke-Json "/api/app/football/player-ranks?$scope&rankType=DROP_TABLE&pageNum=1&pageSize=20"
if($invalid.code -ne 40001){throw "Invalid rankType expected 40001, got $($invalid.code)"}
Write-Host "[OK] invalid rankType"
Write-Host "T15 football ranks smoke passed"
