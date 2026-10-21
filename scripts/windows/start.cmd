@echo off
rem Double-click me, or run from a terminal. Extra options are passed through (see start.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
if "%~1"=="" pause
