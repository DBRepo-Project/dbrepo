#!/bin/bash
function log {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $1"
}

log "Sleep 15s to start S3 API"
sleep 15
log "Start polling"
until curl -sSL 127.0.0.1:9000
do
    log "S3 API not ready on port 9000, wait 5s ..."
    sleep 5
done
log "Ready"
echo "s3.bucket.create -name dbrepo-upload" | weed shell
log "Created bucket dbrepo-upload"
echo "s3.bucket.create -name dbrepo-download" | weed shell
log "Created bucket dbrepo-download"