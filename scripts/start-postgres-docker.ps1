# 方案 A：Docker 启动带 pgvector 的 PostgreSQL
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "检查 Docker..." -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host @"

Docker 未运行。请先：
  1. 启动 Docker Desktop，等待托盘图标显示 Running
  2. 再执行: .\scripts\start-postgres-docker.ps1

"@ -ForegroundColor Yellow
    exit 1
}

# 默认 5433，避免与本机 PostgreSQL(5432) 冲突
$port = if ($env:RAGCHUNK_DB_PORT) { $env:RAGCHUNK_DB_PORT } else { "5433" }
$listening = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listening -and $listening.OwningProcess -ne 0) {
    $proc = Get-Process -Id $listening.OwningProcess -ErrorAction SilentlyContinue
    if ($proc -and $proc.ProcessName -eq "postgres") {
        Write-Host "警告: 本机 PostgreSQL 已占用端口 $port (PID $($listening.OwningProcess))。" -ForegroundColor Yellow
        Write-Host "请先停止本机 PostgreSQL 服务，或设置: `$env:RAGCHUNK_DB_PORT='5433' 并修改 docker-compose 端口映射。" -ForegroundColor Yellow
    }
}

Push-Location $root
try {
    Write-Host "拉取并启动 pgvector/pgvector:pg16 ..." -ForegroundColor Cyan
    docker compose up -d
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host "等待数据库就绪..." -ForegroundColor Cyan
    $ok = $false
    for ($i = 0; $i -lt 30; $i++) {
        docker exec ragchunk-postgres pg_isready -U ragchunk -d ragchunk 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ok = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ok) {
        Write-Host "容器已启动但健康检查超时，请执行: docker logs ragchunk-postgres" -ForegroundColor Red
        exit 1
    }

    Write-Host "检查 pgvector 扩展..." -ForegroundColor Cyan
    $ext = docker exec ragchunk-postgres psql -U ragchunk -d ragchunk -tAc "SELECT extversion FROM pg_extension WHERE extname='vector';"
    if ($ext) {
        Write-Host "pgvector 已安装，版本: $ext" -ForegroundColor Green
    } else {
        Write-Host "pgvector 未启用，尝试 CREATE EXTENSION..." -ForegroundColor Yellow
        docker exec ragchunk-postgres psql -U ragchunk -d ragchunk -c "CREATE EXTENSION IF NOT EXISTS vector;"
    }

    Write-Host @"

PostgreSQL + pgvector 已就绪。

  主机: localhost
  端口: $port
  库名: ragchunk
  用户: ragchunk
  密码: ragchunk

下一步:
  mvn spring-boot:run
  或: .\scripts\test-db-connection.ps1

"@ -ForegroundColor Green
} finally {
    Pop-Location
}
