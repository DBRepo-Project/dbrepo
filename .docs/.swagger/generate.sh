#!/bin/bash

# clean up
echo "clean up ./dist ./site"
rm -rf ./dist ./site

# ensure target directories are present
echo "ensure target directory ./site are present"
mkdir -p ./site

# extract static site
echo "extract static site .docs/.swagger/dist.tar.gz"
tar xzf .docs/.swagger/dist.tar.gz
for service in "analyse" "mirror" "data" "metadata" "upload"; do
  mkdir -p ./site/$service
  echo "extract static site ./dist -> ./site/$service"
  cp -r ./dist/* ./site/$service
  echo "placing .docs/.swagger/api-$service.yaml -> ./site/$service/api.yaml"
  cp ".docs/.swagger/api-$service.yaml" "./site/$service/api.yaml"
done