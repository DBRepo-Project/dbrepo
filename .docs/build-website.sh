#!/bin/bash

function generate_docs {
  echo "==================================================="
  echo "Building DOCS for version $1 on branch $2"
  echo "==================================================="
  git checkout "$2"
  pip install -r ./requirements.txt
  mkdir -p ./final
  if [ "$1" = "latest" ]; then
    sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" .docs/redirect.html
    cp ./.docs/redirect.html ./final/index.html
  fi
  find .docs/ -type f -exec sed -i -e "s/__APPVERSION__/$1/g" {} \;
  find .docs/ -type f -exec sed -i -e "s/__CHARTVERSION__/$1/g" {} \;
  mkdocs build && cp -r ./site "./final/$1"
  cp -r "./swagger/$1" "./final/$1/swagger"
}

function generate_api {
  echo "==================================================="
  echo "Building API for version $1 on branch $2"
  echo "==================================================="
  git checkout "$2"
  bash .docs/.swagger/swagger-site.sh
  find ./site -type f -exec sed -i -e "s/__APPVERSION__/$1/g" {} \;
  mkdir -p "./swagger/$1"
  cp -r ./site/* "./swagger/$1/"
}

# usage
if [ -z "$v1_TAGS" ]; then
    echo "Variable v1_TAGS not set"
    exit 1
fi
tags=(${v1_TAGS//,/ })

# usage
if [ -z "$APP_VERSION" ]; then
    echo "Variable APP_VERSION not set"
    exit 2
fi
echo "APP_VERSION=$APP_VERSION"
for i in "${!tags[@]}"; do
  version="${tags[i]}"
  echo " ~> $version"
done
echo " ~> latest"

# ensure branches exist on machine
git fetch

# tags
for i in "${!tags[@]}"; do
  version="${tags[i]}"
  generate_api "$version" "v$version"
  generate_docs "$version" "v$version"
done

# master
generate_api "latest" "master"
generate_docs "latest" "master"
