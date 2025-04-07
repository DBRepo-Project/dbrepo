#!/bin/bash
cat << EOF > /app/.s3cfg
access_key = ${S3_ACCESS_KEY_ID}
secret_key = ${S3_SECRET_ACCESS_KEY}
# Setup endpoint
host_base = ${STORAGE_ENDPOINT}
host_bucket = ${S3_BUCKET}
use_https = False
# Enable S3 v4 signature APIs
signature_v2 = False
EOF
s3cmd --config=/app/.s3cfg ls | grep "s3://${S3_BUCKET}"
if s3cmd --config=/app/.s3cfg ls | grep -q "s3://${S3_BUCKET}"; then
  echo "Bucket s3://${S3_BUCKET} already exists, skip."
  exit 0
fi
s3cmd --config=/app/.s3cfg mb s3://${S3_BUCKET}