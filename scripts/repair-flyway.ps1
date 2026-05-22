# 修复 Flyway checksum 不一致（例如修改过 V2__table_comments.sql 后）
# 用法: .\scripts\repair-flyway.ps1
# 需本机 PostgreSQL 与 application-local 默认连接一致

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "Flyway repair (对齐 flyway_schema_history 与 classpath 迁移脚本)..." -ForegroundColor Cyan
& (Join-Path $root "mvnw.cmd") -q flyway:repair `
  "-Dflyway.url=jdbc:postgresql://${env:RAGCHUNK_DB_HOST:-localhost}:${env:RAGCHUNK_DB_PORT:-5432}/${env:RAGCHUNK_DB_NAME:-ragchunk}" `
  "-Dflyway.user=${env:RAGCHUNK_DB_USER:-postgres}" `
  "-Dflyway.password=${env:RAGCHUNK_DB_PASSWORD:-123}" `
  "-Dflyway.locations=classpath:db/migration"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "完成。可重新启动应用。" -ForegroundColor Green
