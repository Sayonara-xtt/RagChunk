# Apply Flyway V1 schema to local PostgreSQL (when app cannot start yet)
param(
    [string]$PgBin = "D:\AnZhuang\PostgreSQL17\bin\psql.exe",
    [int]$Port = 5432,
    [string]$User = "postgres",
    [string]$Password = "123",
    [string]$DatabaseName = "ragchunk"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sql = Join-Path $root "src\main\resources\db\migration\V1__init_schema.sql"
if (-not (Test-Path $PgBin)) { throw "psql not found: $PgBin" }
if (-not (Test-Path $sql)) { throw "SQL not found: $sql" }

$env:PGPASSWORD = $Password
Get-Content $sql -Raw | & $PgBin -U $User -h localhost -p $Port -d $DatabaseName -v ON_ERROR_STOP=1
Write-Host "Schema applied. Tables:" -ForegroundColor Green
& $PgBin -U $User -h localhost -p $Port -d $DatabaseName -c "\dt"
