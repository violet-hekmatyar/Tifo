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
        $params["Body"] = ($Body | ConvertTo-Json -Depth 8)
    }
    return Invoke-RestMethod @params
}

function Assert-Code($Result, [int]$ExpectedCode, [string]$Label) {
    if ($Result.code -ne $ExpectedCode) {
        throw "$Label expected code=$ExpectedCode, actual=$($Result.code), message=$($Result.message)"
    }
    Write-Host "[OK] $Label code=$ExpectedCode"
}

Write-Host "T03 auth smoke on port $Port"

$health = Invoke-Json GET "/api/public/health"
Assert-Code $health 0 "public health"

$dbHealth = Invoke-Json GET "/api/public/health/db"
Assert-Code $dbHealth 0 "db health"

$redisHealth = Invoke-Json GET "/api/public/health/redis"
Assert-Code $redisHealth 0 "redis health"

$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$username = "test_user_$suffix"
$phone = "139$($suffix.ToString().Substring($suffix.ToString().Length - 8))"
$password = "Tifo123456"

$register = Invoke-Json POST "/api/auth/register" @{
    username = $username
    phone = $phone
    password = $password
}
Assert-Code $register 0 "register unique user"

$login = Invoke-Json POST "/api/auth/login" @{
    username = $username
    password = $password
}
Assert-Code $login 0 "login unique user"
$userToken = $login.data.accessToken
if ([string]::IsNullOrWhiteSpace($userToken)) {
    throw "login did not return accessToken"
}
Write-Host "[OK] user token acquired"

$me = Invoke-Json GET "/api/auth/me" $null $userToken
Assert-Code $me 0 "me with token"
if ($me.data.username -ne $username) {
    throw "me returned unexpected username: $($me.data.username)"
}

$meNoToken = Invoke-Json GET "/api/auth/me"
Assert-Code $meNoToken 40101 "me without token"

$userAdmin = Invoke-Json GET "/api/admin/health" $null $userToken
Assert-Code $userAdmin 40301 "user forbidden for admin health"

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

$adminHealth = Invoke-Json GET "/api/admin/health" $null $adminToken
Assert-Code $adminHealth 0 "admin health"

$lockedUsername = "locked_user_$suffix"
for ($i = 1; $i -le 6; $i++) {
    $badLogin = Invoke-Json POST "/api/auth/login" @{
        username = $lockedUsername
        password = "wrong-password"
    }
    Write-Host "[OK] bad login attempt $i returned code=$($badLogin.code)"
    if ($i -lt 6 -and $badLogin.code -ne 40101) {
        throw "bad login attempt $i expected 40101 before lock, actual=$($badLogin.code)"
    }
    if ($i -eq 6 -and $badLogin.code -ne 40103) {
        throw "bad login attempt $i expected 40103 lock, actual=$($badLogin.code)"
    }
}

Write-Host "T03 auth smoke passed"
