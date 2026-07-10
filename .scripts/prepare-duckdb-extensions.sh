#!/bin/bash

set -euo pipefail

DUCKDB_VERSION="${DUCKDB_VERSION:-1.2.2}"
DUCKDB_PLATFORM="${DUCKDB_PLATFORM:-linux_amd64}"

SOURCE_DIR="./dbrepo-data-service/rest-service/lib/v${DUCKDB_VERSION}/${DUCKDB_PLATFORM}"
TARGET_DIR="./dbrepo-data-service/lib"

mkdir -p "${TARGET_DIR}"

gzip -c "${SOURCE_DIR}/httpfs.duckdb_extension" > "${TARGET_DIR}/httpfs.duckdb_extension.gz"
gzip -c "${SOURCE_DIR}/mysql_scanner.duckdb_extension" > "${TARGET_DIR}/mysql_scanner.duckdb_extension.gz"
