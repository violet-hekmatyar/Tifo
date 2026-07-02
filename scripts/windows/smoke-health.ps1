$ErrorActionPreference = "Stop"
Invoke-RestMethod -Uri "http://localhost:8080/api/public/health" -Method GET | ConvertTo-Json -Depth 8
