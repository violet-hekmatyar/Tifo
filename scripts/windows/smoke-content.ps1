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

Write-Host "T05 content smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "content_user_$suffix"
$phone = "137$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register content user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login content user"
$token = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "login did not return accessToken"
}
Write-Host "[OK] content token acquired"

$seedContent = Invoke-Json GET "/api/app/contents/20001"
Assert-Code $seedContent 0 "seed content detail"
if ($seedContent.data.contentId -ne 20001) {
    throw "seed content detail returned unexpected contentId"
}
if ($seedContent.data.liked -ne $false -or $seedContent.data.favorited -ne $false) {
    throw "anonymous content detail expected liked=false and favorited=false"
}

$seedComments = Invoke-Json GET "/api/app/comments?targetType=CONTENT&targetId=20001"
Assert-Code $seedComments 0 "seed comments"
if ($seedComments.data.records.Count -lt 2) {
    throw "seed comments expected at least 2 root records"
}

$post = Invoke-Json POST "/api/app/contents/posts" @{
    title = "T05 smoke post $suffix"
    body = "This is a smoke post for content interaction closure."
    mediaUrls = @("/uploads/content/smoke-$suffix.jpg")
    relationList = @(
        @{
            relationType = "TEAM"
            relationId = 30001
        },
        @{
            relationType = "PLAYER"
            relationId = 40001
        }
    )
} $token
Assert-Code $post 0 "create post"
$contentId = $post.data.contentId
if ($null -eq $contentId) {
    throw "create post did not return contentId"
}

$newContent = Invoke-Json GET "/api/app/contents/$contentId" $null $token
Assert-Code $newContent 0 "new content detail"
if ($newContent.data.contentId -ne $contentId) {
    throw "new content detail returned unexpected contentId"
}

$comment = Invoke-Json POST "/api/app/comments" @{
    targetType = "CONTENT"
    targetId = $contentId
    parentId = 0
    contentText = "Root comment from T05 smoke."
} $token
Assert-Code $comment 0 "create root comment"
$commentId = $comment.data.commentId

$reply = Invoke-Json POST "/api/app/comments" @{
    targetType = "CONTENT"
    targetId = $contentId
    parentId = $commentId
    contentText = "Reply from T05 smoke."
} $token
Assert-Code $reply 0 "create reply"

$newComments = Invoke-Json GET "/api/app/comments?targetType=CONTENT&targetId=$contentId"
Assert-Code $newComments 0 "new content comments"
if ($newComments.data.records.Count -lt 1) {
    throw "new content comments expected records"
}

$likeContent = Invoke-Json POST "/api/app/likes/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
} $token
Assert-Code $likeContent 0 "like content"
if ($likeContent.data.liked -ne $true) {
    throw "like content expected liked=true"
}

$unlikeContent = Invoke-Json POST "/api/app/likes/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
} $token
Assert-Code $unlikeContent 0 "unlike content"
if ($unlikeContent.data.liked -ne $false) {
    throw "unlike content expected liked=false"
}

$likeComment = Invoke-Json POST "/api/app/likes/toggle" @{
    targetType = "COMMENT"
    targetId = $commentId
} $token
Assert-Code $likeComment 0 "like comment"
if ($likeComment.data.liked -ne $true) {
    throw "like comment expected liked=true"
}

$favorite = Invoke-Json POST "/api/app/favorites/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
} $token
Assert-Code $favorite 0 "favorite content"
if ($favorite.data.favorited -ne $true) {
    throw "favorite content expected favorited=true"
}

$unfavorite = Invoke-Json POST "/api/app/favorites/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
} $token
Assert-Code $unfavorite 0 "unfavorite content"
if ($unfavorite.data.favorited -ne $false) {
    throw "unfavorite content expected favorited=false"
}

$noTokenPost = Invoke-Json POST "/api/app/contents/posts" @{
    title = "no token"
    body = "should fail"
}
Assert-Code $noTokenPost 40101 "post without token"

$noTokenComment = Invoke-Json POST "/api/app/comments" @{
    targetType = "CONTENT"
    targetId = $contentId
    parentId = 0
    contentText = "should fail"
}
Assert-Code $noTokenComment 40101 "comment without token"

$noTokenLike = Invoke-Json POST "/api/app/likes/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
}
Assert-Code $noTokenLike 40101 "like without token"

$noTokenFavorite = Invoke-Json POST "/api/app/favorites/toggle" @{
    targetType = "CONTENT"
    targetId = $contentId
}
Assert-Code $noTokenFavorite 40101 "favorite without token"

Write-Host "T05 content smoke passed"
