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
    Start-Sleep -Milliseconds 3
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
    Write-Host "[OK] $Prefix token length=$($login.data.accessToken.Length)"
    return @{
        Token = $login.data.accessToken
        UserId = $login.data.user.id
    }
}

Write-Host "T12 comment hot smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$userA = New-SmokeUser "comment_a" "132"
$userB = New-SmokeUser "comment_b" "133"

$post = Invoke-Json POST "/api/app/contents/posts" @{
    title = "T12 hot comment smoke"
    body = "A post used by T12 comment hot smoke."
    relationList = @(
        @{
            relationType = "TEAM"
            relationId = 30001
        }
    )
} $userA.Token
Assert-Code $post 0 "create smoke post"
$contentId = $post.data.contentId

$comment = Invoke-Json POST "/api/app/comments" @{
    contentId = $contentId
    content = "This is the root comment for hot ranking."
} $userA.Token
Assert-Code $comment 0 "create root comment"
$commentId = $comment.data.commentId

$reply = Invoke-Json POST "/api/app/comments" @{
    contentId = $contentId
    parentId = $commentId
    replyToUserId = $userA.UserId
    content = "Reply from another user."
} $userB.Token
Assert-Code $reply 0 "create reply"

$like = Invoke-Json POST "/api/app/comments/$commentId/likes/toggle" $null $userB.Token
Assert-Code $like 0 "like root comment"
if ($like.data.liked -ne $true -or $like.data.likeCount -lt 1) {
    throw "like root comment expected liked=true and likeCount>=1"
}

$hotList = Invoke-Json GET "/api/app/comments?contentId=$contentId&sort=hot&pageNum=1&pageSize=20" $null $userB.Token
Assert-Code $hotList 0 "comments sort hot"
$rootFromHot = $hotList.data.records | Where-Object { $_.commentId -eq $commentId } | Select-Object -First 1
if ($null -eq $rootFromHot) {
    throw "hot list did not contain root comment"
}
if ($rootFromHot.likeCount -lt 1 -or $rootFromHot.replyCount -lt 1 -or $rootFromHot.liked -ne $true) {
    throw "hot root expected likeCount>=1, replyCount>=1, liked=true"
}
if ($null -eq $rootFromHot.replies -or $rootFromHot.replies.Count -lt 1) {
    throw "hot root expected reply preview"
}

$latestList = Invoke-Json GET "/api/app/comments?contentId=$contentId&sort=latest&pageNum=1&pageSize=20"
Assert-Code $latestList 0 "comments sort latest"

$replies = Invoke-Json GET "/api/app/comments/$commentId/replies?pageNum=1&pageSize=20&sort=latest"
Assert-Code $replies 0 "comment replies"
if ($null -eq $replies.data.records -or $replies.data.records.Count -lt 1) {
    throw "reply list expected records"
}
if ($replies.data.records[0].rootId -ne $commentId -or $replies.data.records[0].replyToUserId -ne $userA.UserId) {
    throw "reply list expected rootId and replyToUserId"
}

$hotComments = Invoke-Json GET "/api/app/comments/hot?contentId=$contentId&limit=3"
Assert-Code $hotComments 0 "hot comments"
if ($null -eq $hotComments.data -or $hotComments.data.Count -lt 1) {
    throw "hot comments expected records"
}
if ($hotComments.data[0].commentId -ne $commentId) {
    throw "hot comments expected root comment first"
}

$feed = Invoke-Json GET "/api/app/feed?tab=mixed&pageNum=1&pageSize=100" $null $userB.Token
Assert-Code $feed 0 "feed with hotComment"
$feedCard = $feed.data.records | Where-Object { $_.contentId -eq $contentId } | Select-Object -First 1
if ($null -eq $feedCard -or $null -eq $feedCard.hotComment -or $feedCard.hotComment.commentId -ne $commentId) {
    throw "feed expected hotComment for smoke post"
}

$delete = Invoke-Json DELETE "/api/app/comments/$commentId" $null $userA.Token
Assert-Code $delete 0 "delete own comment"

$afterDelete = Invoke-Json GET "/api/app/comments?contentId=$contentId&sort=hot&pageNum=1&pageSize=20"
Assert-Code $afterDelete 0 "comments after delete"
if ($afterDelete.data.records | Where-Object { $_.commentId -eq $commentId }) {
    throw "deleted comment must not appear in normal list"
}

$likeDeleted = Invoke-Json POST "/api/app/comments/$commentId/likes/toggle" $null $userB.Token
Assert-Code $likeDeleted 40401 "like deleted comment"

Write-Host "T12 comment hot smoke passed"
