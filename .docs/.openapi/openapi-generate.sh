#!/bin/bash
declare -A services
services[4050]=analyse
services[4060]=search
services[4070]=dashboard
services[9093]=data
services[9099]=metadata

# requires https://github.com/mikefarah/yq/ -> v4.44.3

function retrieve () {
  if [[ "$2" == analyse ]] || [[ "$2" == search ]] || [[ "$2" == dashboard ]]; then
    echo "... retrieve json api from localhost:$1"
    curl -sSL "http://localhost:$1/api-$2.json" | yq -o=json - > "./.docs/.openapi/api-$2.yaml"
  else
    echo "... retrieve yaml api from localhost:$1"
    curl -sSL "http://localhost:$1/v3/api-docs.yaml" > "./.docs/.openapi/api-$2.yaml"
  fi
}

for key in "${!services[@]}"; do
  echo "Generating ${services[$key]} API"
  retrieve "$key" "${services[$key]}"
done
