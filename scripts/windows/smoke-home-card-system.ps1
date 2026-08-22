param(
    [int]$Port = 8080,
    [ValidateSet("ANY", "RULE", "CF", "FALLBACK")][string]$ExpectedMode = "ANY"
)

$ErrorActionPreference = "Stop"

function Invoke-Json($Method, $Path, $Body = $null, $Token = $null) {
    $params = @{ Uri="http://127.0.0.1:$Port$Path"; Method=$Method; TimeoutSec=15; Headers=@{} }
    if ($Token) { $params.Headers.Authorization = "Bearer $Token" }
    if ($null -ne $Body) { $params.ContentType="application/json"; $params.Body=($Body | ConvertTo-Json -Depth 10) }
    Invoke-RestMethod @params
}

function Require($Condition, [string]$Message) { if (-not $Condition) { throw $Message } }

function Matches-Mode($Feed, [string]$Mode) {
    if ($Mode -eq "ANY") { return $true }
    if ($Mode -eq "RULE") { return $Feed.experimentBucket -eq "A" -and $Feed.algorithmVersion -eq "RULE_V2" }
    if ($Mode -eq "CF") { return $Feed.experimentBucket -eq "B" -and $Feed.algorithmVersion -eq "CF_V1" }
    return $Feed.experimentBucket -eq "B" -and $Feed.algorithmVersion -eq "RULE_V2"
}

$selected = $null
for ($attempt = 0; $attempt -lt 24; $attempt++) {
    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds() + $attempt
    $username = "t19_${ExpectedMode}_$suffix"
    $register = Invoke-Json POST "/api/auth/register" @{
        username=$username; phone="136$($suffix.ToString().Substring($suffix.ToString().Length-8))"; password="Tifo123456"
    }
    Require ($register.code -eq 0) "T19 registration failed"
    $login = Invoke-Json POST "/api/auth/login" @{username=$username;password="Tifo123456"}
    Require ($login.code -eq 0) "T19 login failed"
    $token = $login.data.accessToken
    $preferences = Invoke-Json POST "/api/app/onboarding/preferences" @{
        mainTeamId=30001; followTeamIds=@(30001); followPlayerIds=@(40001)
    } $token
    Require ($preferences.code -eq 0) "T19 preferences failed"
    $page1 = Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=50" $null $token
    if ($page1.code -eq 0 -and (Matches-Mode $page1.data $ExpectedMode)) {
        $selected = @{ token=$token; page1=$page1.data }
        break
    }
}
Require ($null -ne $selected) "could not find user for expected mode $ExpectedMode"

$page1 = $selected.page1
$page2Response = Invoke-Json GET "/api/app/feed?tab=recommend&pageNum=2&pageSize=50" $null $selected.token
Require ($page2Response.code -eq 0) "T19 page2 feed failed"
$page2 = $page2Response.data
$records = @($page1.records)
Require ($records.Count -gt 0) "T19 recommend feed is empty"

$requiredTypes = @("CONTENT", "MATCH", "DISCUSSION", "HOT_COMMENT", "RANKING", "PLAYER_RATING")
foreach ($type in $requiredTypes) {
    Require (($records | Where-Object { $_.cardType -eq $type }).Count -gt 0) "T19 missing cardType=$type"
}

$contentIds = @($records | Where-Object { $_.cardType -eq "CONTENT" } | ForEach-Object { $_.contentId })
$discussionIds = @($records | Where-Object { $_.cardType -eq "DISCUSSION" } | ForEach-Object { $_.contentId })
Require (-not ($contentIds | Where-Object { $discussionIds -contains $_ })) "CONTENT and DISCUSSION duplicate the same contentId"

$auxTypes = @("DISCUSSION", "HOT_COMMENT", "RANKING", "PLAYER_RATING")
$aux = @($records | Where-Object { $auxTypes -contains $_.cardType })
Require (($aux.Count / $records.Count) -le 0.200001) "auxiliary card ratio exceeds 20%"
$lastAux = -1
for ($i = 0; $i -lt $records.Count; $i++) {
    if ($auxTypes -contains $records[$i].cardType) {
        if ($lastAux -ge 0) { Require (($i - $lastAux - 1) -ge 3) "auxiliary card min-gap is below 3" }
        $lastAux = $i
    }
    Require ($records[$i].position -eq $i) "page1 position is not continuous at index $i"
    Require (-not [string]::IsNullOrWhiteSpace($records[$i].impressionId)) "missing impressionId at index $i"
    Require (-not [string]::IsNullOrWhiteSpace($records[$i].cardKey)) "missing cardKey at index $i"
}
Require ((@($records.impressionId | Sort-Object -Unique)).Count -eq $records.Count) "page1 impressionId is not unique"
Require ((@($records.cardKey | Sort-Object -Unique)).Count -eq $records.Count) "page1 cardKey is not unique"

$page2Keys = @($page2.records | ForEach-Object { $_.cardKey })
Require (-not ($records.cardKey | Where-Object { $page2Keys -contains $_ })) "page1/page2 contains duplicate cardKey"
for ($i = 0; $i -lt @($page2.records).Count; $i++) {
    Require ($page2.records[$i].position -eq (50 + $i)) "page2 position is not continuous at index $i"
}

if ($ExpectedMode -eq "RULE") { Require ($page1.experimentBucket -eq "A" -and $page1.algorithmVersion -eq "RULE_V2") "RULE bucket failed" }
if ($ExpectedMode -eq "CF") { Require ($page1.experimentBucket -eq "B" -and $page1.algorithmVersion -eq "CF_V1") "CF bucket failed" }
if ($ExpectedMode -eq "FALLBACK") { Require ($page1.experimentBucket -eq "B" -and $page1.algorithmVersion -eq "RULE_V2") "Python-down fallback failed" }

Write-Host "T19 home card system smoke passed"
