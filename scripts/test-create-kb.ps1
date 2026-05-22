# 创建知识库接口测试
# 使用前请先启动: mvn spring-boot:run 或 IDEA 运行 RagChunkApplication

# 避免 PowerShell 控制台用 GBK 显示 UTF-8 导致「乱码」
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Base = if ($env:RAGCHUNK_BASE) { $env:RAGCHUNK_BASE } else { "http://localhost:8080" }
$BodyFile = Join-Path $PSScriptRoot "create-kb-body.json"

Write-Host "POST $Base/api/v1/knowledge-bases" -ForegroundColor Cyan
Write-Host "Body: $BodyFile`n"

try {
    $resp = Invoke-RestMethod -Method Post `
        -Uri "$Base/api/v1/knowledge-bases" `
        -ContentType "application/json; charset=utf-8" `
        -InFile $BodyFile
    $resp | ConvertTo-Json -Depth 8
    Write-Host "`n成功 kbId = $($resp.id)" -ForegroundColor Green
} catch {
    Write-Host "请求失败: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    Write-Host "`n请先启动应用 (端口 8080)" -ForegroundColor Yellow
    exit 1
}
