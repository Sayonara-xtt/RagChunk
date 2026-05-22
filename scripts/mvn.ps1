# 调用项目 Maven Wrapper（无需系统 PATH 中的 mvn）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
& (Join-Path $root "mvnw.cmd") @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
