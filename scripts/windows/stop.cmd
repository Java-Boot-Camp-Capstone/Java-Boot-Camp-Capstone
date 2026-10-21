@echo off
rem Double-click me, or run from a terminal. Extra options are passed through (see stop.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
if "%~1"=="" pause
