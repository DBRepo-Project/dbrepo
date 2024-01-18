#!/bin/bash
declare -A services
services[1080]=upload
services[4000]=search
services[5000]=analyse
services[9093]=data
services[9099]=metadata
services[3305]=sidecar

# clean up
echo "clean up ./dist ./site"
rm -rf ./dist ./site

# ensure target directories are present
echo "ensure target directory ./site are present"
mkdir -p ./site

# extract static site
echo "extract static site .docs/.swagger/dist.tar.gz"
tar xzf .docs/.swagger/dist.tar.gz
for key in "${!services[@]}"; do
  mkdir -p ./site/${services[$key]}
  echo "extract static site ./dist -> ./site/${services[$key]}"
  cp -r ./dist/* ./site/${services[$key]}
  echo "placing .docs/.swagger/api-${services[$key]}.yaml -> ./site/${services[$key]}/api.yaml"
  cp ".docs/.swagger/api-${services[$key]}.yaml" "./site/${services[$key]}/api.yaml"
done