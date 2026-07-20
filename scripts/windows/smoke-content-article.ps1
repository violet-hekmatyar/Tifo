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
        $params["Body"] = ($Body | ConvertTo-Json -Depth 12)
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

function Invoke-Upload($Path, $BizType, $Token) {
    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    try {
        $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        $content = [System.Net.Http.MultipartFormDataContent]::new()
        $bytes = [System.IO.File]::ReadAllBytes($Path)
        $fileContent = [System.Net.Http.ByteArrayContent]::new($bytes)
        $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/png")
        $content.Add($fileContent, "file", [System.IO.Path]::GetFileName($Path))
        $content.Add([System.Net.Http.StringContent]::new($BizType), "bizType")
        $response = $client.PostAsync("http://localhost:$Port/api/app/files/upload", $content).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        return ($body | ConvertFrom-Json)
    } finally {
        $client.Dispose()
    }
}

function Assert-Code($Result, [int]$ExpectedCode, [string]$Label) {
    if ($Result.code -ne $ExpectedCode) {
        $json = $Result | ConvertTo-Json -Depth 12
        throw "$Label expected code=$ExpectedCode, actual=$($Result.code), body=$json"
    }
    Write-Host "[OK] $Label code=$ExpectedCode"
}

Write-Host "T13 content article smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t13-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

try {
    $pngPath = Join-Path $tempDir "tiny.png"
    [System.IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="))

    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    $username = "article_user_$suffix"
    $phone = "131$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
    $password = "Tifo123456"

    $register = Invoke-Json POST "/api/auth/register" @{
        username = $username
        phone = $phone
        password = $password
    }
    Assert-Code $register 0 "register article user"

    $login = Invoke-Json POST "/api/auth/login" @{
        username = $username
        password = $password
    }
    Assert-Code $login 0 "login article user"
    $token = $login.data.accessToken
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "login did not return accessToken"
    }
    Write-Host "[OK] token length=$($token.Length)"

    $cover = Invoke-Upload $pngPath "CONTENT_IMAGE" $token
    Assert-Code $cover 0 "upload cover"
    $image = Invoke-Upload $pngPath "CONTENT_IMAGE" $token
    Assert-Code $image 0 "upload article image"

    $article = Invoke-Json POST "/api/app/contents/articles" @{
        title = "T13 article smoke"
        summary = "T13 summary"
        coverFileId = $cover.data.fileId
        blocks = @(
            @{ blockType = "TEXT"; text = "First article paragraph."; sortOrder = 1 },
            @{ blockType = "IMAGE"; mediaFileId = $image.data.fileId; sortOrder = 2 },
            @{ blockType = "TEXT"; text = "Second article paragraph."; sortOrder = 3 }
        )
        relationList = @(
            @{ relationType = "TEAM"; relationId = 30001 },
            @{ relationType = "TEAM"; relationId = 30001 },
            @{ relationType = "PLAYER"; relationId = 40001 },
            @{ relationType = "MATCH"; relationId = 50001 }
        )
    } $token
    Assert-Code $article 0 "create article"
    $articleId = $article.data.contentId

    $detail = Invoke-Json GET "/api/app/contents/$articleId" $null $token
    Assert-Code $detail 0 "article detail"
    if ($detail.data.contentType -ne "ARTICLE" -or $detail.data.blocks.Count -ne 3) {
        throw "article detail expected ARTICLE with 3 blocks"
    }
    if ($detail.data.blocks[0].sortOrder -ne 1 -or $detail.data.blocks[1].blockType -ne "IMAGE" -or $detail.data.blocks[2].sortOrder -ne 3) {
        throw "article blocks order/type mismatch"
    }
    if ($detail.data.relationList.Count -ne 3) {
        throw "article relationList expected deduplicated count=3, actual=$($detail.data.relationList.Count)"
    }

    $updated = Invoke-Json PUT "/api/app/contents/$articleId/articles" @{
        title = "T13 article smoke updated"
        summary = "T13 updated summary"
        coverFileId = $cover.data.fileId
        blocks = @(
            @{ blockType = "TEXT"; text = "Updated paragraph."; sortOrder = 1 },
            @{ blockType = "IMAGE"; mediaFileId = $image.data.fileId; sortOrder = 2 }
        )
        relationList = @(
            @{ relationType = "TEAM"; relationId = 30002 }
        )
    } $token
    Assert-Code $updated 0 "update article"
    if ($updated.data.blocks.Count -ne 2 -or $updated.data.title -ne "T13 article smoke updated") {
        throw "updated article detail mismatch"
    }

    $post = Invoke-Json POST "/api/app/contents/posts" @{
        title = "T13 liked post"
        body = "A post for my likes."
        relationList = @(
            @{ relationType = "TEAM"; relationId = 30001 },
            @{ relationType = "TEAM"; relationId = 30001 }
        )
    } $token
    Assert-Code $post 0 "create post"
    $postId = $post.data.contentId

    $like = Invoke-Json POST "/api/app/likes/toggle" @{
        targetType = "CONTENT"
        targetId = $postId
    } $token
    Assert-Code $like 0 "like post"

    $comment = Invoke-Json POST "/api/app/comments" @{
        contentId = $postId
        content = "comment for comment-like filtering"
    } $token
    Assert-Code $comment 0 "create comment"
    $commentLike = Invoke-Json POST "/api/app/comments/$($comment.data.commentId)/likes/toggle" $null $token
    Assert-Code $commentLike 0 "like comment"

    $likes = Invoke-Json GET "/api/app/users/me/likes?pageNum=1&pageSize=20&targetType=CONTENT" $null $token
    Assert-Code $likes 0 "my likes"
    if (-not ($likes.data.records | Where-Object { $_.contentId -eq $postId })) {
        throw "my likes expected liked content"
    }
    if ($likes.data.records | Where-Object { $_.contentId -eq $comment.data.commentId }) {
        throw "my likes must not include comment likes"
    }

    $unlike = Invoke-Json POST "/api/app/likes/toggle" @{
        targetType = "CONTENT"
        targetId = $postId
    } $token
    Assert-Code $unlike 0 "unlike post"
    $likesAfterUnlike = Invoke-Json GET "/api/app/users/me/likes?pageNum=1&pageSize=20&targetType=CONTENT" $null $token
    Assert-Code $likesAfterUnlike 0 "my likes after unlike"
    if ($likesAfterUnlike.data.records | Where-Object { $_.contentId -eq $postId }) {
        throw "my likes should not contain unliked content"
    }

    $teamSearch = Invoke-Json GET "/api/app/search/entities?keyword=Barcelona&entityType=TEAM"
    Assert-Code $teamSearch 0 "search team"
    if ($teamSearch.data.records.Count -lt 1) { throw "team search expected records" }

    $playerSearch = Invoke-Json GET "/api/app/search/entities?keyword=Lewandowski&entityType=PLAYER"
    Assert-Code $playerSearch 0 "search player"
    if ($playerSearch.data.records.Count -lt 1) { throw "player search expected records" }

    $matchSearch = Invoke-Json GET "/api/app/search/entities?keyword=Barcelona&entityType=MATCH"
    Assert-Code $matchSearch 0 "search match"
    if ($matchSearch.data.records.Count -lt 1) { throw "match search expected records" }

    $badRelation = Invoke-Json POST "/api/app/contents/articles" @{
        title = "bad relation"
        blocks = @(
            @{ blockType = "TEXT"; text = "bad"; sortOrder = 1 }
        )
        relationList = @(
            @{ relationType = "TEAM"; relationId = 999999999 }
        )
    } $token
    Assert-Code $badRelation 40401 "bad relation"

    foreach ($teamId in @(30001,30002,30003,30004,30005,30006)) {
        $follow = Invoke-Json POST "/api/app/follows/toggle" @{
            followType = "TEAM"
            targetId = $teamId
        } $token
        Assert-Code $follow 0 "follow team $teamId"
    }

    Write-Host "T13 content article smoke passed"
} finally {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
