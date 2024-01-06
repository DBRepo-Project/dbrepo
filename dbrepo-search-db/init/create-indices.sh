#!/bin/bash
if [ ! -z "${CURL_EXTRA_ARGS}" ]; then
  echo "Executing cURL with extra args: ${CURL_EXTRA_ARGS}"
fi
until curl ${CURL_EXTRA_ARGS} -sSL -u "${OPENSEARCH_USERNAME}:${OPENSEARCH_PASSWORD}" -o /dev/null "${OPENSEARCH_HOST}/_cat/indices" 2>&1
do
  echo "Not yet ready, wait 5s ..."
  sleep 5
done
index="database"
STATUS=$(curl ${CURL_EXTRA_ARGS} -sSLI "${OPENSEARCH_HOST}/$index" -u "${OPENSEARCH_USERNAME}:${OPENSEARCH_PASSWORD}" 2>/dev/null | head -n 1 | cut -d$' ' -f2)
if [ "${STATUS}" == "200" ]; then
  echo "Index $index already present, skipping..."
  continue
fi
RES=$(curl ${CURL_EXTRA_ARGS} -sSL -X PUT "${OPENSEARCH_HOST}/$index" -u "${OPENSEARCH_USERNAME}:${OPENSEARCH_PASSWORD}" -H "Content-Type: application/json" --data "@$index.json")
ACK=$(echo "$RES" | jq .acknowledged)
if [ $ACK ]; then
  echo "Created $index index"
else
  echo "Failed to create $index index: $RES"
fi
