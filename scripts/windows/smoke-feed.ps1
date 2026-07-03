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

function Assert-Records($Result, [string]$Label) {
    if ($null -eq $Result.data.records -or $Result.data.records.Count -lt 1) {
        $json = $Result | ConvertTo-Json -Depth 10
        throw "$Label expected non-empty records, body=$json"
    }
    Write-Host "[OK] $Label records=$($Result.data.records.Count)"
}

Write-Host "T07 feed smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$defaultFeed = Invoke-Json GET "/api/app/feed"
Assert-Code $defaultFeed 0 "default feed"
Assert-Records $defaultFeed "default feed"

$recommend = Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=10"
Assert-Code $recommend 0 "recommend feed"
Assert-Records $recommend "recommend feed"

$news = Invoke-Json GET "/api/app/feed?tab=news&pageNum=1&pageSize=10"
Assert-Code $news 0 "news feed"
Assert-Records $news "news feed"
if (-not ($news.data.records | Where-Object { $_.cardType -eq "CONTENT" })) {
    throw "news feed expected at least one CONTENT card"
}
if ($news.data.records | Where-Object { $_.cardType -eq "MATCH" -or $_.contentType -eq "POST" }) {
    throw "news feed must not contain MATCH cards or POST content"
}

$match = Invoke-Json GET "/api/app/feed?tab=match&pageNum=1&pageSize=10"
Assert-Code $match 0 "match feed"
Assert-Records $match "match feed"
if (-not ($match.data.records | Where-Object { $_.cardType -eq "MATCH" })) {
    throw "match feed expected at least one MATCH card"
}

$mixed = Invoke-Json GET "/api/app/feed?tab=mixed&pageNum=1&pageSize=10"
Assert-Code $mixed 0 "mixed feed"
Assert-Records $mixed "mixed feed"

$hotLeagues = Invoke-Json GET "/api/app/feed/hot-leagues?limit=10"
Assert-Code $hotLeagues 0 "hot leagues"
if ($null -eq $hotLeagues.data -or $hotLeagues.data.Count -lt 3) {
    throw "hot leagues expected at least 3 records"
}
Write-Host "[OK] hot leagues records=$($hotLeagues.data.Count)"

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "feed_user_$suffix"
$phone = "135$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register feed user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login feed user"
$token = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "login did not return accessToken"
}
Write-Host "[OK] feed token acquired"

$preferences = Invoke-Json POST "/api/app/onboarding/preferences" @{
    mainTeamId = 30001
    followTeamIds = @(30001)
    followPlayerIds = @(40001)
} $token
Assert-Code $preferences 0 "save feed preferences"

$recommendWithToken = Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=10" $null $token
Assert-Code $recommendWithToken 0 "recommend feed with token"
Assert-Records $recommendWithToken "recommend feed with token"

$following = Invoke-Json GET "/api/app/feed?tab=following&pageNum=1&pageSize=10" $null $token
Assert-Code $following 0 "following feed with token"
Assert-Records $following "following feed with token"

$teamFeed = Invoke-Json GET "/api/app/feed?teamId=30001&pageNum=1&pageSize=10" $null $token
Assert-Code $teamFeed 0 "feed by team"

$leagueFeed = Invoke-Json GET "/api/app/feed?leagueId=10003&pageNum=1&pageSize=10" $null $token
Assert-Code $leagueFeed 0 "feed by league"

$pageSizeCap = Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=500"
Assert-Code $pageSizeCap 0 "feed pageSize cap"
if ($pageSizeCap.data.pageSize -ne 100) {
    throw "feed pageSize expected capped at 100, actual=$($pageSizeCap.data.pageSize)"
}

Write-Host "T07 feed smoke passed"
