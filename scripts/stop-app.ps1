# Stop RagChunk / free port 8080
$ErrorActionPreference = "SilentlyContinue"
$port = if ($env:RAGCHUNK_PORT) { [int]$env:RAGCHUNK_PORT } else { 8080 }

Get-NetTCPConnection -LocalPort $port -State Listen | ForEach-Object {
    $procId = $_.OwningProcess
    Write-Host "Stopping PID $procId (port $port)"
    Stop-Process -Id $procId -Force
}

Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like '*RagChunkApplication*' } |
    ForEach-Object {
        Write-Host "Stopping RagChunk PID $($_.ProcessId)"
        Stop-Process -Id $_.ProcessId -Force
    }

Start-Sleep -Seconds 2
if (Get-NetTCPConnection -LocalPort $port -State Listen) {
    Write-Host "Port $port still in use" -ForegroundColor Yellow
} else {
    Write-Host "Port $port is free" -ForegroundColor Green
}
