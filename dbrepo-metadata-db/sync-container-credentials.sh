#!/usr/bin/env bash

set -euo pipefail

readonly client=/opt/bitnami/mariadb/bin/mariadb
readonly container_id=6cfb3b8e-1792-4e46-871a-f3d103527203

to_hex() {
  printf '%s' "$1" | od -An -tx1 | tr -d ' \n'
}

MYSQL_PWD="$DATA_DB_PASSWORD" "$client" \
  --host=data-db \
  --port=3306 \
  --user=root \
  --execute='SELECT 1' >/dev/null

MYSQL_PWD="$READONLY_PASSWORD" "$client" \
  --host=data-db \
  --port=3306 \
  --user="$READONLY_USERNAME" \
  --execute='SELECT 1' >/dev/null

container_count="$(MYSQL_PWD="$METADATA_DB_PASSWORD" "$client" \
  --host=metadata-db \
  --port=3306 \
  --user=root \
  --database="$METADATA_DB" \
  --batch \
  --skip-column-names \
  --execute="SELECT COUNT(*) FROM mdb_containers WHERE id = '$container_id'")"

if [[ "$container_count" != "1" ]]; then
  printf 'Expected one default data container, found %s\n' "$container_count" >&2
  exit 1
fi

data_password_hex="$(to_hex "$DATA_DB_PASSWORD")"
readonly_username_hex="$(to_hex "$READONLY_USERNAME")"
readonly_password_hex="$(to_hex "$READONLY_PASSWORD")"

MYSQL_PWD="$METADATA_DB_PASSWORD" "$client" \
  --host=metadata-db \
  --port=3306 \
  --user=root \
  --database="$METADATA_DB" \
  --execute="UPDATE mdb_containers
    SET privileged_username = 'root',
        privileged_password = CONVERT(UNHEX('$data_password_hex') USING utf8mb4),
        readonly_username = CONVERT(UNHEX('$readonly_username_hex') USING utf8mb4),
        readonly_password = CONVERT(UNHEX('$readonly_password_hex') USING utf8mb4)
    WHERE id = '$container_id'"

printf 'Synchronized credentials for the default data container\n'
