$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]
$javaProcess = $null
$port = 8080

function Pass($message) {
    Write-Host "[OK] $message"
}

function Fail($message) {
    Write-Host "[FAIL] $message"
    $script:failures.Add($message) | Out-Null
}

function Require-Path($path, $label) {
    if (Test-Path -LiteralPath $path) {
        Pass "$label exists"
    } else {
        Fail "$label missing: $path"
    }
}

function Require-Absent($path, $label) {
    if (Test-Path -LiteralPath $path) {
        Fail "$label must not exist: $path"
    } else {
        Pass "$label not present"
    }
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

try {
    Write-Host "T01 Spring Boot skeleton check"
    Write-Host "Working directory: $((Get-Location).Path)"

    Require-Path "pom.xml" "pom.xml"
    Require-Path "src/main/java/com/southstand/SouthStandApplication.java" "SouthStandApplication"
    Require-Absent "application-prod.yml" "application-prod.yml"
    Require-Absent "Dockerfile" "Dockerfile"
    Require-Absent "docker-compose.yml" "docker-compose.yml"
    Require-Absent "schema.sql" "schema.sql"
    Require-Absent "seed.sql" "seed.sql"
    Require-Absent "scripts/sql/schema.sql" "scripts/sql/schema.sql"
    Require-Absent "scripts/sql/seed.sql" "scripts/sql/seed.sql"

    if ($failures.Count -gt 0) {
        throw "pre-check failed"
    }

    Invoke-Step "mvn clean test" "mvn clean test"
    Invoke-Step "mvn clean package" "mvn clean package"

    Require-Path "target/south-stand-server.jar" "target/south-stand-server.jar"
    if ($failures.Count -gt 0) {
        throw "package artifact missing"
    }

    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($listener) {
        $owner = Get-CimInstance Win32_Process -Filter "ProcessId = $($listener.OwningProcess)" -ErrorAction SilentlyContinue
        Write-Host "[WARN] localhost:$port is already used by PID $($listener.OwningProcess)"
        if ($owner -and $owner.CommandLine) {
            Write-Host "[WARN] Existing command line: $($owner.CommandLine)"
        }
        $port = 18080
        Write-Host "[WARN] Using localhost:$port for T01 jar smoke to avoid stopping unrelated Java processes."
    }

    Write-Host ""
    Write-Host "Starting jar for health smoke on port $port..."
    $javaProcess = Start-Process -FilePath "java" `
        -ArgumentList @("-jar", "target/south-stand-server.jar", "--server.port=$port") `
        -WorkingDirectory (Get-Location).Path `
        -WindowStyle Hidden `
        -PassThru

    $health = $null
    $lastError = $null
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        if ($javaProcess.HasExited) {
            throw "java process exited before health check, exit code: $($javaProcess.ExitCode)"
        }
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:$port/api/public/health" -Method GET -TimeoutSec 5
            break
        } catch {
            $lastError = $_.Exception.Message
        }
    }

    if ($null -eq $health) {
        throw "health endpoint did not respond: $lastError"
    }

    $health | ConvertTo-Json -Depth 8

    if ($health.code -ne 0) {
        throw "health code expected 0, actual: $($health.code)"
    }
    if ($health.message -ne "success") {
        throw "health message expected success, actual: $($health.message)"
    }
    if ($health.data.status -ne "UP") {
        throw "health data.status expected UP, actual: $($health.data.status)"
    }
    Pass "health endpoint returned code=0, message=success, data.status=UP"

    Write-Host ""
    Write-Host "T01 check passed"
} catch {
    Fail $_.Exception.Message
    Write-Host ""
    Write-Host "T01 check failed"
    exit 1
} finally {
    if ($null -ne $javaProcess -and -not $javaProcess.HasExited) {
        Stop-Process -Id $javaProcess.Id -Force
        Wait-Process -Id $javaProcess.Id -Timeout 10 -ErrorAction SilentlyContinue
        Write-Host "Stopped java process PID $($javaProcess.Id)"
    }
}

if ($failures.Count -gt 0) {
    exit 1
}
exit 0
