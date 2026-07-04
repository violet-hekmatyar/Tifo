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
        $json = $Result | ConvertTo-Json -Depth 10
        throw "$Label expected code=$ExpectedCode, actual=$($Result.code), body=$json"
    }
    Write-Host "[OK] $Label code=$ExpectedCode"
}

Write-Host "T10 storage media smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t10-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

try {
    $pngPath = Join-Path $tempDir "tiny.png"
    [System.IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="))

    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    $username = "storage_user_$suffix"
    $phone = "134$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
    $password = "Tifo123456"

    $register = Invoke-Json POST "/api/auth/register" @{
        username = $username
        phone = $phone
        password = $password
    }
    Assert-Code $register 0 "register storage user"

    $login = Invoke-Json POST "/api/auth/login" @{
        username = $username
        password = $password
    }
    Assert-Code $login 0 "login storage user"
    $token = $login.data.accessToken
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "login did not return accessToken"
    }
    Write-Host "[OK] token acquired length=$($token.Length)"

    $avatarUpload = Invoke-Upload $pngPath "AVATAR" $token
    Assert-Code $avatarUpload 0 "upload avatar"
    if ($avatarUpload.data.storageType -ne "LOCAL") {
        throw "avatar upload expected storageType=LOCAL, actual=$($avatarUpload.data.storageType)"
    }

    $bindAvatar = Invoke-Json POST "/api/app/users/me/avatar" @{
        fileId = $avatarUpload.data.fileId
    } $token
    Assert-Code $bindAvatar 0 "bind avatar"
    if ($bindAvatar.data.avatarUrl -ne $avatarUpload.data.url) {
        throw "bind avatar returned unexpected avatarUrl"
    }

    $contentImageUpload = Invoke-Upload $pngPath "CONTENT_IMAGE" $token
    Assert-Code $contentImageUpload 0 "upload content image"
    if ($contentImageUpload.data.storageType -ne "LOCAL") {
        throw "content image expected storageType=LOCAL"
    }

    $post = Invoke-Json POST "/api/app/contents/posts" @{
        title = "T10 storage media post $suffix"
        body = "T10 post with mediaFileIds."
        mediaFileIds = @($contentImageUpload.data.fileId)
    } $token
    Assert-Code $post 0 "create post with mediaFileIds"
    $contentId = $post.data.contentId

    $detail = Invoke-Json GET "/api/app/contents/$contentId" $null $token
    Assert-Code $detail 0 "content detail with media"
    $mediaUrls = @($detail.data.mediaList | ForEach-Object { $_.mediaUrl })
    if ($mediaUrls -notcontains $contentImageUpload.data.url) {
        $json = $detail.data.mediaList | ConvertTo-Json -Depth 10
        throw "content detail mediaList did not contain uploaded url. mediaList=$json"
    }
    Write-Host "[OK] content detail includes uploaded media url"

    $delete = Invoke-Json DELETE "/api/app/files/$($avatarUpload.data.fileId)" $null $token
    Assert-Code $delete 0 "soft delete avatar file"

    try {
        $deletedResponse = Invoke-WebRequest -Uri "http://localhost:$Port$($avatarUpload.data.url)" -Method GET -TimeoutSec 10 -UseBasicParsing
        throw "deleted public file expected 404, actual=$($deletedResponse.StatusCode)"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -ne 404) {
            throw "deleted public file expected 404, actual=$statusCode"
        }
    }
    Write-Host "[OK] deleted public file returns 404"

    Write-Host "T10 storage media smoke passed"
} finally {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
