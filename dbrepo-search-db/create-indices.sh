#!/bin/bash
until curl -sSL -o /dev/null localhost:9200/_plugins/_security/api/account -H "Authorization: Basic YWRtaW46YWRtaW4=" 2>&1
do
  echo "[create-indices.sh] OpenSearch not yet ready"
  sleep 5
done
echo "[create-indices.sh] OpenSearch ready"
for index in "user" "view" "database" "identifier" "concept" "column" "table" "unit"; do
  RES=$(curl -sSL -X PUT "127.0.0.1:9200/$index" -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "Content-Type: application/json" --data "@$index.json")
  ACK=$(echo "$RES" | jq .acknowledged)
  if [ $ACK ]; then
    echo "[create-indices.sh] Created $index index"
  else
    echo "[create-indices.sh] Failed to create $index index: $RES"
  fi
done