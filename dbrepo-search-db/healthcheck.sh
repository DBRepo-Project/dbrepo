#!/bin/bash
INDICES=$(curl -sSL http://localhost:9200/_cat/indices | awk '{ if ($6) { print $3 } }')
for index in "user" "view" "database" "identifier" "concept" "column" "table" "unit"; do
  if [ ! $(echo $INDICES | grep $index) ]; then
    echo "[healtcheck.sh] Index $index does not exist"
    exit 1
  fi
done