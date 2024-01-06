#!/bin/bash
function log {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $1"
}

log "SeaweedFS master is set to ${SEAWEEDFS_ENDPOINT}"
log "Starting to create buckets dbrepo-upload, dbrepo-download"
echo "s3.bucket.create -name dbrepo-upload" | weed shell -master="${SEAWEEDFS_ENDPOINT}"
log "Created bucket dbrepo-upload"
echo "s3.bucket.create -name dbrepo-download" | weed shell -master="${SEAWEEDFS_ENDPOINT}"
log "Created bucket dbrepo-download"