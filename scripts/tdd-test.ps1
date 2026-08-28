param(
    [ValidateSet("focus", "unit", "integration", "all")]
    [string]$Mode = "unit",

    [ValidateSet("RED", "GREEN", "REGRESSION")]
    [string]$Phase = "REGRESSION",

    [string]$Test,

    [switch]$WithPostgres
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenWrapper = Join-Path $projectRoot "mvnw.cmd"
$evidenceDirectory = Join-Path $projectRoot "docs\tdd\evidence"
$mavenUserDirectory = if ($env:MAVEN_USER_HOME) {
    $env:MAVEN_USER_HOME
} elseif ($env:USERPROFILE) {
    Join-Path $env:USERPROFILE ".m2"
} else {
    $null
}

if ($Mode -eq "focus" -and [string]::IsNullOrWhiteSpace($Test)) {
    throw "-Test is required when -Mode focus, for example RuleChunkerTest#splitsLongText"
}

New-Item -ItemType Directory -Path $evidenceDirectory -Force | Out-Null
$safeTestName = if ($Test) { $Test -replace '[^A-Za-z0-9_.#-]', '_' } else { "suite" }
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$evidenceFile = Join-Path $evidenceDirectory "$timestamp-$Phase-$Mode-$safeTestName.log"

$mavenArguments = switch ($Mode) {
    "focus" { @("-Dtest=$Test", "test") }
    "unit" { @("-Dgroups=unit", "test") }
    "integration" { @("-Dgroups=integration", "test") }
    "all" { @("test") }
}
if ($mavenUserDirectory) {
    $mavenRepository = Join-Path $mavenUserDirectory "repository"
    $mavenArguments = @("-Dmaven.repo.local=$mavenRepository") + $mavenArguments
}

$previousPgFlag = $env:RUN_PG_INTEGRATION
if ($WithPostgres) {
    $env:RUN_PG_INTEGRATION = "1"
}

Write-Host "TDD phase : $Phase"
Write-Host "Test mode : $Mode"
Write-Host "Evidence  : $evidenceFile"
Write-Host "Command   : .\mvnw.cmd $($mavenArguments -join ' ')"

$evidenceHeader = @(
    "TDD phase : $Phase"
    "Test mode : $Mode"
    "Command   : .\mvnw.cmd $($mavenArguments -join ' ')"
    "Started   : $(Get-Date -Format o)"
    ""
)
$evidenceHeader | Set-Content -Path $evidenceFile -Encoding utf8
try {
    & $mavenWrapper @mavenArguments 2>&1 | Tee-Object -FilePath $evidenceFile -Append
    $testExitCode = $LASTEXITCODE
} finally {
    if ($WithPostgres) {
        $env:RUN_PG_INTEGRATION = $previousPgFlag
    }
}

if ($Phase -eq "RED") {
    if ($testExitCode -eq 0) {
        Write-Error "RED validation failed: the focused test passed. Verify that the behavior is absent and the assertion is effective."
    }
    Write-Host "RED observed. Confirm the failure in: $evidenceFile" -ForegroundColor Yellow
    exit $testExitCode
}

if ($testExitCode -ne 0) {
    Write-Error "$Phase validation failed. See: $evidenceFile"
}

Write-Host "$Phase validation passed. Evidence: $evidenceFile" -ForegroundColor Green
