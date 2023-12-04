#!/bin/sh
/bin/sh ./create-buckets.sh &
/entrypoint.sh server -dir=/data -s3 -s3.port=9000 -s3.config=/app/s3_config.json -metricsPort=9091