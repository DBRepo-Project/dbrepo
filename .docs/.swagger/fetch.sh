#!/bin/bash
# This script is executed before pushing to the pipeline for the moment.
# @author: Martin Weise

declare -A services
services[5000]=analyse
services[9091]=container
services[9092]=database
services[9093]=query
services[9094]=table
services[9096]=identifier
services[9097]=semantics
services[9098]=user
services[9099]=metadata

function retrieve () {
  if [[ "$2" == analyse ]]; then
    echo "... retrieve json api from localhost:$1"
    wget "http://localhost:$1/api-$2.json" -O "./.docs/.swagger/api-$2.yaml" -q
  else
    echo "... retrieve yaml api from localhost:$1"
    wget "http://localhost:$1/v3/api-docs.yaml" -O "./.docs/.swagger/api-$2.yaml" -q
  fi
}

for key in "${!services[@]}"; do
  echo "Generating ${services[$key]} API"
  retrieve "$key" "${services[$key]}"
done