param([int]$Port = 8080)

$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:$Port"

function Invoke-Json($method, $path, $token = $null) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    try { return Invoke-RestMethod -Uri "$base$path" -Method $method -Headers $headers -TimeoutSec 15 }
    catch { if ($_.ErrorDetails.Message) { return ($_.ErrorDetails.Message | ConvertFrom-Json) }; throw }
}
function Require($condition, $message) { if (-not $condition) { throw $message } }
function Require-Ok($result, $label) {
    Require ($result.code -eq 0) "$label failed: code=$($result.code), message=$($result.message)"
    Write-Host "[OK] $label"
    return $result.data
}
function Require-Records($result, $label) {
    $data = Require-Ok $result $label
    Require ($null -ne $data.records -and @($data.records).Count -gt 0) "$label returned no records"
    Require ($null -ne $data.total -and $null -ne $data.pageNum -and $null -ne $data.pageSize -and $null -ne $data.pages) "$label pagination contract missing"
    return $data
}

$loginBody = @{ username = "demo_user_01"; password = "Demo123456!" } | ConvertTo-Json
$login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 15
$loginData = Require-Ok $login "login"
$token = $loginData.accessToken
Require (-not [string]::IsNullOrWhiteSpace($token)) "login token missing"

$feed = Require-Records (Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=10" $token) "home feed"
Require ($feed.records[0].cardType) "feed cardType missing"

foreach ($type in @("TEAM", "PLAYER", "MATCH", "CONTENT")) {
    $keyword = if ($type -eq "TEAM") { "Demo%20Team" } elseif ($type -eq "PLAYER") { "Demo%20Player" } elseif ($type -eq "MATCH") { "Demo%20Team" } else { "Barcelona" }
    $search = Require-Records (Invoke-Json GET "/api/app/search/entities?keyword=$keyword&entityType=$type&pageNum=1&pageSize=10") "$type search"
    Require (@($search.records | Where-Object { $_.entityType -eq $type }).Count -gt 0) "$type search returned wrong entityType"
}

$teamId = [Int64]13000000000000001
$playerId = [Int64]14000000000000001
$matchId = [Int64]15000000000000017
$contentId = [Int64]16000000000000001
Require-Ok (Invoke-Json GET "/api/app/football/teams/$teamId") "team detail" | Out-Null
Require-Ok (Invoke-Json GET "/api/app/football/teams/$teamId/overview") "team overview" | Out-Null
Require-Ok (Invoke-Json GET "/api/app/football/players/$playerId") "player detail" | Out-Null
Require-Ok (Invoke-Json GET "/api/app/football/players/$playerId/overview") "player overview" | Out-Null
Require-Ok (Invoke-Json GET "/api/app/football/matches/$matchId") "match detail" | Out-Null
Require-Ok (Invoke-Json GET "/api/app/football/matches/$matchId/overview") "match overview" | Out-Null
Require-Records (Invoke-Json GET "/api/app/comments?contentId=$contentId&sort=hot&pageNum=1&pageSize=10" $token) "comments and interaction"

$notifications = Require-Ok (Invoke-Json GET "/api/app/notifications?pageNum=1&pageSize=10" $token) "notifications"
Require ($null -ne $notifications.records -and $null -ne $notifications.total) "notification pagination contract missing"
Require-Ok (Invoke-Json GET "/api/app/notifications/unread-count" $token) "notification unread count" | Out-Null

Write-Host "T22 Backend V1 core smoke passed"
