#!/usr/bin/env bash
# Dumps the eClinic MySQL database before a deploy. .github/workflows/deploy.yml runs it on the VM
# after "git reset --hard <new>" and before "docker compose up", so the dump matches the running commit's schema.
#
#   bash deploy/vm-backup.sh <running-sha> <new-sha>
#
# Writes $BACKUP_DIR/eclinic-<UTC time>-<running7>-to-<new7>.sql.gz and keeps the newest $BACKUP_KEEP dumps.
# Exit 0 when MySQL is not running (first install: nothing to back up); non-zero when the dump fails.
# Env: MYSQL_CONTAINER (default eclinic-mysql), BACKUP_DIR (default ~/eclinic-backups), BACKUP_KEEP (default 10).
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "usage: $0 <running-sha> <new-sha>" >&2
  exit 2
fi

container="${MYSQL_CONTAINER:-eclinic-mysql}"
dir="${BACKUP_DIR:-$HOME/eclinic-backups}"
keep="${BACKUP_KEEP:-10}"

if [ "$(docker inspect -f '{{.State.Running}}' "$container" 2>/dev/null)" != "true" ]; then
  echo "Backup: container $container is not running, skipping the dump."
  exit 0
fi

# Dumps hold patient data: owner-only directory and files.
umask 077
mkdir -p "$dir"
chmod 700 "$dir"

final="$dir/eclinic-$(date -u +%Y%m%d-%H%M%S)-${1:0:7}-to-${2:0:7}.sql.gz"
partial="$final.partial"
trap 'rm -f "$partial"' EXIT

# Single quotes on purpose: the variables expand inside the container, so the root password never reaches the host.
# shellcheck disable=SC2016
docker exec "$container" sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysqldump -uroot --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  | gzip > "$partial"

gzip -t "$partial"
if ! gzip -dc "$partial" | tail -n 1 | grep -q -- '-- Dump completed'; then
  echo "Backup: $partial has no '-- Dump completed' line, the dump is incomplete." >&2
  exit 1
fi

mv "$partial" "$final"
echo "Backup: $final ($(du -h "$final" | cut -f1))"

# Rotation: keep the newest $keep dumps.
find "$dir" -maxdepth 1 -name 'eclinic-*.sql.gz' -printf '%T@ %p\n' \
  | sort -rn | tail -n +"$((keep + 1))" | cut -d' ' -f2- | xargs -r rm -f --
