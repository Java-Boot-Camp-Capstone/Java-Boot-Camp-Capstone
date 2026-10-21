@echo off
rem Runs stop.ps1 (options are passed through, e.g. stop.cmd -Reset). Never waits for a key press.
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0stop.ps1" %*
exit /b %ERRORLEVEL%
