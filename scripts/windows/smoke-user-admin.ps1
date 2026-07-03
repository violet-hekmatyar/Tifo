param(
    [int]$Port = 8080,
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "password"
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
    if ($null -eq $Result.data -or $null -eq $Result.data.records -or $Result.data.records.Count -lt 1) {
        $json = $Result | ConvertTo-Json -Depth 10
        throw "$Label expected non-empty records, body=$json"
    }
    Write-Host "[OK] $Label records=$($Result.data.records.Count)"
}

Write-Host "T08 user/admin smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "my_admin_user_$suffix"
$phone = "136$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register user-admin user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login user-admin user"
$userToken = $login.data.accessToken
$userId = $login.data.user.id
if ([string]::IsNullOrWhiteSpace($userToken) -or $null -eq $userId) {
    throw "login did not return accessToken or user id"
}
Write-Host "[OK] user token acquired"

$preferences = Invoke-Json POST "/api/app/onboarding/preferences" @{
    mainTeamId = 30001
    followTeamIds = @(30001, 30002)
    followPlayerIds = @(40001, 40002)
} $userToken
Assert-Code $preferences 0 "save onboarding preferences"

$post = Invoke-Json POST "/api/app/contents/posts" @{
    title = "T08 smoke post $suffix"
    body = "This post makes the my contents page non-empty."
    mediaUrls = @("/uploads/content/t08-$suffix.jpg")
    relationList = @(
        @{
            relationType = "TEAM"
            relationId = 30001
        }
    )
} $userToken
Assert-Code $post 0 "create user post"

$favorite = Invoke-Json POST "/api/app/favorites/toggle" @{
    targetType = "CONTENT"
    targetId = 20001
} $userToken
Assert-Code $favorite 0 "favorite seed content"
if ($favorite.data.favorited -ne $true) {
    throw "favorite seed content expected favorited=true"
}

$comment = Invoke-Json POST "/api/app/comments" @{
    targetType = "CONTENT"
    targetId = 20001
    parentId = 0
    contentText = "T08 smoke comment."
} $userToken
Assert-Code $comment 0 "comment seed content"

$summary = Invoke-Json GET "/api/app/users/me/summary" $null $userToken
Assert-Code $summary 0 "my summary"
if ($null -eq $summary.data.stats) {
    throw "summary expected stats"
}

$profileUpdate = Invoke-Json PUT "/api/app/users/me/profile" @{
    nickname = "T08 Fan $suffix"
    avatarUrl = "/uploads/avatar/t08.png"
    bio = "T08 smoke profile"
    mainTeamId = 30001
} $userToken
Assert-Code $profileUpdate 0 "update my profile"

$myContents = Invoke-Json GET "/api/app/users/me/contents" $null $userToken
Assert-Code $myContents 0 "my contents"
Assert-Records $myContents "my contents"

$myFavorites = Invoke-Json GET "/api/app/users/me/favorites" $null $userToken
Assert-Code $myFavorites 0 "my favorites"
Assert-Records $myFavorites "my favorites"

$myComments = Invoke-Json GET "/api/app/users/me/comments" $null $userToken
Assert-Code $myComments 0 "my comments"
Assert-Records $myComments "my comments"

$summaryNoToken = Invoke-Json GET "/api/app/users/me/summary"
Assert-Code $summaryNoToken 40101 "my summary without token"

$adminLogin = Invoke-Json POST "/api/auth/login" @{
    username = $AdminUsername
    password = $AdminPassword
}
Assert-Code $adminLogin 0 "admin login"
$adminToken = $adminLogin.data.accessToken
if ([string]::IsNullOrWhiteSpace($adminToken)) {
    throw "admin login did not return accessToken"
}
Write-Host "[OK] admin token acquired"

$dashboard = Invoke-Json GET "/api/admin/dashboard/summary" $null $adminToken
Assert-Code $dashboard 0 "admin dashboard summary"

$adminUsers = Invoke-Json GET "/api/admin/users" $null $adminToken
Assert-Code $adminUsers 0 "admin users"
Assert-Records $adminUsers "admin users"

$adminUsersKeyword = Invoke-Json GET "/api/admin/users?keyword=$username" $null $adminToken
Assert-Code $adminUsersKeyword 0 "admin users keyword"

$disable = Invoke-Json PUT "/api/admin/users/$userId/status" @{
    status = "DISABLED"
    reason = "T08 smoke disable"
} $adminToken
Assert-Code $disable 0 "disable user"
if ($disable.data.status -ne "DISABLED") {
    throw "disable user expected status=DISABLED"
}

$enable = Invoke-Json PUT "/api/admin/users/$userId/status" @{
    status = "ACTIVE"
    reason = "T08 smoke enable"
} $adminToken
Assert-Code $enable 0 "enable user"
if ($enable.data.status -ne "ACTIVE") {
    throw "enable user expected status=ACTIVE"
}

$adminContents = Invoke-Json GET "/api/admin/contents" $null $adminToken
Assert-Code $adminContents 0 "admin contents"
Assert-Records $adminContents "admin contents"

$hideContent = Invoke-Json PUT "/api/admin/contents/20001/status" @{
    status = "HIDDEN"
    reason = "T08 smoke hide"
} $adminToken
Assert-Code $hideContent 0 "hide content"
if ($hideContent.data.status -ne "HIDDEN") {
    throw "hide content expected status=HIDDEN"
}

$hiddenDetail = Invoke-Json GET "/api/app/contents/20001" $null $userToken
Assert-Code $hiddenDetail 40401 "hidden content detail"

$restoreContent = Invoke-Json PUT "/api/admin/contents/20001/status" @{
    status = "PUBLISHED"
    reason = "T08 smoke restore"
} $adminToken
Assert-Code $restoreContent 0 "restore content"
if ($restoreContent.data.status -ne "PUBLISHED") {
    throw "restore content expected status=PUBLISHED"
}

$restoredDetail = Invoke-Json GET "/api/app/contents/20001" $null $userToken
Assert-Code $restoredDetail 0 "restored content detail"

$userAdmin = Invoke-Json GET "/api/admin/users" $null $userToken
Assert-Code $userAdmin 40301 "user forbidden for admin users"

$adminNoToken = Invoke-Json GET "/api/admin/dashboard/summary"
Assert-Code $adminNoToken 40101 "admin dashboard without token"

Write-Host "T08 user/admin smoke passed"
