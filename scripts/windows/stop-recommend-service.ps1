param([int]$Port = 8100)
$ErrorActionPreference = "Stop"
$pidFile = Join-Path ([IO.Path]::GetTempPath()) "south-stand-recommend-$Port.pid"
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Host "[OK] no recommendation PID file"; exit 0 }
$servicePid = [int](Get-Content -Raw -LiteralPath $pidFile)
$process = Get-CimInstance Win32_Process -Filter "ProcessId=$servicePid" -ErrorAction SilentlyContinue
if (-not $process) { Remove-Item -LiteralPath $pidFile -Force; Write-Host "[OK] stale PID file removed"; exit 0 }
if ($process.CommandLine -notmatch "uvicorn" -or $process.CommandLine -notmatch "app.main:app") {
    throw "PID $servicePid is not the T18 recommendation service; refusing to stop it"
}
Stop-Process -Id $servicePid -Force
Wait-Process -Id $servicePid -Timeout 10 -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $pidFile -Force
Write-Host "[OK] stopped recommendation service PID=$servicePid"
