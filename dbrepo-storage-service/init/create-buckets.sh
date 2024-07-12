#!/bin/bash
S3_BUCKET=${S3_BUCKET:-dbrepo}

function log {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $1"
}

log "SeaweedFS master is set to ${WEED_CLUSTER_SW_MASTER}"
log "Starting to create bucket ${S3_BUCKET}"
echo "s3.bucket.create -name ${S3_BUCKET}" | weed shell -master="${WEED_CLUSTER_SW_MASTER}"
log "Created bucket ${S3_BUCKET}"
