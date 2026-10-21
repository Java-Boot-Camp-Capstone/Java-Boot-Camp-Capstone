# Northstar CRM - one-time setup (Windows)
#
# Everything in the architecture (Angular, Spring Boot, PostgreSQL, Kafka) runs in Docker containers,
# so the only things a machine needs are Docker Desktop and Git. This script:
#   1. installs Docker Desktop and Git with winget if they are missing
#   2. starts Docker Desktop
#   3. creates .env from .env.example (local, synthetic passwords - never committed)
#   4. downloads/builds all four containers so `start` is fast
#
# Optional:  install.cmd -DevTools   also installs JDK 21 + Node 22 for working on the code in IntelliJ.

param([switch]$DevTools)
# 'Continue', not 'Stop': in Windows PowerShell 5.1 'Stop' turns docker's normal stderr output into a crash.
# Every docker call below checks $LASTEXITCODE instead.
$ErrorActionPreference = 'Continue'
Set-Location (Split-Path $PSScriptRoot -Parent | Split-Path -Parent)   # repo root

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Has($cmd) { [bool](Get-Command $cmd -ErrorAction SilentlyContinue) }
function WingetInstall($id, $name) {
    if (-not (Has 'winget')) { throw "winget is not available. Install $name manually, then re-run this script." }
    Write-Host "Installing $name ..."
    winget install --id $id -e --accept-source-agreements --accept-package-agreements --silent
}

Step "Checking Git"
if (Has 'git') { Write-Host "Git found: $(git --version)" } else { WingetInstall 'Git.Git' 'Git' }

Step "Checking Docker Desktop"
$dockerExe = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
if (-not (Has 'docker') -and -not (Test-Path $dockerExe)) {
    WingetInstall 'Docker.DockerDesktop' 'Docker Desktop'
    Write-Host "`nDocker Desktop was installed. Windows may ask you to sign out/restart (WSL2 setup)." -ForegroundColor Yellow
    Write-Host "After restarting, open Docker Desktop once, accept the terms, then run install.cmd again." -ForegroundColor Yellow
    exit 0
}
Write-Host "Docker found."

if ($DevTools) {
    Step "Installing developer tools (JDK 21, Node 22)"
    if (-not (Has 'java')) { WingetInstall 'EclipseAdoptium.Temurin.21.JDK' 'JDK 21' } else { Write-Host "Java found." }
    if (-not (Has 'node')) { WingetInstall 'OpenJS.NodeJS.LTS' 'Node.js LTS' } else { Write-Host "Node found." }
}

Step "Starting Docker Desktop"
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Start-Process $dockerExe | Out-Null
    for ($i = 0; $i -lt 60; $i++) { Start-Sleep 3; docker info *> $null; if ($LASTEXITCODE -eq 0) { break } }
    if ($LASTEXITCODE -ne 0) { throw "Docker Desktop did not start. Open it manually, wait for 'Engine running', then re-run." }
}
Write-Host "Docker is running."

Step "Creating .env (local settings)"
if (Test-Path .env) { Write-Host ".env already exists - keeping it." }
else { Copy-Item .env.example .env; Write-Host "Created .env from .env.example." }

Step "Downloading and building containers (first time takes a few minutes)"
docker compose pull postgres kafka
if ($LASTEXITCODE -ne 0) { throw "docker compose pull failed" }
docker compose build
if ($LASTEXITCODE -ne 0) { throw "docker compose build failed" }

Write-Host "`nSetup complete. Next: run scripts\windows\start.cmd" -ForegroundColor Green
