#!/bin/bash
DUCKDB_VERSION="${DUCKDB_VERSION:-1.2.2}"

mkdir -p ./dbrepo-data-service/lib
gzip -c ./dbrepo-data-service/rest-service/lib/v${DUCKDB_VERSION}/linux_amd64/httpfs.duckdb_extension > ./dbrepo-data-service/lib/httpfs.duckdb_extension.gz
gzip -c ./dbrepo-data-service/rest-service/lib/v${DUCKDB_VERSION}/linux_amd64/mysql_scanner.duckdb_extension > ./dbrepo-data-service/lib/mysql_scanner.duckdb_extension.gz
