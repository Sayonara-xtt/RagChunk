@echo off
REM Stop RagChunk and free port 8080 (bypasses PowerShell execution policy)
setlocal
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-app.ps1" %*
