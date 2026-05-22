# 测试 RagChunk 数据库连接（使用 application.yaml 默认配置）
param(
    [string]$DbHost = "localhost",
    [int]$Port = 5432,
    [string]$Database = "ragchunk",
    [string]$User = "ragchunk",
    [string]$Password = "ragchunk"
)

$ErrorActionPreference = "Stop"
if ($env:RAGCHUNK_DB_HOST) { $DbHost = $env:RAGCHUNK_DB_HOST }
if ($env:RAGCHUNK_DB_PORT) { $Port = [int]$env:RAGCHUNK_DB_PORT }
if ($env:RAGCHUNK_DB_NAME) { $Database = $env:RAGCHUNK_DB_NAME }
if ($env:RAGCHUNK_DB_USER) { $User = $env:RAGCHUNK_DB_USER }
if ($env:RAGCHUNK_DB_PASSWORD) { $Password = $env:RAGCHUNK_DB_PASSWORD }

$root = Split-Path -Parent $PSScriptRoot
$mvn = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-3.9.15-bin\4rlcemksed9vjmkvgss0jpc4po\apache-maven-3.9.15\bin\mvn.cmd"
if (-not (Test-Path $mvn)) { $mvn = "mvn" }

Write-Host "Testing JDBC: ${User}@${DbHost}:${Port}/${Database}" -ForegroundColor Cyan

$env:RAGCHUNK_DB_HOST = $DbHost
$env:RAGCHUNK_DB_PORT = "$Port"
$env:RAGCHUNK_DB_NAME = $Database
$env:RAGCHUNK_DB_USER = $User
$env:RAGCHUNK_DB_PASSWORD = $Password
$env:RUN_DB_TEST = "1"

Push-Location $root
try {
    & $mvn -q test -Dtest=DatabaseConnectionTest
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "数据库连接成功。" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "数据库连接失败 (exit $LASTEXITCODE)。" -ForegroundColor Red
        Write-Host @"

常见处理:
  1. 启动 Docker Desktop 后: docker compose up -d
  2. 或在本机 PostgreSQL 用超级用户执行: scripts/init-postgres.sql
  3. 或设置实际账号后重试:
       `$env:RAGCHUNK_DB_USER = 'postgres'
       `$env:RAGCHUNK_DB_PASSWORD = '你的密码'
       .\scripts\test-db-connection.ps1
"@ -ForegroundColor Yellow
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
