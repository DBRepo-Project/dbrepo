#!/bin/bash
OUTPUT=$(cat .docker/docker-compose.yml | grep "registry.datalab.tuwien.ac.at/.*" | grep -v "$APP_VERSION")
if [ $? -ne 1 ]; then
  echo "[ERROR] Some image version(s) differ from APP_VERSION=${APP_VERSION}: ${OUTPUT}"
  exit 1
fi