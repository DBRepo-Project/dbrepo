#!/bin/bash
declare -A services
services[4000]=search
services[5000]=analyse
services[9093]=data
services[9099]=metadata

# requires https://github.com/mikefarah/yq/ -> v4.44.3

function retrieve () {
  if [[ "$2" == analyse ]] || [[ "$2" == search ]]; then
    echo "... retrieve json api from localhost:$1"
    curl -sSL "http://localhost:$1/api-$2.json" | yq -p=json > "./.docs/.swagger/api-$2.yaml"
  else
    echo "... retrieve yaml api from localhost:$1"
    curl -sSL "http://localhost:$1/v3/api-docs.yaml" > "./.docs/.swagger/api-$2.yaml"
  fi
}

for key in "${!services[@]}"; do
  echo "Generating ${services[$key]} API"
  retrieve "$key" "${services[$key]}"
done
