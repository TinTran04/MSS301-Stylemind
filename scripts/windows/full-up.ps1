[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Assert-DockerReady {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
    }

    & docker version *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Desktop is not running. Start Docker Desktop, wait for it to become ready, then retry.'
    }
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

Assert-DockerReady

Write-Host 'Starting the full StyleMind stack. Existing volumes will be preserved.' -ForegroundColor Cyan
& docker compose -f $composeFile --profile app up -d --build
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'StyleMind stack started. Use scripts\windows\logs.ps1 -Follow to inspect logs.' -ForegroundColor Green
