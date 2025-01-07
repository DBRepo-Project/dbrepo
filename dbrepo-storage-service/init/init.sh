#!/bin/bash
cat << EOF > /app/config/.s3cfg
access_key = ${S3_ACCESS_KEY_ID}
secret_key = ${S3_SECRET_ACCESS_KEY}
# Setup endpoint
host_base = ${STORAGE_ENDPOINT}
host_bucket = ${STORAGE_ENDPOINT}
use_https = False
# Enable S3 v4 signature APIs
signature_v2 = False
EOF
s3cmd --config=/app/config/.s3cfg mb s3://${S3_BUCKET}