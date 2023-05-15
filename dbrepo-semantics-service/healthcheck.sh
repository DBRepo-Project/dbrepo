#!/bin/bash
HTTP_CODE=$(curl --silent --output /dev/stderr --write-out "%{http_code}" 'http://0.0.0.0:5010/metrics')
if test $HTTP_CODE -ne 200; then
  exit 1
fi