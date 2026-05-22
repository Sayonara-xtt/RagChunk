@echo off
REM Run as Administrator. Bypasses PowerShell execution policy for this script only.
setlocal
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-pgvector-windows.ps1" %*
