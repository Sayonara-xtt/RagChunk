# Install pgvector extension for PostgreSQL 17 on Windows (prebuilt binaries)
# Run as Administrator. If "禁止运行脚本", use install-pgvector-windows.cmd or:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\install-pgvector-windows.ps1 ...
param(
    [string]$PgRoot = "",
    [string]$SuperUser = "postgres",
    [string]$SuperPassword = "123",
    [string]$DatabaseName = "ragchunk"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$releaseUrl = "https://github.com/andreiramani/pgvector_pgsql_windows/releases/download/0.8.2_17.6/vector.v0.8.2-pg17.zip"
$zipPath = Join-Path $root "target\vector-pg17.zip"
$extractDir = Join-Path $root "target\vector-pg17"

function Find-PgRoot {
    if ($PgRoot -and (Test-Path $PgRoot)) { return $PgRoot }
    if ($env:PGROOT -and (Test-Path $env:PGROOT)) { return $env:PGROOT }
    $psql = Get-Command psql -ErrorAction SilentlyContinue
    if ($psql) {
        $bin = Split-Path $psql.Source -Parent
        return (Split-Path $bin -Parent)
    }
    $candidates = @(
        "D:\AnZhuang\PostgreSQL17",
        "C:\Program Files\PostgreSQL\17",
        "D:\Program Files\PostgreSQL\17",
        "C:\PostgreSQL\17"
    )
    foreach ($c in $candidates) {
        if (Test-Path (Join-Path $c "bin\psql.exe")) { return $c }
    }
    throw "Cannot find PostgreSQL 17. Pass -PgRoot '<你的PG安装目录>' 或把 psql 加入 PATH"
}

Write-Host "=== Install pgvector for PostgreSQL 17 (Windows) ===" -ForegroundColor Cyan

$pg = Find-PgRoot
Write-Host "PostgreSQL root: $pg"

if (-not (Test-Path $zipPath)) {
    Write-Host "Downloading pgvector 0.8.2 for PG17 ..."
    New-Item -ItemType Directory -Force -Path (Split-Path $zipPath) | Out-Null
    Invoke-WebRequest -Uri $releaseUrl -OutFile $zipPath -UseBasicParsing
}
if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }
Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force

$dll = Get-ChildItem -Path $extractDir -Recurse -Filter "vector.dll" | Select-Object -First 1
$sqlFiles = Get-ChildItem -Path $extractDir -Recurse -Filter "vector*.sql"
$control = Get-ChildItem -Path $extractDir -Recurse -Filter "vector.control" | Select-Object -First 1

if (-not $dll) { throw "vector.dll not found in archive" }

$libDir = Join-Path $pg "lib"
$extDir = Join-Path $pg "share\extension"
New-Item -ItemType Directory -Force -Path $extDir | Out-Null

Write-Host "Copy vector.dll -> $libDir"
Copy-Item $dll.FullName (Join-Path $libDir "vector.dll") -Force
foreach ($f in $sqlFiles) {
    Write-Host "Copy $($f.Name) -> $extDir"
    Copy-Item $f.FullName (Join-Path $extDir $f.Name) -Force
}
if ($control) {
    Copy-Item $control.FullName (Join-Path $extDir "vector.control") -Force
}

$svc = Get-Service -Name "postgresql-x64-17" -ErrorAction SilentlyContinue
if ($svc) {
    Write-Host "Restart PostgreSQL service ..."
    Restart-Service "postgresql-x64-17"
    Start-Sleep -Seconds 3
}

& "$PSScriptRoot\init-local-postgres.ps1" -SuperPassword $SuperPassword -DatabaseName $DatabaseName

$env:PGPASSWORD = $SuperPassword
$psql = Join-Path $pg "bin\psql.exe"
& $psql -U $SuperUser -d $DatabaseName -c "CREATE EXTENSION IF NOT EXISTS vector;"
$ver = & $psql -U $SuperUser -d $DatabaseName -tAc "SELECT extversion FROM pg_extension WHERE extname='vector';"
Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue

Write-Host "pgvector version: $ver" -ForegroundColor Green
Write-Host @"

Done. Start app (default profile local):
  mvn spring-boot:run

Connection: localhost:5432/$DatabaseName  user=$SuperUser

"@ -ForegroundColor Green
