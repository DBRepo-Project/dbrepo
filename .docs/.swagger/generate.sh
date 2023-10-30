#!/bin/bash

# clean up
echo "clean up .docs/.swagger/dist ./site"
rm -rf .docs/.swagger/dist ./site

# ensure target directories are present
echo "ensure target directories .docs/.swagger/dist ./site are present"
mkdir -p .docs/.swagger/dist ./site

# extract static site
echo "extract static site .docs/.swagger/dist.tar.gz"
tar xzf .docs/.swagger/dist.tar.gz
for service in "analyse" "mirror" "data" "metadata" "upload"; do
  mkdir -p ./site/$service
  echo "extract static site .docs/.swagger/dist -> ./site/$service"
  cp -r .docs/.swagger/dist/* ./site/$service
  echo "placing .docs/.swagger/api-$service.yaml -> ./site/$service/api.yaml"
  cp ".docs/.swagger/api-$service.yaml" "./site/$service/api.yaml"
done