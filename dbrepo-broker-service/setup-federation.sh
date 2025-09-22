#!/usr/bin/env bash
set -euo pipefail

# Configuration via environment variables
BROKER_MGMT_URL="${BROKER_SERVICE_ENDPOINT:-http://broker-service:15672}"
BROKER_VHOST="${BROKER_VHOST:-/}"
BROKER_USER="${BROKER_USERNAME:-admin}"
BROKER_PASS="${BROKER_PASSWORD:-admin}"

# Replication exchange/queue on THIS site
REPL_EXCHANGE_NAME="${REPLICATION_EXCHANGE_NAME:-dbrepo-replication}"
REPL_QUEUE_NAME="${REPLICATION_QUEUE_NAME:-}"
REPL_SITE_ID="${REPLICATION_SITE_ID:-}"

# Federation upstream (publisher site) for THIS receiving site
FED_UPSTREAM_NAME="${FEDERATION_UPSTREAM_NAME:-upstream-remote}"
FED_UPSTREAM_URI="${FEDERATION_UPSTREAM_URI:-}"
FED_UPSTREAM_SET_NAME="${FEDERATION_UPSTREAM_SET_NAME:-}"

# Policy
FED_POLICY_NAME="${FEDERATION_POLICY_NAME:-federate-replication-exchange}"

curl_auth=(-u "${BROKER_USER}:${BROKER_PASS}")
curl_base=(--silent --show-error --fail)

echo "[federation-init] Waiting for broker management API at ${BROKER_MGMT_URL} ..."
for i in {1..60}; do
  if curl "${curl_auth[@]}" "${curl_base[@]}" "${BROKER_MGMT_URL}/api/overview" >/dev/null 2>&1; then
    echo "[federation-init] Broker management API is up"
    break
  fi
  sleep 2
done

# Create vhost if necessary
if [[ "${BROKER_VHOST}" != "/" ]]; then
  echo "[federation-init] Ensuring vhost '${BROKER_VHOST}' exists"
  curl "${curl_auth[@]}" "${curl_base[@]}" -X PUT "${BROKER_MGMT_URL}/api/vhosts/${BROKER_VHOST}"
fi

echo "[federation-init] Declaring replication exchange '${REPL_EXCHANGE_NAME}' (topic, durable)"
curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
  -X PUT "${BROKER_MGMT_URL}/api/exchanges/${BROKER_VHOST}/${REPL_EXCHANGE_NAME}" \
  --data '{"type":"topic","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

if [[ -n "${REPL_QUEUE_NAME}" ]]; then
  echo "[federation-init] Declaring replication queue '${REPL_QUEUE_NAME}' (durable)"
  curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
    -X PUT "${BROKER_MGMT_URL}/api/queues/${BROKER_VHOST}/${REPL_QUEUE_NAME}" \
    --data '{"durable":true,"auto_delete":false,"arguments":{}}'

  if [[ -n "${REPL_SITE_ID}" ]]; then
    BINDING_KEY="dbrepo.${REPL_SITE_ID}.*.*"
    echo "[federation-init] Binding queue '${REPL_QUEUE_NAME}' to exchange '${REPL_EXCHANGE_NAME}' with routing key '${BINDING_KEY}'"
    curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
      -X POST "${BROKER_MGMT_URL}/api/bindings/${BROKER_VHOST}/e/${REPL_EXCHANGE_NAME}/q/${REPL_QUEUE_NAME}" \
      --data "{\"routing_key\":\"${BINDING_KEY}\",\"arguments\":{}}"
  else
    echo "[federation-init] REPLICATION_SITE_ID not set, skipping local binding (expect external policy to bind)"
  fi
fi

if [[ -n "${FED_UPSTREAM_URI}" ]]; then
  echo "[federation-init] Creating federation upstream '${FED_UPSTREAM_NAME}' -> ${FED_UPSTREAM_URI}"
  curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
    -X PUT "${BROKER_MGMT_URL}/api/parameters/federation-upstream/${BROKER_VHOST}/${FED_UPSTREAM_NAME}" \
    --data "{\"value\":{\"uri\":\"${FED_UPSTREAM_URI}\",\"expires\":3600000}}"

  if [[ -n "${FED_UPSTREAM_SET_NAME}" ]]; then
    echo "[federation-init] Creating upstream set '${FED_UPSTREAM_SET_NAME}' including '${FED_UPSTREAM_NAME}'"
    curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
      -X PUT "${BROKER_MGMT_URL}/api/parameters/federation-upstream-set/${BROKER_VHOST}/${FED_UPSTREAM_SET_NAME}" \
      --data "{\"value\":{\"upstreams\":[\"${FED_UPSTREAM_NAME}\"]}}"

    echo "[federation-init] Applying policy '${FED_POLICY_NAME}' to federate exchange via upstream set"
    curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
      -X PUT "${BROKER_MGMT_URL}/api/policies/${BROKER_VHOST}/${FED_POLICY_NAME}" \
      --data "{\"pattern\":\"^${REPL_EXCHANGE_NAME}$\",\"apply-to\":\"exchanges\",\"definition\":{\"federation-upstream-set\":\"${FED_UPSTREAM_SET_NAME}\"},\"priority\":0}"
  else
    echo "[federation-init] Applying policy '${FED_POLICY_NAME}' to federate exchange via upstream '${FED_UPSTREAM_NAME}'"
    curl "${curl_auth[@]}" "${curl_base[@]}" -H 'content-type: application/json' \
      -X PUT "${BROKER_MGMT_URL}/api/policies/${BROKER_VHOST}/${FED_POLICY_NAME}" \
      --data "{\"pattern\":\"^${REPL_EXCHANGE_NAME}$\",\"apply-to\":\"exchanges\",\"definition\":{\"federation-upstream\":\"${FED_UPSTREAM_NAME}\"},\"priority\":0}"
  fi
else
  echo "[federation-init] FEDERATION_UPSTREAM_URI not set; skipping federation policy"
fi

echo "[federation-init] Done"


