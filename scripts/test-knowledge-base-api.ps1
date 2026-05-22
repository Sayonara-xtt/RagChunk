# Test: create + list + get knowledge base APIs
$ErrorActionPreference = "Stop"
$Base = if ($env:RAGCHUNK_BASE) { $env:RAGCHUNK_BASE } else { "http://localhost:8080" }
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=== Knowledge Base API Test ===" -ForegroundColor Cyan
Write-Host "Base: $Base"

Write-Host "`n[1] POST /api/v1/knowledge-bases" -ForegroundColor Cyan
$createBody = @{
    name        = "kb-test-" + (Get-Date -Format "yyyyMMdd-HHmmss")
    description = "API test"
    chunking    = @{ aiMode = "never" }
    retrieval   = @{ topK = 3; scoreThreshold = 0.3 }
} | ConvertTo-Json -Depth 5

$kb = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/knowledge-bases" `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($createBody))

Write-Host "  id: $($kb.id)"
Write-Host "  name: $($kb.name)"
Write-Host "  status: $($kb.status)"
$kbId = $kb.id

Write-Host "`n[2] GET /api/v1/knowledge-bases" -ForegroundColor Cyan
$all = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/knowledge-bases"
Write-Host "  count: $($all.Count)"
foreach ($item in $all) {
    Write-Host "    $($item.id) | $($item.name)"
}

Write-Host "`n[3] GET /api/v1/knowledge-bases?id=$kbId" -ForegroundColor Cyan
$one = Invoke-RestMethod -Method Get -Uri "$Base/api/v1/knowledge-bases?id=$kbId"
Write-Host "  id: $($one.id)"
Write-Host "  name: $($one.name)"

if ($one.id -ne $kbId) { throw "get by id mismatch" }
if ($all.Count -lt 1) { throw "list is empty" }

Write-Host "`n=== PASSED ===" -ForegroundColor Green
