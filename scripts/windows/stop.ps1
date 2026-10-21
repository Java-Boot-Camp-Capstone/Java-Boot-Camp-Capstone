# Northstar CRM - stop everything (Windows)
#
#   stop.cmd          stop the containers (database data is kept for next time)
#   stop.cmd -Reset   stop AND delete the database data (fresh start next time)

param([switch]$Reset)
# 'Continue', not 'Stop': in Windows PowerShell 5.1 'Stop' turns docker's normal stderr output into a crash.
# Every docker call below checks $LASTEXITCODE instead.
$ErrorActionPreference = 'Continue'
Set-Location (Split-Path $PSScriptRoot -Parent | Split-Path -Parent)   # repo root

if ($Reset) {
    Write-Host "Stopping containers and deleting database data ..." -ForegroundColor Yellow
    docker compose down -v
} else {
    Write-Host "Stopping containers (data is kept) ..."
    docker compose down
}
if ($LASTEXITCODE -ne 0) { throw "docker compose down failed (is Docker Desktop running?)" }
Write-Host "Stopped." -ForegroundColor Green
