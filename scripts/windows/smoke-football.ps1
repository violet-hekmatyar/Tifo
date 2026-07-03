param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

function Invoke-Json($Method, $Path, $Body = $null, $Token = $null) {
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $params = @{
        Uri = "http://localhost:$Port$Path"
        Method = $Method
        TimeoutSec = 10
        Headers = $headers
    }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json"
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            return ($_.ErrorDetails.Message | ConvertFrom-Json)
        }
        throw
    }
}

function Assert-Code($Result, [int]$ExpectedCode, [string]$Label) {
    if ($Result.code -ne $ExpectedCode) {
        $json = $Result | ConvertTo-Json -Depth 10
        throw "$Label expected code=$ExpectedCode, actual=$($Result.code), body=$json"
    }
    Write-Host "[OK] $Label code=$ExpectedCode"
}

Write-Host "T06 football smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$leagues = Invoke-Json GET "/api/app/football/leagues"
Assert-Code $leagues 0 "league list"
if ($leagues.data.Count -lt 3) {
    throw "league list expected at least 3 records"
}

$important = Invoke-Json GET "/api/app/football/matches/important?pageNum=1&pageSize=10"
Assert-Code $important 0 "important matches"
if ($important.data.records.Count -lt 1) {
    throw "important matches expected records"
}

$teamMatches = Invoke-Json GET "/api/app/football/matches?teamId=30001&pageNum=1&pageSize=10"
Assert-Code $teamMatches 0 "matches by team"
if ($teamMatches.data.records.Count -lt 1) {
    throw "matches by team expected records"
}

$leagueMatches = Invoke-Json GET "/api/app/football/matches?leagueId=10003&pageNum=1&pageSize=10"
Assert-Code $leagueMatches 0 "matches by league"
if ($leagueMatches.data.records.Count -lt 1) {
    throw "matches by league expected records"
}

$team = Invoke-Json GET "/api/app/football/teams/30001"
Assert-Code $team 0 "team detail"
if ([string]::IsNullOrWhiteSpace($team.data.teamName)) {
    throw "team detail expected teamName"
}

$player = Invoke-Json GET "/api/app/football/players/40001"
Assert-Code $player 0 "player detail"
if ([string]::IsNullOrWhiteSpace($player.data.playerName)) {
    throw "player detail expected playerName"
}

$match = Invoke-Json GET "/api/app/football/matches/50001"
Assert-Code $match 0 "match detail"
if ($null -eq $match.data.eventList -or $match.data.eventList.Count -lt 1) {
    throw "match detail expected eventList"
}
if ($null -eq $match.data.report -or $match.data.report.contentId -ne 20004) {
    throw "match detail expected report contentId 20004"
}

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "football_user_$suffix"
$phone = "136$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register football user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login football user"
$token = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "login did not return accessToken"
}
Write-Host "[OK] football token acquired"

$follow = Invoke-Json POST "/api/app/follows/toggle" @{
    followType = "TEAM"
    targetId = 30001
} $token
Assert-Code $follow 0 "follow team 30001"
if ($follow.data.followed -ne $true) {
    $followAgain = Invoke-Json POST "/api/app/follows/toggle" @{
        followType = "TEAM"
        targetId = 30001
    } $token
    Assert-Code $followAgain 0 "follow team 30001 back on"
    if ($followAgain.data.followed -ne $true) {
        throw "follow team expected followed=true"
    }
}

$following = Invoke-Json GET "/api/app/football/matches/following-teams?pageNum=1&pageSize=10" $null $token
Assert-Code $following 0 "following team matches"
if ($following.data.records.Count -lt 1) {
    throw "following team matches expected records"
}

$followingByTeam = Invoke-Json GET "/api/app/football/matches/following-teams?teamId=30001&pageNum=1&pageSize=10" $null $token
Assert-Code $followingByTeam 0 "following team matches by team"
if ($followingByTeam.data.records.Count -lt 1) {
    throw "following team matches by team expected records"
}

$followingNoToken = Invoke-Json GET "/api/app/football/matches/following-teams?pageNum=1&pageSize=10"
Assert-Code $followingNoToken 40101 "following team matches without token"

Write-Host "T06 football smoke passed"
