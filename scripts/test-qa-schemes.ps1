# QA schemes 1 / 2 / 3 / 5 smoke test (server must be running)
# Usage: .\scripts\test-qa-schemes.ps1
$ErrorActionPreference = "Stop"

$Base = if ($env:RAGCHUNK_BASE) { $env:RAGCHUNK_BASE } else { "http://localhost:8080" }
$SampleFile = Join-Path $PSScriptRoot "sample.md"
$Schemes = @(1, 2, 3, 5)
$Question = [System.Text.Encoding]::UTF8.GetString([byte[]](0x52,0x61,0x67,0x43,0x68,0x75,0x6e,0x6b,0x20,0xe4,0xb8,0x80,0xe6,0x9c,0x9f,0xe7,0xa6,0xbb,0xe7,0xba,0xbf,0xe5,0xbb,0xba,0xe5,0xba,0x93,0xe6,0x9c,0x89,0xe5,0x87,0xa0,0xe6,0xad,0xa5,0xef,0xbc,0x9f))
$QuestionVague = [System.Text.Encoding]::UTF8.GetString([byte[]](0xe7,0xa6,0xbb,0xe7,0xba,0xbf,0xe5,0x85,0xa5,0xe5,0xba,0x93,0xe6,0xb5,0x81,0xe7,0xa8,0x8b,0xe5,0xa4,0xa7,0xe6,0xa6,0x82,0xe6,0x80,0x8e,0xe6,0xa0,0xb7,0xef,0xbc,0x9f))

function Wait-DocumentSuccess {
    param([string]$KbId, [string]$DocId, [int]$MaxWaitSec = 120)
    $deadline = (Get-Date).AddSeconds($MaxWaitSec)
    do {
        $uri = "$Base/api/v1/knowledge-bases/$KbId/documents/$DocId"
        $doc = Invoke-RestMethod -Uri $uri
        if ($doc.status -eq "SUCCESS") { return $doc }
        if ($doc.status -eq "FAILED") {
            throw "document ingest failed: $($doc.errorMessage)"
        }
        Write-Host "      poll: status=$($doc.status) stage=$($doc.processStage)"
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "timeout waiting for document SUCCESS"
}

Write-Host "=== QA scheme test ===" -ForegroundColor Cyan
Write-Host "Base: $Base"

try {
    $null = Invoke-RestMethod -Uri "$Base/api/v1/runtime" -TimeoutSec 5
} catch {
    throw "Server not up. Start: mvnw spring-boot:run with profile inmemory"
}

$runtime = Invoke-RestMethod -Uri "$Base/api/v1/runtime"
Write-Host "llmConfigured=$($runtime.llmConfigured) provider=$($runtime.llmProvider)"

$createBody = @{
    name      = "qa-scheme-" + (Get-Date -Format "HHmmss")
    chunking  = @{ aiMode = "never" }
    retrieval = @{ topK = 5; scoreThreshold = 0.1 }
    qa        = @{ scheme = 1; maxLlmCalls = 5; maxSearchRounds = 5; agentMaxIterations = 3 }
} | ConvertTo-Json -Depth 6

Write-Host "`n[setup] create knowledge base"
$kb = Invoke-RestMethod -Method Post -Uri "$Base/api/v1/knowledge-bases" `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($createBody))
$kbId = $kb.id
Write-Host "      kbId=$kbId"

Write-Host "`n[setup] upload document"
$uploadJson = curl.exe -s -X POST "$Base/api/v1/knowledge-bases/$kbId/documents?smartChunk=false" `
    -F "file=@$SampleFile"
$upload = $uploadJson | ConvertFrom-Json
$docId = $upload.docId
if (-not $docId) { $docId = $upload.documentIds[0] }
$doc = Wait-DocumentSuccess -KbId $kbId -DocId $docId
Write-Host "      docId=$docId chunkCount=$($doc.chunkCount)"

$results = @()
foreach ($scheme in $Schemes) {
    Write-Host "`n--- scheme $scheme ---" -ForegroundColor Yellow
    $q = if ($scheme -eq 2) { $QuestionVague } else { $Question }
    $chatJson = (@{ question = $q; qaScheme = $scheme } | ConvertTo-Json -Compress)
    try {
        $chat = Invoke-RestMethod -Method Post `
            -Uri "$Base/api/v1/knowledge-bases/$kbId/chat" `
            -ContentType "application/json; charset=utf-8" `
            -Body ([System.Text.Encoding]::UTF8.GetBytes($chatJson))
        $meta = $chat.meta
        $preview = $chat.answer
        if ($preview.Length -gt 100) { $preview = $preview.Substring(0, 100) + "..." }
        $results += [pscustomobject]@{
            scheme           = $scheme
            schemeName       = $meta.schemeName
            ok               = $true
            citations        = $chat.citations.Count
            llmCalls         = $meta.llmCalls
            searchRounds     = $meta.searchRounds
            rewriteTriggered = $meta.rewriteTriggered
            answerPreview    = $preview
        }
        Write-Host "      q=$q"
        Write-Host "      name=$($meta.schemeName) citations=$($chat.citations.Count) llm=$($meta.llmCalls) search=$($meta.searchRounds) rewrite=$($meta.rewriteTriggered)"
        Write-Host "      answer: $preview"
    } catch {
        $results += [pscustomobject]@{ scheme = $scheme; ok = $false; error = $_.Exception.Message }
        Write-Host "      FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n=== summary ===" -ForegroundColor Cyan
$results | Format-Table -AutoSize

if (@($results | Where-Object { -not $_.ok }).Count -gt 0) { exit 1 }
Write-Host "All schemes OK." -ForegroundColor Green
