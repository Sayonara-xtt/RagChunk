# RagChunk 端到端测试脚本（需先 mvn spring-boot:run）
$ErrorActionPreference = "Stop"
$Base = if ($env:RAGCHUNK_BASE) { $env:RAGCHUNK_BASE } else { "http://localhost:8080" }
$SampleFile = Join-Path $PSScriptRoot "sample.md"

Write-Host "=== RagChunk E2E ===" -ForegroundColor Cyan
Write-Host "Base URL: $Base"

# 1. 创建知识库
$createBody = @{
    name      = "e2e-test-" + (Get-Date -Format "HHmmss")
    chunking  = @{ aiMode = "never" }
    retrieval = @{ topK = 3; scoreThreshold = 0.1 }
} | ConvertTo-Json -Depth 5

Write-Host "`n[1] 创建知识库..."
$kb = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/knowledge-bases" `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($createBody))
$kbId = $kb.id
Write-Host "    kbId = $kbId"

# 2. 上传文档
if (-not (Test-Path $SampleFile)) {
    throw "sample file not found: $SampleFile"
}
Write-Host "`n[2] 上传文档 $SampleFile ..."
$doc = Invoke-RestMethod -Method Post `
    -Uri "$Base/api/v1/knowledge-bases/$kbId/documents?smartChunk=false" `
    -Form @{ file = Get-Item $SampleFile }
Write-Host "    status = $($doc.status), chunkCount = $($doc.chunkCount), qualityScore = $($doc.qualityScore)"
if ($doc.status -ne "SUCCESS") {
    Write-Host "    error: $($doc.errorMessage)" -ForegroundColor Red
    exit 1
}

# 3. 问答
Write-Host "`n[3] 问答..."
$question = "RagChunk 一期离线建库有几步？"
$chatJson = (@{ question = $question } | ConvertTo-Json -Compress)
$chat = Invoke-RestMethod -Method Post `
    -Uri "$Base/api/v1/knowledge-bases/$kbId/chat" `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($chatJson))
Write-Host "    citations: $($chat.citations.Count)"
Write-Host "    answer:`n$($chat.answer)"

Write-Host "`n=== E2E 完成 ===" -ForegroundColor Green
