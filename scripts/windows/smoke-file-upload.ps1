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

function Invoke-Upload($Path, $BizType, $Token = $null) {
    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    try {
        if ($Token) {
            $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
        }
        $content = [System.Net.Http.MultipartFormDataContent]::new()
        $bytes = [System.IO.File]::ReadAllBytes($Path)
        $fileContent = [System.Net.Http.ByteArrayContent]::new($bytes)
        if ($Path.EndsWith(".png")) {
            $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/png")
        } else {
            $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/plain")
        }
        $content.Add($fileContent, "file", [System.IO.Path]::GetFileName($Path))
        $content.Add([System.Net.Http.StringContent]::new($BizType), "bizType")
        $response = $client.PostAsync("http://localhost:$Port/api/app/files/upload", $content).GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if ([string]::IsNullOrWhiteSpace($body)) {
            return [PSCustomObject]@{ statusCode = [int]$response.StatusCode; body = $null }
        }
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

Write-Host "T09 file upload smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t09-$([DateTimeOffset]::Now.ToUnixTimeMilliseconds())"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

try {
    $pngPath = Join-Path $tempDir "tiny.png"
    $txtPath = Join-Path $tempDir "evil.txt"
    [System.IO.File]::WriteAllBytes($pngPath, [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="))
    Set-Content -LiteralPath $txtPath -Value "not an image" -Encoding ASCII

    $suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
    $username = "file_user_$suffix"
    $phone = "135$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
    $password = "Tifo123456"

    $register = Invoke-Json POST "/api/auth/register" @{
        username = $username
        phone = $phone
        password = $password
    }
    Assert-Code $register 0 "register file user"

    $login = Invoke-Json POST "/api/auth/login" @{
        username = $username
        password = $password
    }
    Assert-Code $login 0 "login file user"
    $token = $login.data.accessToken
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "login did not return accessToken"
    }
    Write-Host "[OK] token acquired length=$($token.Length)"

    $upload = Invoke-Upload $pngPath "AVATAR" $token
    Assert-Code $upload 0 "upload png"
    if ($null -eq $upload.data.fileId -or [string]::IsNullOrWhiteSpace($upload.data.url)) {
        throw "upload did not return fileId/url"
    }
    if ($upload.data.storageType -and $upload.data.storageType -ne "LOCAL") {
        throw "upload expected storageType=LOCAL when present, actual=$($upload.data.storageType)"
    }
    if ($upload.data.url -like "*:\*" -or $upload.data.url -like "/*/*:*") {
        throw "upload returned suspicious local path: $($upload.data.url)"
    }
    Write-Host "[OK] uploaded fileId=$($upload.data.fileId), url=$($upload.data.url)"

    $publicResponse = Invoke-WebRequest -Uri "http://localhost:$Port$($upload.data.url)" -Method GET -TimeoutSec 10 -UseBasicParsing
    if ($publicResponse.StatusCode -ne 200) {
        throw "public file expected 200, actual=$($publicResponse.StatusCode)"
    }
    $nosniff = $publicResponse.Headers["X-Content-Type-Options"]
    if ($null -eq $nosniff -or "$nosniff" -ne "nosniff") {
        throw "public file expected X-Content-Type-Options=nosniff, actual=$nosniff"
    }
    Write-Host "[OK] public file accessible"

    $noTokenUpload = Invoke-Upload $pngPath "AVATAR"
    Assert-Code $noTokenUpload 40101 "upload without token"

    $txtUpload = Invoke-Upload $txtPath "GENERAL_IMAGE" $token
    Assert-Code $txtUpload 40001 "upload invalid extension"

    Write-Host "T09 file upload smoke passed"
} finally {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
