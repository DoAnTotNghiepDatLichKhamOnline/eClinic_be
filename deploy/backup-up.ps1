# Starts the laptop backup (compose project "eclinicbackup") behind a Cloudflare Tunnel and prints its public URL.
#   powershell -File deploy\backup-up.ps1                       # start with the data already on the laptop
#   powershell -File deploy\backup-up.ps1 -Pull                 # first load a fresh dump from the VM
#   powershell -File deploy\backup-up.ps1 -Restore | -Dump <f>  # first load the newest dump in backups\ | the given one
# Tunnel: named when CF_TUNNEL_TOKEN is set (URL https://CF_TUNNEL_HOSTNAME), else a quick *.trycloudflare.com tunnel.
param(
    [string]$Dump,
    [switch]$Pull,
    [switch]$Restore,
    [string]$Project = 'eclinicbackup'
)

. "$PSScriptRoot\backup-lib.ps1"
# Cloudflare only accepts TLS 1.2+, which PowerShell 5.1 does not enable by default
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# docker logs writes cloudflared output to stderr; PowerShell 5.1 turns redirected stderr into errors under 'Stop'
function Get-ContainerLog([string]$name) {
    $ErrorActionPreference = 'Continue'
    (& docker logs $name 2>&1 | ForEach-Object { "$_" }) -join "`n"
}

# Actuator answers application/vnd.spring-boot.actuator.v3+json, which PowerShell 5.1 returns as bytes, not text
function Test-Health([string]$url) {
    try {
        $body = (Invoke-WebRequest -UseBasicParsing -TimeoutSec 10 "$url/actuator/health").Content
        if ($body -is [byte[]]) { $body = [Text.Encoding]::UTF8.GetString($body) }
        $body -match '"status":"UP"'
    } catch { $false }
}

# 1. Restore only when asked, so re-running after a crash keeps the data made on the laptop
if ($Pull -or $Dump -or $Restore) {
    $restoreArgs = @{ Project = $Project }
    if ($Pull) { $restoreArgs.Pull = $true }
    if ($Dump) { $restoreArgs.Dump = $Dump }
    & "$PSScriptRoot\backup-restore.ps1" @restoreArgs
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

# 2. Start the app, then the tunnel (a broken tunnel would otherwise make --wait hang or fail without saying why)
$tunnel = if (Get-Setting 'CF_TUNNEL_TOKEN') { 'named' } else { 'quick' }
$other = if ($tunnel -eq 'named') { 'quick' } else { 'named' }
$container = "eclinic-tunnel-$tunnel"
Clear-OtherProjects $Project
Write-Host "Starting project '$Project' with the $tunnel tunnel (the first build takes several minutes) ..."
Compose $Project @('up', '-d', '--build', '--wait', '--wait-timeout', '900') @()
Compose $Project @('rm', '-sf', "tunnel-$other") @($other)
Compose $Project @('up', '-d', '--no-deps', "tunnel-$tunnel") @($tunnel)

# 3. Find the public URL and check the API answers through it
$hostname = Get-Setting 'CF_TUNNEL_HOSTNAME'
$url = $null
$checks = 0
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    $checks++
    # A bad token makes cloudflared exit and restart-loop, so it shows as "restarting" or "exited"
    $state = docker inspect -f '{{.State.Status}}' $container
    if ($state -ne 'running') {
        Fail "Tunnel container $container is not running ($state). Check CF_TUNNEL_TOKEN; logs: docker logs $container"
    }
    if ($tunnel -eq 'named') {
        if (-not $hostname) { if ($checks -ge 4) { break } else { continue } }
        $candidate = "https://$hostname"
    } else {
        # After a restart the log holds old URLs too; the newest one is the live tunnel
        $found = [regex]::Matches((Get-ContainerLog $container), 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($found.Count -eq 0) { continue }
        $candidate = $found[$found.Count - 1].Value
    }
    if (Test-Health $candidate) { $url = $candidate; break }
}

if ($tunnel -eq 'named' -and -not $hostname) {
    Write-Host 'Named tunnel is running. Set CF_TUNNEL_HOSTNAME in .env to have its URL checked and printed.' -ForegroundColor Yellow
    exit 0
}
if (-not $url) { Fail "The API did not answer through the tunnel within 90s. Logs: docker logs $container" }
Write-Host "Backup ready: $url" -ForegroundColor Green
Write-Host 'Frontend vercel.json rewrite:'
Write-Host "  { `"source`": `"/api/:path*`", `"destination`": `"$url/api/:path*`" }"
