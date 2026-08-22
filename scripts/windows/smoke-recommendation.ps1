param([int]$Port = 8080, [ValidateSet("ANY", "RULE", "CF", "FALLBACK")][string]$ExpectedMode = "ANY")
$ErrorActionPreference = "Stop"
function Json($method, $path, $body = $null, $token = $null) {
    $p = @{ Uri="http://127.0.0.1:$Port$path"; Method=$method; TimeoutSec=10; Headers=@{} }
    if ($token) { $p.Headers.Authorization = "Bearer $token" }
    if ($null -ne $body) { $p.ContentType="application/json"; $p.Body=($body|ConvertTo-Json -Depth 10) }
    Invoke-RestMethod @p
}
function Require($condition, $message) { if (-not $condition) { throw $message } }

$feed = $null; $token = $null
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds() + $attempt
    $username = "t18_${ExpectedMode}_$suffix"
    $register = Json POST "/api/auth/register" @{username=$username;phone="137$($suffix.ToString().Substring($suffix.ToString().Length-8))";password="Tifo123456"}
    Require ($register.code -eq 0) "T18 registration failed"
    $login = Json POST "/api/auth/login" @{username=$username;password="Tifo123456"}
    $candidateToken = $login.data.accessToken
    $candidateFeed = Json GET "/api/app/feed?tab=recommend&pageNum=1&pageSize=10" $null $candidateToken
    $matches = $ExpectedMode -eq "ANY" `
        -or ($ExpectedMode -eq "RULE" -and $candidateFeed.data.experimentBucket -eq "A" -and $candidateFeed.data.algorithmVersion -eq "RULE_V2") `
        -or ($ExpectedMode -eq "CF" -and $candidateFeed.data.experimentBucket -eq "B" -and $candidateFeed.data.algorithmVersion -eq "CF_V1") `
        -or ($ExpectedMode -eq "FALLBACK" -and $candidateFeed.data.experimentBucket -eq "B" -and $candidateFeed.data.algorithmVersion -eq "RULE_V2")
    if ($matches) { $feed = $candidateFeed; $token = $candidateToken; break }
}
Require ($null -ne $feed) "could not dynamically find a user for expected mode $ExpectedMode"
Require ($feed.code -eq 0 -and $feed.data.records.Count -gt 0) "recommend feed unavailable"
Require ($feed.data.algorithmVersion -in @("RULE_V2","CF_V1","HOT")) "missing algorithmVersion"
Require ($feed.data.experimentBucket -in @("A","B")) "missing stable experiment bucket"
Require (-not [string]::IsNullOrWhiteSpace($feed.data.requestId)) "missing requestId"
Require ($feed.data.records[0].position -ge 0 -and $feed.data.records[0].impressionId) "missing card attribution"
if ($ExpectedMode -eq "RULE") { Require ($feed.data.algorithmVersion -eq "RULE_V2" -and $feed.data.experimentBucket -eq "A") "expected RULE_V2 bucket A" }
if ($ExpectedMode -eq "CF") { Require ($feed.data.algorithmVersion -eq "CF_V1" -and $feed.data.experimentBucket -eq "B") "expected CF_V1 bucket B" }
if ($ExpectedMode -eq "FALLBACK") {
    Require ($feed.data.algorithmVersion -eq "RULE_V2") "expected RULE_V2 fallback"
    Require ($feed.data.experimentBucket -eq "B") "fallback must retain bucket B"
}

$eventId = "t18-smoke-$suffix"
$events = @("EXPOSE","CLICK","DETAIL","LIKE","FAVORITE","COMMENT") | ForEach-Object {
    @{clientEventId="$eventId-$_";behaviorType=$_;targetType="CONTENT";targetId=16000000000000001;
      scene="HOME_RECOMMEND";algorithmVersion=$feed.data.algorithmVersion;modelVersion=$feed.data.modelVersion;
      experimentId=$feed.data.experimentId;experimentBucket=$feed.data.experimentBucket;requestId=$feed.data.requestId;
      impressionId=$feed.data.records[0].impressionId;position=0;dwellMs=$(if($_ -eq "DETAIL"){15000}else{$null});eventTime=(Get-Date).ToString("s")}
}
$first = Json POST "/api/app/recommendation/behaviors/batch" @{events=$events} $token
$second = Json POST "/api/app/recommendation/behaviors/batch" @{events=$events} $token
Require ($first.data.saved -eq 6) "behavior batch did not save all six types"
Require ($second.data.duplicated -eq 6) "clientEventId idempotency failed"
Write-Host "T18 recommendation smoke passed: algorithm=$($feed.data.algorithmVersion), bucket=$($feed.data.experimentBucket), cards=$($feed.data.records.Count)"
