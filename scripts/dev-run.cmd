@echo off
REM 开发模式启动（本机 PG + 默认 local,ollama；nollama 仅 local）
REM 用法: scripts\dev-run.cmd
REM       scripts\dev-run.cmd ollama
cd /d "%~dp0.."
REM 默认 local+ollama；仅本机 PG 不用 Ollama 时: dev-run.cmd nollama
if /i "%~1"=="nollama" (
  set SPRING_PROFILES_ACTIVE=local
) else (
  set SPRING_PROFILES_ACTIVE=local,ollama
)
call mvnw.cmd spring-boot:run
