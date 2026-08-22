param([int]$Port = 8100)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$serviceRoot = Join-Path $root "recommend-service"
$pidFile = Join-Path ([IO.Path]::GetTempPath()) "south-stand-recommend-$Port.pid"
$outLog = Join-Path ([IO.Path]::GetTempPath()) "south-stand-recommend-$Port.log"
$errLog = "$outLog.err"

if (Test-Path -LiteralPath $pidFile) {
    $oldPid = [int](Get-Content -Raw -LiteralPath $pidFile)
    $old = Get-CimInstance Win32_Process -Filter "ProcessId=$oldPid" -ErrorAction SilentlyContinue
    if ($old -and $old.CommandLine -match "uvicorn" -and $old.CommandLine -match "app.main:app") {
        Write-Host "[OK] recommendation service already owns PID $oldPid"
        exit 0
    }
    Remove-Item -LiteralPath $pidFile -Force
}

& py -c "import fastapi,uvicorn,pydantic,pymysql" | Out-Null
if ($LASTEXITCODE -ne 0) { throw "T18 Python dependencies are unavailable" }
if (-not $env:DB_HOST) { $env:DB_HOST = "127.0.0.1" }
if (-not $env:DB_PORT) { $env:DB_PORT = "3306" }
if (-not $env:DB_NAME) { $env:DB_NAME = "south_stand" }
if (-not $env:DB_USER) { $env:DB_USER = if ($env:MYSQL_USERNAME) { $env:MYSQL_USERNAME } else { "root" } }
if (-not $env:DB_PASSWORD -and $env:MYSQL_PASSWORD) { $env:DB_PASSWORD = $env:MYSQL_PASSWORD }

$process = Start-Process py -ArgumentList @("-m", "uvicorn", "app.main:app", "--host", "127.0.0.1", "--port", $Port) `
    -WorkingDirectory $serviceRoot -WindowStyle Hidden -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
Set-Content -LiteralPath $pidFile -Value $process.Id -Encoding ascii
for ($i = 0; $i -lt 50; $i++) {
    if ($process.HasExited) { throw "recommendation service exited; see $errLog" }
    try {
        $health = Invoke-RestMethod "http://127.0.0.1:$Port/health" -TimeoutSec 2
        Write-Host "[OK] recommendation service PID=$($process.Id), modelReady=$($health.modelReady), modelVersion=$($health.modelVersion)"
        exit 0
    } catch { Start-Sleep -Milliseconds 200 }
}
throw "recommendation service health timeout; see $errLog"
