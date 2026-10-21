@echo off
rem Runs install.ps1 (options are passed through, e.g. install.cmd -DevTools). Never waits for a key press.
powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0install.ps1" %*
exit /b %ERRORLEVEL%
