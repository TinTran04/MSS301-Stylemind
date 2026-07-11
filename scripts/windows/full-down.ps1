[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
}

& docker version *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running. Start Docker Desktop, wait for it to become ready, then retry.'
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.full.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

Write-Host 'Stopping the StyleMind stack and removing orphan containers. Volumes are preserved.' -ForegroundColor Yellow
& docker compose -f $composeFile down --remove-orphans
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host 'StyleMind stack stopped. Named volumes were not removed.' -ForegroundColor Green
