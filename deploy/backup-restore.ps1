# Loads a VM mysqldump into the laptop backup's MySQL (compose project "eclinicbackup"); does not start the app services.
#   powershell -File deploy\backup-restore.ps1 -Pull                 # fresh dump from the VM (BACKUP_VM_HOST in .env)
#   powershell -File deploy\backup-restore.ps1 [-Dump <file.sql.gz>]  # given file, else the newest in backups\
param(
    [string]$Dump,
    [switch]$Pull,
    [string]$Project = 'eclinicbackup'
)

. "$PSScriptRoot\backup-lib.ps1"
# Commands run inside the containers use only single quotes: PowerShell 5.1 mangles '"' in native arguments
$dumpCmd = 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysqldump -uroot --single-transaction --routines --triggers $MYSQL_DATABASE'
# The healthcheck pings the socket, so it passes during first-boot init; the TCP port only opens on the real server
$loadCmd = 'i=0; until mysqladmin ping -h127.0.0.1 --silent >/dev/null 2>&1; do i=$((i+1)); [ $i -gt 120 ] && exit 1; sleep 1; done; set -eo pipefail; echo DROP DATABASE IF EXISTS $MYSQL_DATABASE\; CREATE DATABASE $MYSQL_DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci\; | MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot; gunzip -c /tmp/restore.sql.gz | MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot $MYSQL_DATABASE; rm -f /tmp/restore.sql.gz'
$queryCmd = 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot -N -B $MYSQL_DATABASE'

# 1. Pick the dump
if ($Pull) {
    $envVals = Read-DotEnv
    $vmHost = $envVals['BACKUP_VM_HOST']
    if (-not $vmHost) { Fail 'Set BACKUP_VM_HOST in .env to use -Pull.' }
    $key = $envVals['BACKUP_SSH_KEY']
    if (-not $key) { $key = '~/.ssh/eclinic_gcp' }
    $key = $key -replace '^~', $HOME
    if (-not (Test-Path $key)) { Fail "SSH key not found: $key" }
    New-Item -ItemType Directory -Force $backups | Out-Null
    $Dump = Join-Path $backups ('eclinic-vm-{0:yyyyMMdd-HHmm}.sql.gz' -f (Get-Date))
    Write-Host "Dumping the VM database on $vmHost ..."
    Run 'ssh' @('-i', $key, "ubuntu@$vmHost", "docker exec eclinic-mysql sh -c '$dumpCmd' | gzip > /tmp/eclinic-backup.sql.gz")
    Run 'scp' @('-i', $key, "ubuntu@${vmHost}:/tmp/eclinic-backup.sql.gz", $Dump)
    Run 'ssh' @('-i', $key, "ubuntu@$vmHost", 'rm -f /tmp/eclinic-backup.sql.gz')
} elseif (-not $Dump) {
    $newest = Get-ChildItem (Join-Path $backups '*.sql.gz') -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $newest) { Fail "No dump given and none in $backups. Use -Pull or -Dump <file.sql.gz>." }
    $Dump = $newest.FullName
}
if (-not (Test-Path $Dump)) { Fail "Dump not found: $Dump" }
$Dump = (Resolve-Path $Dump).Path
Write-Host "Restoring $Dump into project '$Project'"

# 2. Free the fixed container names
Clear-OtherProjects $Project

# 3. Only MySQL runs while loading, so no service writes or migrates halfway
Compose $Project @('stop')
Compose $Project @('up', '-d', '--wait', 'mysql')
Run 'docker' @('cp', $Dump, 'eclinic-mysql:/tmp/restore.sql.gz')
Run 'docker' @('exec', 'eclinic-mysql', 'sh', '-c', $loadCmd)

# 4. The laptop must run the VM's commit, or Flyway refuses the restored history
$stats = 'SELECT (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()), (SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success = 1);' |
    docker exec -i eclinic-mysql sh -c $queryCmd
if ($LASTEXITCODE -ne 0) { Fail 'Restored, but could not read flyway_schema_history.' }
$tables, $dbVersion = "$stats".Trim() -split '\s+'
$codeVersion = (Get-ChildItem (Join-Path $root 'common/src/main/resources/db/migration') -Filter 'V*__*.sql' |
    ForEach-Object { [int]($_.Name -replace '^V(\d+)__.*', '$1') } | Measure-Object -Maximum).Maximum
Write-Host "Restored: $tables tables, Flyway version $dbVersion (code has V$codeVersion)" -ForegroundColor Green
if ("$dbVersion" -ne "$codeVersion") {
    Write-Host "Warning: the dump is at V$dbVersion but this checkout is at V$codeVersion. Check out the commit the VM runs before starting the app." -ForegroundColor Yellow
}
