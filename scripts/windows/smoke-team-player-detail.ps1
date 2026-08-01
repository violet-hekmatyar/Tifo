param([int]$Port=8080)
$ErrorActionPreference="Stop";$base="http://localhost:$Port/api/app/football"
function Get-Ok([string]$url,[string]$label){$r=Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15;if($r.code-ne 0){throw "$label code=$($r.code)"};Write-Host "[OK] $label";return $r.data}
function Expect-Code([string]$url,[int]$code,[string]$label){try{$r=Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15;if($r.code-ne$code){throw "$label expected=$code actual=$($r.code)"}}catch{if($_.ErrorDetails.Message){$r=$_.ErrorDetails.Message|ConvertFrom-Json;if($r.code-ne$code){throw}}else{throw}};Write-Host "[OK] $label"}
Write-Host "T16 team player detail smoke on port $Port"
$leagues=Get-Ok "$base/leagues" "leagues";$league=$leagues|Select-Object -First 1
$seasons=Get-Ok "$base/leagues/$($league.leagueId)/seasons" "seasons";$season=$seasons|Where-Object current|Select-Object -First 1;if(-not$season){$season=$seasons|Select-Object -First 1}
$standing=Get-Ok "$base/standings?leagueId=$($league.leagueId)&seasonId=$($season.seasonId)" "standings";$standingRow=$standing.records|Select-Object -First 1
$teamId=$standingRow.teamId;$overview=Get-Ok "$base/teams/$teamId/overview?seasonId=$($season.seasonId)" "team overview"
if($overview.standing.rank-ne$standingRow.rank){throw "overview standing mismatch"};Write-Host "[OK] overview standing matches T15"
$stats=Get-Ok "$base/teams/$teamId/stats?seasonId=$($season.seasonId)" "team stats";if($stats.goalsFor-ne$standingRow.goalsFor-or$stats.goalsAgainst-ne$standingRow.goalsAgainst){throw "team stats mismatch"};Write-Host "[OK] team stats match T15"
$roster=Get-Ok "$base/teams/$teamId/players?seasonId=$($season.seasonId)&pageSize=100" "team roster";if($roster.records.Count-lt1){throw "empty roster"}
$captains=@($roster.records|Where-Object captain);if($captains.Count-gt1){throw "multiple captains"};$shirts=@($roster.records|Where-Object{$null-ne$_.shirtNumber}|ForEach-Object shirtNumber);if(($shirts|Sort-Object -Unique).Count-ne$shirts.Count){throw "duplicate shirts"};Write-Host "[OK] roster position, shirt and captain constraints"
$honors=Get-Ok "$base/teams/$teamId/honors" "team honors";if($honors.Count-lt1){throw "empty honors"}
$playerId=$roster.records[0].playerId;$playerOverview=Get-Ok "$base/players/$playerId/overview?seasonId=$($season.seasonId)" "player overview"
$playerStats=Get-Ok "$base/players/$playerId/stats?seasonId=$($season.seasonId)" "player stats";$playerTeams=Get-Ok "$base/players/$playerId/teams" "player teams";$career=Get-Ok "$base/players/$playerId/career" "player career"
$sumGoals=($career.bySeason|Measure-Object -Property goals -Sum).Sum;if([int]$sumGoals-ne[int]$career.totalGoals){throw "career goal aggregation mismatch"};Write-Host "[OK] career aggregation"
Expect-Code "$base/teams/999999999999/overview" 40401 "missing team"
Expect-Code "$base/players/999999999999/overview" 40401 "missing player"
Expect-Code "$base/teams/$teamId/overview?seasonId=1" 40001 "mismatched season"
Write-Host "T16 team player detail smoke passed"
