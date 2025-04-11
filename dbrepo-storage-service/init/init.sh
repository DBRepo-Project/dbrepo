#!/bin/bash
cat << EOF > /app/.s3cfg
access_key = ${S3_ACCESS_KEY_ID}
secret_key = ${S3_SECRET_ACCESS_KEY}
# Setup endpoint
host_base = ${STORAGE_ENDPOINT}
host_bucket = ${STORAGE_ENDPOINT}
use_https = False
# Enable S3 v4 signature APIs
signature_v2 = False
EOF

# create bucket
if s3cmd --config=/app/.s3cfg ls | grep -q "s3://${S3_BUCKET}"; then
  echo "[INFO] Bucket s3://${S3_BUCKET} already exists, skip."
else
  if ! s3cmd --config=/app/.s3cfg mb s3://${S3_BUCKET}; then
    echo "[ERROR] Failed to create bucket s3://${S3_BUCKET}"
  fi
fi

# expire daily
s3cmd --config=/app/.s3cfg expire s3://${S3_BUCKET} --expiry-prefix "" --expiry-days 1