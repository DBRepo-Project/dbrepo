#!/bin/bash
declare -A services
services[1080]=upload
services[4000]=search
services[5000]=analyse
services[9093]=data
services[9099]=metadata
services[3305]=sidecar

# ensure target directories are present
echo "ensure target directory ./site are present"
mkdir -p ./site

# extract static site
echo "extract static site .docs/.swagger/dist.tar.gz"
for key in "${!services[@]}"; do
  mkdir -p ./site/${services[$key]}
  echo "extract static site ./swagger-ui.html -> ./site/${services[$key]}"
  cp .docs/.swagger/swagger-ui.html ./site/${services[$key]}/index.html
  cp .docs/.swagger/custom.css ./site/${services[$key]}/custom.css
  sed -i -e "s/__SERVICENAME__/${services[$key]^} Service/g" ./site/${services[$key]}/index.html
  cp ".docs/.swagger/api-${services[$key]}.yaml" "./site/${services[$key]}/api.yaml"
done