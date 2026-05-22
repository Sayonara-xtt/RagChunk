# Check if ollama Spring profile is active
$ErrorActionPreference = "Stop"
$Base = if ($env:RAGCHUNK_BASE) { $env:RAGCHUNK_BASE } else { "http://localhost:8080" }

Write-Host "=== Ollama Profile Check ===" -ForegroundColor Cyan

Write-Host ""
Write-Host "[1] Environment" -ForegroundColor Cyan
$spa = [Environment]::GetEnvironmentVariable("SPRING_PROFILES_ACTIVE")
if ($spa) {
    Write-Host "  SPRING_PROFILES_ACTIVE = $spa"
    if ($spa -match "ollama") { Write-Host "  OK: contains ollama" -ForegroundColor Green }
    else { Write-Host "  WARN: no ollama in env" -ForegroundColor Yellow }
} else {
    Write-Host "  SPRING_PROFILES_ACTIVE not set"
}

Write-Host ""
Write-Host "[2] Runtime API ($Base)" -ForegroundColor Cyan
try {
    $r = Invoke-RestMethod -Uri "$Base/api/v1/runtime" -TimeoutSec 5
    Write-Host "  activeProfiles: $($r.activeProfiles -join ', ')"
    Write-Host "  defaultProfiles: $($r.defaultProfiles -join ', ')"
    Write-Host "  ollamaProfileActive: $($r.ollamaProfileActive)"
    Write-Host "  llmProvider: $($r.llmProvider)"
    Write-Host "  llmBaseUrl: $($r.llmBaseUrl)"
    Write-Host "  chatModel: $($r.chatModel)"
    if ($r.ollamaProfileActive -and $r.llmProvider -eq "ollama") {
        Write-Host ""
        Write-Host "RESULT: Ollama is ACTIVE" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "RESULT: Ollama is NOT active" -ForegroundColor Yellow
        Write-Host "  Start with: .\mvnw.cmd spring-boot:run `"-Dspring-boot.run.profiles=local,ollama`""
    }
} catch {
    Write-Host "  ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Start the app first, then re-run this script."
}
