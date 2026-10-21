# Northstar CRM - start everything (Windows)
# Starts PostgreSQL, Kafka, the Spring Boot API and the Angular UI, waits until they are healthy,
# runs a quick check, then opens the app in your browser.
#
#   start.cmd             start (rebuilds containers only if code changed)
#   start.cmd -NoBrowser  don't open the browser

param([switch]$NoBrowser)
# 'Continue', not 'Stop': in Windows PowerShell 5.1 'Stop' turns docker's normal stderr output into a crash.
# Every docker call below checks $LASTEXITCODE instead.
$ErrorActionPreference = 'Continue'
Set-Location (Split-Path $PSScriptRoot -Parent | Split-Path -Parent)   # repo root

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

Step "Checking Docker"
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    $dockerExe = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $dockerExe)) { throw "Docker Desktop is not installed. Run scripts\windows\install.cmd first." }
    Write-Host "Starting Docker Desktop ..."
    Start-Process $dockerExe | Out-Null
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep 3; docker info *> $null; if ($LASTEXITCODE -eq 0) { break } }
    if ($LASTEXITCODE -ne 0) { throw "Docker Desktop did not start. Open it manually and try again." }
}

if (-not (Test-Path .env)) { Copy-Item .env.example .env; Write-Host "Created .env from .env.example." }
$envFile = @{}
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2; $envFile[$k.Trim()] = $v.Trim()
}

Step "Starting containers (first run builds them - a few minutes)"
docker compose up -d --build --wait --wait-timeout 300
if ($LASTEXITCODE -ne 0) {
    docker compose ps
    throw "Some containers did not become healthy. See logs with: docker compose logs backend"
}
docker compose ps

Step "Quick check"
function Status($url, $method = 'GET', $body = $null, $headers = @{}) {
    try {
        $r = Invoke-WebRequest -Uri $url -Method $method -Body $body -Headers $headers `
             -ContentType 'application/json' -UseBasicParsing -TimeoutSec 15
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode } else { return 0 }
    }
}
$ok = $true
function Check($name, $expected, $actual) {
    $pass = ($expected -eq $actual)
    if (-not $pass) { $script:ok = $false }
    $mark = if ($pass) { 'PASS' } else { 'FAIL' }
    Write-Host ("{0}  {1,-45} expected {2}, got {3}" -f $mark, $name, $expected, $actual) -ForegroundColor $(if ($pass) { 'Green' } else { 'Red' })
}
Check 'API health' 200 (Status 'http://localhost:8080/actuator/health')
Check 'Angular UI' 200 (Status 'http://localhost:4200/')
Check 'API without login is rejected' 401 (Status 'http://localhost:4200/api/customers')
$login = @{ username = 'agent'; password = $envFile['APP_DEMO_AGENT_PASSWORD'] } | ConvertTo-Json
try {
    $token = (Invoke-RestMethod -Uri 'http://localhost:4200/api/auth/login' -Method POST -Body $login `
              -ContentType 'application/json' -TimeoutSec 15).token
} catch { $token = $null }
Check 'Agent login works' 'token' $(if ($token) { 'token' } else { 'none' })
if ($token) {
    Check 'Customers load after login' 200 (Status 'http://localhost:4200/api/customers' -headers @{ Authorization = "Bearer $token" })
}

Write-Host ""
if ($ok) { Write-Host "Northstar CRM is running." -ForegroundColor Green }
else { Write-Host "Running, but a check failed - see above." -ForegroundColor Yellow }
Write-Host "  App:      http://localhost:4200"
Write-Host "  Login:    agent / $($envFile['APP_DEMO_AGENT_PASSWORD'])   (from your .env file)"
Write-Host "  Stop:     scripts\windows\stop.cmd"

if (-not $NoBrowser) { Start-Process 'http://localhost:4200' }
