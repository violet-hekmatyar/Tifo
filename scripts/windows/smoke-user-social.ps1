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

function New-SmokeUser([string]$Prefix, [string]$PhonePrefix) {
    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    Start-Sleep -Milliseconds 2
    $username = "$Prefix`_$suffix"
    $phone = "$PhonePrefix$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
    $password = "Tifo123456"

    $register = Invoke-Json POST "/api/auth/register" @{
        username = $username
        phone = $phone
        password = $password
    }
    Assert-Code $register 0 "register $Prefix"

    $login = Invoke-Json POST "/api/auth/login" @{
        username = $username
        password = $password
    }
    Assert-Code $login 0 "login $Prefix"
    if ([string]::IsNullOrWhiteSpace($login.data.accessToken)) {
        throw "$Prefix login did not return accessToken"
    }
    return @{
        Username = $username
        Password = $password
        Token = $login.data.accessToken
        UserId = $login.data.user.id
    }
}

Write-Host "T11 user social smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$userA = New-SmokeUser "social_a" "136"
$userB = New-SmokeUser "social_b" "138"

$publicProfile = Invoke-Json GET "/api/app/users/$($userB.UserId)/profile"
Assert-Code $publicProfile 0 "public user profile without token"
if ($publicProfile.data.relationStatus -ne "NONE") {
    throw "anonymous relationStatus expected NONE, actual=$($publicProfile.data.relationStatus)"
}

$profileWithToken = Invoke-Json GET "/api/app/users/$($userB.UserId)/profile" $null $userA.Token
Assert-Code $profileWithToken 0 "user profile with token"
if ($profileWithToken.data.relationStatus -ne "NONE") {
    throw "initial relationStatus expected NONE, actual=$($profileWithToken.data.relationStatus)"
}

$selfProfile = Invoke-Json GET "/api/app/users/$($userA.UserId)/profile" $null $userA.Token
Assert-Code $selfProfile 0 "self public profile with token"
if ($selfProfile.data.relationStatus -ne "SELF") {
    throw "self relationStatus expected SELF, actual=$($selfProfile.data.relationStatus)"
}

$follow = Invoke-Json POST "/api/app/users/$($userB.UserId)/follow" $null $userA.Token
Assert-Code $follow 0 "follow user"
if ($follow.data.followed -ne $true -or $follow.data.relationStatus -ne "FOLLOWING") {
    throw "follow expected followed=true and FOLLOWING"
}

$afterFollow = Invoke-Json GET "/api/app/users/$($userB.UserId)/profile" $null $userA.Token
Assert-Code $afterFollow 0 "profile after follow"
if ($afterFollow.data.relationStatus -ne "FOLLOWING") {
    throw "after follow relationStatus expected FOLLOWING, actual=$($afterFollow.data.relationStatus)"
}

$followings = Invoke-Json GET "/api/app/users/$($userA.UserId)/followings?pageNum=1&pageSize=10" $null $userA.Token
Assert-Code $followings 0 "followings list"
if (-not ($followings.data.records | Where-Object { $_.userId -eq $userB.UserId })) {
    throw "followings list did not contain user B"
}

$followers = Invoke-Json GET "/api/app/users/$($userB.UserId)/followers?pageNum=1&pageSize=10" $null $userA.Token
Assert-Code $followers 0 "followers list"
if (-not ($followers.data.records | Where-Object { $_.userId -eq $userA.UserId })) {
    throw "followers list did not contain user A"
}

$reverseProfile = Invoke-Json GET "/api/app/users/$($userA.UserId)/profile" $null $userB.Token
Assert-Code $reverseProfile 0 "reverse relation profile"
if ($reverseProfile.data.relationStatus -ne "FOLLOWED_BY") {
    throw "reverse relationStatus expected FOLLOWED_BY, actual=$($reverseProfile.data.relationStatus)"
}

$mutual = Invoke-Json POST "/api/app/users/$($userA.UserId)/follow" $null $userB.Token
Assert-Code $mutual 0 "mutual follow"
if ($mutual.data.relationStatus -ne "MUTUAL") {
    throw "mutual relationStatus expected MUTUAL, actual=$($mutual.data.relationStatus)"
}

$post = Invoke-Json POST "/api/app/contents/posts" @{
    title = "T11 social smoke post"
    body = "Post from followed user for following feed."
    relationList = @(
        @{
            relationType = "TEAM"
            relationId = 30001
        }
    )
} $userB.Token
Assert-Code $post 0 "create followed-user post"

$contents = Invoke-Json GET "/api/app/users/$($userB.UserId)/contents?pageNum=1&pageSize=10" $null $userA.Token
Assert-Code $contents 0 "public user contents"

$stand = Invoke-Json GET "/api/app/users/me/stand" $null $userA.Token
Assert-Code $stand 0 "my stand"
if ($stand.data.followingUserCount -lt 1) {
    throw "my stand expected followingUserCount >= 1"
}

$followingFeed = Invoke-Json GET "/api/app/feed?tab=following&pageNum=1&pageSize=10" $null $userA.Token
Assert-Code $followingFeed 0 "following feed after user follow"
if ($null -eq $followingFeed.data.records) {
    throw "following feed expected records collection"
}

$favoriteOther = Invoke-Json GET "/api/app/users/$($userB.UserId)/favorites?pageNum=1&pageSize=10" $null $userA.Token
Assert-Code $favoriteOther 40301 "other user favorites forbidden"

$selfFollow = Invoke-Json POST "/api/app/users/$($userA.UserId)/follow" $null $userA.Token
Assert-Code $selfFollow 40001 "self follow forbidden"

$unfollow = Invoke-Json DELETE "/api/app/users/$($userB.UserId)/follow" $null $userA.Token
Assert-Code $unfollow 0 "unfollow user"
if ($unfollow.data.followed -ne $false -or $unfollow.data.relationStatus -ne "FOLLOWED_BY") {
    throw "unfollow expected followed=false and FOLLOWED_BY because user B still follows user A"
}

Write-Host "T11 user social smoke passed"
