#!/bin/bash
declare -A services
services[5000]=analyse
services[9050]=mirror
services[9093]=data
services[9099]=metadata

function retrieve () {
  if [[ "$2" == analyse ]]; then
    echo "... retrieve json api from localhost:$1"
    wget "http://localhost:$1/api-$2.json" -O "./api-$2.yaml" -q
  else
    echo "... retrieve yaml api from localhost:$1"
    wget "http://localhost:$1/v3/api-docs.yaml" -O "./api-$2.yaml" -q
  fi
}

for key in "${!services[@]}"; do
  echo "Generating ${services[$key]} API"
  retrieve "$key" "${services[$key]}"
done