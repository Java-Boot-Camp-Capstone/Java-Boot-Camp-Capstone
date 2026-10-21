@echo off
rem Runs start.ps1 (options are passed through, e.g. start.cmd -NoBrowser)
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
exit /b %ERRORLEVEL%
