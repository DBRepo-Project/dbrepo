#!/bin/bash
# clean up
echo "clean up ./dist ./site"
rm -rf ./dist ./site

# ensure target directories are present
echo "ensure target directories ./dist ./site are present"
mkdir -p ./dist ./site

# extract static site
echo "extract static site ./dist.tar.gz"
tar xzf ./dist.tar.gz
for service in "analyse" "mirror" "data" "metadata" "upload"; do
  mkdir -p ./site/$service
  echo "extract static site ./dist -> ./site/$service"
  cp -r ./dist/* ./site/$service
  echo "placing ./api-$service.yaml -> ./site/$service/api.yaml"
  cp "./api-$service.yaml" "./site/$service/api.yaml"
done