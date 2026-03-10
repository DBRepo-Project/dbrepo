#!/bin/sh
echo "s3.bucket.create -name $S3_BUCKET" | weed shell -master=$STORAGE_ENDPOINT_MASTER