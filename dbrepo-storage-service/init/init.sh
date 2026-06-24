#!/bin/bash
cat << EOF > /app/config/.s3cfg
access_key = ${S3_ACCESS_KEY_ID}
secret_key = ${S3_SECRET_ACCESS_KEY}
# Setup endpoint
host_base = ${STORAGE_ENDPOINT}
host_bucket = ${S3_BUCKET}
use_https = False
# Enable S3 v4 signature APIs
signature_v2 = False
EOF

function log() {
    echo "{\"@timestamp\":\"$(date -u +"%Y-%m-%dT%H:%M:%S+00:00")\", \"log.level\": \"$1\", \"message\": \"$2\"}" >> /var/log/app/service/storage/init.log
}

S3CMD_OPTS="--config=/app/config/.s3cfg"

function wait_for_storage () {
  local attempts=30
  local host="${STORAGE_ENDPOINT%%:*}"
  local port="${STORAGE_ENDPOINT##*:}"

  while [[ $attempts -gt 0 ]]; do
    if bash -c ">/dev/tcp/${host}/${port}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
    attempts=$((attempts - 1))
  done

  echo "[ERROR] Failed to connect to storage endpoint ${STORAGE_ENDPOINT}"
  log "ERROR" "Failed to connect to storage endpoint ${STORAGE_ENDPOINT}"
  return 1
}

wait_for_storage

# create bucket
if s3cmd $S3CMD_OPTS ls | grep -q "s3://${S3_BUCKET}"; then
  echo "[INFO] Bucket s3://${S3_BUCKET} already exists, skip"
  log "INFO" "Bucket s3://${S3_BUCKET} already exists, skip"
else
  if ! s3cmd $S3CMD_OPTS mb s3://${S3_BUCKET}; then
    echo "[ERROR] Failed to create bucket s3://${S3_BUCKET}"
    log "ERROR" "Failed to create bucket s3://${S3_BUCKET}"
  fi
fi

# expire daily
if ! s3cmd $S3CMD_OPTS expire s3://${S3_BUCKET} --expiry-prefix "" --expiry-days 1; then
  echo "[ERROR] Failed to configure expiry for bucket s3://${S3_BUCKET}"
  log "ERROR" "Failed to configure expiry for bucket s3://${S3_BUCKET}"
  exit 1
fi
