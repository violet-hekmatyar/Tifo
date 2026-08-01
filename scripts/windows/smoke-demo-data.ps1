param([int]$Port = 8080)

$ErrorActionPreference = "Stop"
$base = "http://localhost:$Port"
$userBase = [Int64]11000000000000001
$teamBase = [Int64]13000000000000001
$playerBase = [Int64]14000000000000001
$matchBase = [Int64]15000000000000001
$contentBase = [Int64]16000000000000001

function Invoke-Json($method, $path, $token = $null) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    try { return Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $headers -TimeoutSec 15 }
    catch { if ($_.ErrorDetails.Message) { return ($_.ErrorDetails.Message | ConvertFrom-Json) }; throw }
}
function Assert-Ok($result, $label) {
    if ($result.code -ne 0) { throw "$label failed: code=$($result.code), message=$($result.message)" }
    Write-Host "[OK] $label"
}
function Assert-Records($result, $label) {
    Assert-Ok $result $label
    if (-not $result.data.records -or $result.data.records.Count -eq 0) { throw "$label returned no records" }
}

$loginBody = @{ username = "demo_user_01"; password = "Demo123456!" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 15
Assert-Ok $login "demo login"
$token = $login.data.accessToken

Assert-Records (Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=10" $token) "feed page 1"
Assert-Records (Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=2&pageSize=10" $token) "feed page 2"
Assert-Records (Invoke-Json GET "/api/app/search/entities?keyword=Demo%20Team&entityType=TEAM&pageNum=1&pageSize=10") "team search"
Assert-Records (Invoke-Json GET "/api/app/search/entities?keyword=Demo%20Player&entityType=PLAYER&pageNum=2&pageSize=10") "player page 2"
Assert-Records (Invoke-Json GET "/api/app/football/matches?pageNum=1&pageSize=10") "match page 1"
Assert-Records (Invoke-Json GET "/api/app/football/matches?pageNum=2&pageSize=10") "match page 2"
Assert-Ok (Invoke-Json GET "/api/app/football/teams/$teamBase") "team detail"
Assert-Ok (Invoke-Json GET "/api/app/football/players/$playerBase") "player detail"

$finishedId = $matchBase + 16
$match = Invoke-Json GET "/api/app/football/matches/$finishedId"
Assert-Ok $match "finished match detail"
$homeGoals = @($match.data.eventList | Where-Object { $_.eventType -eq "GOAL" -and $_.teamId -eq $match.data.homeTeam.teamId }).Count
$awayGoals = @($match.data.eventList | Where-Object { $_.eventType -eq "GOAL" -and $_.teamId -eq $match.data.awayTeam.teamId }).Count
if ($homeGoals -ne $match.data.homeTeam.score -or $awayGoals -ne $match.data.awayTeam.score) { throw "match score does not equal GOAL events" }
Write-Host "[OK] score and GOAL events agree"

$articleId = $contentBase + 32
$article = Invoke-Json GET "/api/app/contents/$articleId" $token
Assert-Ok $article "article detail"
$orders = @($article.data.blocks | ForEach-Object { [int]$_.sortOrder })
if ($orders.Count -lt 3 -or ($orders -join ',') -ne (($orders | Sort-Object) -join ',')) { throw "article blocks are not ordered" }
Write-Host "[OK] article blocks ordered"

Assert-Records (Invoke-Json GET "/api/app/users/me/likes?pageNum=2&pageSize=10&targetType=CONTENT" $token) "my likes page 2"
Assert-Records (Invoke-Json GET "/api/app/users/me/favorites?pageNum=2&pageSize=10" $token) "my favorites page 2"
Assert-Records (Invoke-Json GET "/api/app/comments?contentId=$contentBase&sort=hot&pageNum=1&pageSize=10" $token) "hot comments"
Assert-Records (Invoke-Json GET "/api/app/comments?contentId=$contentBase&sort=latest&pageNum=2&pageSize=10" $token) "latest comments page 2"
Assert-Ok (Invoke-Json GET "/api/app/users/$userBase/profile" $token) "public user profile"
Assert-Records (Invoke-Json GET "/api/app/users/$userBase/followings?pageNum=1&pageSize=10" $token) "user followings"
Assert-Records (Invoke-Json GET "/api/app/users/$userBase/followers?pageNum=1&pageSize=10" $token) "user followers"

for ($i = 1; $i -le 10; $i++) {
    $asset = Invoke-WebRequest -Uri "$base/demo/contents/cover-$($i.ToString('00')).svg" -UseBasicParsing -TimeoutSec 10
    if ($asset.StatusCode -ne 200 -or $asset.RawContentLength -lt 100) { throw "demo asset $i unavailable" }
}
Write-Host "[OK] 10 demo image URLs accessible"
Write-Host "T14 demo data smoke passed"
