# Shared helpers for backup-restore.ps1 and backup-up.ps1 (dot-sourced; ASCII only, PowerShell 5.1 reads it as ANSI).
# Never put '"' inside native arguments: PowerShell 5.1 strips it before docker/ssh see it.

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$backups = Join-Path $root 'backups'

function Fail([string]$msg) { Write-Host $msg -ForegroundColor Red; exit 1 }

function Run([string]$exe, [string[]]$argv) {
    & $exe @argv
    if ($LASTEXITCODE -ne 0) { Fail "Failed ($LASTEXITCODE): $exe $($argv -join ' ')" }
}

function Read-DotEnv {
    $vals = @{}
    $file = Join-Path $root '.env'
    if (Test-Path $file) {
        foreach ($line in Get-Content $file -Encoding UTF8) {
            if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { $vals[$Matches[1]] = $Matches[2].Trim('"', "'") }
        }
    }
    $vals
}

# Process env first, then .env (same order docker compose uses)
function Get-Setting([string]$name) {
    $val = [Environment]::GetEnvironmentVariable($name)
    if ($val) { return $val }
    (Read-DotEnv)[$name]
}

function Compose([string]$proj, [string[]]$argv, [string[]]$tunnels = @('quick', 'named')) {
    $base = @('compose', '-p', $proj, '-f', (Join-Path $root 'docker-compose.yml'), '-f', (Join-Path $root 'docker-compose.backup.yml'), '--profile', 'app')
    foreach ($t in $tunnels) { $base += @('--profile', $t) }
    Run 'docker' ($base + $argv)
}

# Container names are fixed, so another project (e.g. the dev stack) must give them up first; its volumes are kept
function Clear-OtherProjects([string]$proj) {
    $others = docker ps -a --filter 'name=^eclinic-' --format '{{.Labels}}' |
        ForEach-Object { if ($_ -match 'com\.docker\.compose\.project=([^,]+)') { $Matches[1] } } |
        Sort-Object -Unique | Where-Object { $_ -ne $proj }
    foreach ($p in $others) {
        Write-Host "Removing containers of project '$p' (volumes are kept) ..."
        Compose $p @('down')
    }
}
