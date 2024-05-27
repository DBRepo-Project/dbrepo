#!/bin/bash
declare -A services
#services[1080]=upload
services[4000]=search
services[5000]=analyse
services[9093]=data
services[9099]=metadata
services[3305]=sidecar

rm -f ./tmp.yaml
mkdir -p ./site/swagger
touch ./tmp.yaml

# -> build paths: map
for key in "${!services[@]}"; do
  cat .docs/.swagger/api-${services[$key]}.yaml | yq .paths >> ./tmp.yaml
done

# -> merge with api.base.yaml into final api.yaml
yq ".paths *= load(\"tmp.yaml\")" .docs/.swagger/api.base.yaml > .docs/.swagger/api.yaml

