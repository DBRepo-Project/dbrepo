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
    echo "{\"@timestamp\":\"$(date --rfc-3339='seconds')\", \"log.level\": \"$1\", \"message\": \"$2\"}" >> /var/log/app/service/storage/init.log
}

# create bucket
if s3cmd --config=/app/config/.s3cfg ls | grep -q "s3://${S3_BUCKET}"; then
  echo "[INFO] Bucket s3://${S3_BUCKET} already exists, skip"
  log "INFO" "Bucket s3://${S3_BUCKET} already exists, skip"
else
  if ! s3cmd --config=/app/config/.s3cfg mb s3://${S3_BUCKET}; then
    echo "[ERROR] Failed to create bucket s3://${S3_BUCKET}"
    log "ERROR" "Failed to create bucket s3://${S3_BUCKET}"
  fi
fi

# expire daily
s3cmd --config=/app/config/.s3cfg expire s3://${S3_BUCKET} --expiry-prefix "" --expiry-days 1