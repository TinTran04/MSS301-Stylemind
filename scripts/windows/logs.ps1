[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Service,

    [switch]$Follow
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker CLI was not found. Install Docker Desktop and reopen the terminal.'
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $repositoryRoot 'BE\docker-compose.full.yml'

if (-not (Test-Path -LiteralPath $composeFile)) {
    throw "Compose file was not found: $composeFile"
}

$arguments = @('compose', '-f', $composeFile, 'logs')
if ($Follow) {
    $arguments += '--follow'
}
if ($Service) {
    $arguments += $Service
}

& docker @arguments
exit $LASTEXITCODE
