#!/bin/bash
declare -A services
services[1080]=upload
services[4000]=search
services[5000]=analyse
services[9093]=data
services[9099]=metadata
services[3305]=sidecar

function version () {
  sed -i "s/dbrepo-latest/${TAG}/g" "./.docs/.swagger/api-$1.yaml"
}

for key in "${!services[@]}"; do
  echo "Version ${services[$key]} API"
  version "${services[$key]}"
done
