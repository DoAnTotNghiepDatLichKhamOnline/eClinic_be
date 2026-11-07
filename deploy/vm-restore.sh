#!/usr/bin/env bash
# Replaces the eClinic MySQL database with a dump made by deploy/vm-backup.sh. Used by .github/workflows/rollback.yml
# with the app services stopped. Drops and recreates the database first, so tables from newer migrations do not linger.
#
#   bash deploy/vm-restore.sh <dump.sql.gz>
#
# Env: MYSQL_CONTAINER (default eclinic-mysql). Database-level grants of MYSQL_USER survive the drop.
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "usage: $0 <dump.sql.gz>" >&2
  exit 2
fi

dump="$1"
container="${MYSQL_CONTAINER:-eclinic-mysql}"

# Check the dump before anything is dropped.
gzip -t "$dump"
if ! gzip -dc "$dump" | tail -n 1 | grep -q -- '-- Dump completed'; then
  echo "Restore: $dump has no '-- Dump completed' line, refusing to restore." >&2
  exit 1
fi

# Single quotes on purpose: the variables expand inside the container. New database gets the server defaults
# (utf8mb4 / utf8mb4_unicode_ci from the compose "command"), same as the original.
# shellcheck disable=SC2016
docker exec "$container" sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot -e "DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`; CREATE DATABASE \`$MYSQL_DATABASE\`;"'

# shellcheck disable=SC2016
gzip -dc "$dump" | docker exec -i "$container" sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD exec mysql -uroot "$MYSQL_DATABASE"'

echo "Restore: $dump loaded into $container."
