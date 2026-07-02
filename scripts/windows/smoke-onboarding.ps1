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
            Write-Host $_.ErrorDetails.Message
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

Write-Host "T04 onboarding smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "onboard_user_$suffix"
$phone = "138$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register onboarding user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login onboarding user"
$token = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "login did not return accessToken"
}
Write-Host "[OK] onboarding token acquired"

$options = Invoke-Json GET "/api/app/onboarding/options" $null $token
Assert-Code $options 0 "onboarding options"
if ($options.data.recommendedTeams.Count -lt 2) {
    throw "recommendedTeams expected at least 2"
}
if ($options.data.recommendedPlayers.Count -lt 2) {
    throw "recommendedPlayers expected at least 2"
}
Write-Host "[OK] options returned teams=$($options.data.recommendedTeams.Count), players=$($options.data.recommendedPlayers.Count)"

$teamIds = @($options.data.recommendedTeams | Select-Object -First 2 | ForEach-Object { $_.teamId })
$playerIds = @($options.data.recommendedPlayers | Select-Object -First 2 | ForEach-Object { $_.playerId })
$mainTeamId = $teamIds[0]

$preferences = Invoke-Json POST "/api/app/onboarding/preferences" @{
    mainTeamId = $mainTeamId
    followTeamIds = $teamIds
    followPlayerIds = $playerIds
} $token
Assert-Code $preferences 0 "save onboarding preferences"

$profile = Invoke-Json GET "/api/app/users/me/profile" $null $token
Assert-Code $profile 0 "profile after onboarding"
if ($profile.data.onboardingCompleted -ne $true) {
    throw "profile onboardingCompleted expected true"
}
if ($null -eq $profile.data.mainTeam) {
    throw "profile mainTeam expected not null"
}
if ($profile.data.followStats.teamFollowCount -lt 2) {
    throw "teamFollowCount expected >= 2"
}
if ($profile.data.followStats.playerFollowCount -lt 2) {
    throw "playerFollowCount expected >= 2"
}
Write-Host "[OK] profile contains onboarding and follow stats"

$toggleOff = Invoke-Json POST "/api/app/follows/toggle" @{
    followType = "TEAM"
    targetId = $teamIds[1]
} $token
Assert-Code $toggleOff 0 "toggle team off"
if ($toggleOff.data.followed -ne $false) {
    throw "toggle off expected followed=false"
}

$toggleOn = Invoke-Json POST "/api/app/follows/toggle" @{
    followType = "TEAM"
    targetId = $teamIds[1]
} $token
Assert-Code $toggleOn 0 "toggle team on"
if ($toggleOn.data.followed -ne $true) {
    throw "toggle on expected followed=true"
}

$allTeamIds = @($options.data.recommendedTeams | Select-Object -First 6 | ForEach-Object { $_.teamId })
foreach ($teamId in $allTeamIds) {
    $follow = Invoke-Json POST "/api/app/follows/toggle" @{
        followType = "TEAM"
        targetId = $teamId
    } $token
    Assert-Code $follow 0 "toggle team $teamId"
    if ($follow.data.followed -eq $false) {
        $followAgain = Invoke-Json POST "/api/app/follows/toggle" @{
            followType = "TEAM"
            targetId = $teamId
        } $token
        Assert-Code $followAgain 0 "toggle team $teamId back on"
    }
}
$profileAfterSix = Invoke-Json GET "/api/app/users/me/profile" $null $token
Assert-Code $profileAfterSix 0 "profile after six team follows"
if ($profileAfterSix.data.followStats.teamFollowCount -lt 6) {
    throw "teamFollowCount expected >= 6 when no follow limit is enforced"
}
Write-Host "[OK] no team follow limit enforced, count=$($profileAfterSix.data.followStats.teamFollowCount)"

$noTokenPreferences = Invoke-Json POST "/api/app/onboarding/preferences" @{
    mainTeamId = $mainTeamId
    followTeamIds = @($mainTeamId)
    followPlayerIds = @()
}
Assert-Code $noTokenPreferences 40101 "preferences without token"

$noTokenProfile = Invoke-Json GET "/api/app/users/me/profile"
Assert-Code $noTokenProfile 40101 "profile without token"

Write-Host "T04 onboarding smoke passed"
