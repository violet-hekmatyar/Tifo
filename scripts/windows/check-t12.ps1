$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]
$javaProcess = $null
$oldEnv = @{
    SPRING_PROFILES_ACTIVE = $env:SPRING_PROFILES_ACTIVE
    SERVER_PORT = $env:SERVER_PORT
    MYSQL_HOST = $env:MYSQL_HOST
    MYSQL_PORT = $env:MYSQL_PORT
    MYSQL_DATABASE = $env:MYSQL_DATABASE
    MYSQL_USERNAME = $env:MYSQL_USERNAME
    MYSQL_PASSWORD = $env:MYSQL_PASSWORD
    REDIS_HOST = $env:REDIS_HOST
    REDIS_PORT = $env:REDIS_PORT
    REDIS_PASSWORD = $env:REDIS_PASSWORD
    JWT_SECRET = $env:JWT_SECRET
    APP_FILE_STORAGE_ROOT = $env:APP_FILE_STORAGE_ROOT
    APP_FILE_LOCAL_STORAGE_ROOT = $env:APP_FILE_LOCAL_STORAGE_ROOT
    APP_FILE_STORAGE_TYPE = $env:APP_FILE_STORAGE_TYPE
}

function Pass($message) {
    Write-Host "[OK] $message"
}

function Fail($message) {
    Write-Host "[FAIL] $message"
    $script:failures.Add($message) | Out-Null
}

function Require-Path($path, $label) {
    if (Test-Path -LiteralPath $path) { Pass "$label exists" } else { Fail "$label missing: $path" }
}

function Require-Absent($path, $label) {
    if (Test-Path -LiteralPath $path) { Fail "$label must not exist: $path" } else { Pass "$label not present" }
}

function Invoke-Step($command, $label) {
    Write-Host ""
    Write-Host "Running: $label"
    Invoke-Expression $command
    if ($LASTEXITCODE -ne 0) {
        throw "$label failed with exit code $LASTEXITCODE"
    }
    Pass "$label passed"
}

function Restore-Env {
    foreach ($key in $oldEnv.Keys) {
        if ($null -ne $oldEnv[$key]) {
            Set-Item -Path "Env:$key" -Value $oldEnv[$key]
        } else {
            Remove-Item "Env:$key" -ErrorAction SilentlyContinue
        }
    }
}

try {
    Write-Host "T12 comment hot check"
    Write-Host "Working directory: $((Get-Location).Path)"

    Require-Path "pom.xml" "pom.xml"
    Require-Path "scripts/windows/reset-dev-db.ps1" "reset-dev-db.ps1"
    Require-Path "scripts/windows/smoke-auth.ps1" "smoke-auth.ps1"
    Require-Path "scripts/windows/smoke-football.ps1" "smoke-football.ps1"
    Require-Path "scripts/windows/smoke-feed.ps1" "smoke-feed.ps1"
    Require-Path "scripts/windows/smoke-file-upload.ps1" "smoke-file-upload.ps1"
    Require-Path "scripts/windows/smoke-storage-media.ps1" "smoke-storage-media.ps1"
    Require-Path "scripts/windows/smoke-user-social.ps1" "smoke-user-social.ps1"
    Require-Path "scripts/windows/smoke-comment-hot.ps1" "smoke-comment-hot.ps1"
    Require-Path "src/main/java/com/southstand/user/service/UserSocialService.java" "UserSocialService"
    Require-Path "src/main/java/com/southstand/user/controller/UserSocialController.java" "UserSocialController"
    Require-Path "src/main/java/com/southstand/interaction/service/CommentService.java" "CommentService"
    Require-Path "src/main/java/com/southstand/interaction/vo/HotCommentVO.java" "HotCommentVO"
    Require-Path "src/main/java/com/southstand/file/service/StorageService.java" "StorageService"
    Require-Path "src/main/java/com/southstand/file/service/LocalStorageService.java" "LocalStorageService"
    Require-Path "src/main/java/com/southstand/file/service/StorageServiceResolver.java" "StorageServiceResolver"
    Require-Path "src/main/java/com/southstand/file/service/AliyunOssStorageService.java" "AliyunOssStorageService"
    Require-Path "src/main/java/com/southstand/file/service/QiniuKodoStorageService.java" "QiniuKodoStorageService"
    Require-Path "src/main/java/com/southstand/file/service/MinioStorageService.java" "MinioStorageService"
    Require-Absent "application-prod.yml" "application-prod.yml"

    foreach ($trackedPattern in @(".env", "*.local", "tmp", "logs", "uploads", "target")) {
        $tracked = git ls-files -- $trackedPattern
        if (-not [string]::IsNullOrWhiteSpace($tracked)) {
            Fail "sensitive or generated path tracked by Git: $trackedPattern"
        }
    }

    if ($failures.Count -gt 0) {
        throw "pre-check failed"
    }

    $env:MYSQL_HOST = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "localhost" }
    $env:MYSQL_PORT = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
    $env:MYSQL_DATABASE = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "south_stand" }
    $env:MYSQL_USERNAME = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" }
    $env:REDIS_HOST = if ($env:REDIS_HOST) { $env:REDIS_HOST } else { "localhost" }
    $env:REDIS_PORT = if ($env:REDIS_PORT) { $env:REDIS_PORT } else { "6379" }
    $env:JWT_SECRET = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "dev_only_change_me_jwt_secret_please_override_in_prod_2026" }
    $env:APP_FILE_STORAGE_TYPE = "LOCAL"
    $env:APP_FILE_LOCAL_STORAGE_ROOT = Join-Path ([System.IO.Path]::GetTempPath()) "south-stand-t10-uploads"

    if (-not $env:MYSQL_PASSWORD) {
        $docker = Get-Command docker -ErrorAction SilentlyContinue
        if ($docker) {
            $containerId = (& $docker.Source ps --filter "name=^/apihub-mysql$" --format "{{.ID}}")
            if (-not [string]::IsNullOrWhiteSpace($containerId)) {
                $containerPassword = (& $docker.Source exec apihub-mysql printenv MYSQL_ROOT_PASSWORD)
                if (-not [string]::IsNullOrWhiteSpace($containerPassword)) {
                    $env:MYSQL_PASSWORD = $containerPassword.Trim()
                }
            }
        }
    }

    Invoke-Step ".\scripts\windows\reset-dev-db.ps1 -ConfirmReset -ConfirmationText 'RESET south_stand'" "reset-dev-db.ps1"
    Invoke-Step "mvn test" "mvn test"
    Invoke-Step "mvn clean package" "mvn clean package"

    Require-Path "target/south-stand-server.jar" "target/south-stand-server.jar"
    if ($failures.Count -gt 0) {
        throw "package artifact missing"
    }

    $script:port = 8080
    $listener = Get-NetTCPConnection -LocalPort $script:port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        Write-Host "[WARN] localhost:8080 is already in use, using 8090 for T12 smoke."
        $script:port = 8090
        $listener8090 = Get-NetTCPConnection -LocalPort $script:port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($listener8090) {
            throw "localhost:8080 and localhost:8090 are both in use. Please stop one port before running T12 check."
        }
    }

    $env:SPRING_PROFILES_ACTIVE = "dev"
    $env:SERVER_PORT = "$script:port"

    Write-Host ""
    Write-Host "Starting jar for T12 smoke on port $script:port..."
    $javaProcess = Start-Process -FilePath "java" `
        -ArgumentList @("-jar", "target/south-stand-server.jar", "--server.port=$script:port") `
        -WorkingDirectory (Get-Location).Path `
        -WindowStyle Hidden `
        -PassThru

    $started = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 2
        if ($javaProcess.HasExited) {
            throw "java process exited before smoke checks, exit code: $($javaProcess.ExitCode)"
        }
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:$script:port/api/public/health" -Method GET -TimeoutSec 5
            if ($health.code -eq 0) {
                $started = $true
                break
            }
        } catch {
            # keep waiting
        }
    }

    if (-not $started) {
        throw "application did not start before timeout"
    }

    Invoke-Step ".\scripts\windows\smoke-auth.ps1 -Port $script:port" "smoke-auth.ps1"
    Invoke-Step ".\scripts\windows\smoke-football.ps1 -Port $script:port" "smoke-football.ps1"
    Invoke-Step ".\scripts\windows\smoke-feed.ps1 -Port $script:port" "smoke-feed.ps1"
    Invoke-Step ".\scripts\windows\smoke-file-upload.ps1 -Port $script:port" "smoke-file-upload.ps1"
    Invoke-Step ".\scripts\windows\smoke-storage-media.ps1 -Port $script:port" "smoke-storage-media.ps1"
    Invoke-Step ".\scripts\windows\smoke-user-social.ps1 -Port $script:port" "smoke-user-social.ps1"
    Invoke-Step ".\scripts\windows\smoke-comment-hot.ps1 -Port $script:port" "smoke-comment-hot.ps1"

    Write-Host ""
    Write-Host "T12 check passed"
} catch {
    Fail $_.Exception.Message
    Write-Host ""
    Write-Host "T12 check failed"
    exit 1
} finally {
    if ($null -ne $javaProcess -and -not $javaProcess.HasExited) {
        Stop-Process -Id $javaProcess.Id -Force
        Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue
        Write-Host "Stopped java process PID $($javaProcess.Id)"
    }
    Remove-Item -LiteralPath $env:APP_FILE_LOCAL_STORAGE_ROOT -Recurse -Force -ErrorAction SilentlyContinue
    Restore-Env
}

if ($failures.Count -gt 0) {
    exit 1
}
exit 0
