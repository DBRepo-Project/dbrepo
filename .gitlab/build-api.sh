#!/bin/bash

# usage
if [ "$#" -ne 1 ]; then
    echo "USAGE: ./build-api.sh git-branch [git-branch ...]"
    exit 1
fi
# usage
if [ -z "$APP_VERSION" ]; then
    echo "Variable APP_VERSION not set"
    exit 2
fi
echo "APP_VERSION=$APP_VERSION"

# ensure branches exist on machine
git fetch

for branch in "$@"; do
  git checkout "$branch"
  bash .docs/.swagger/swagger-site.sh
  find ./site -type f -exec sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" {} \;
  mkdir -p ./swagger/${APP_VERSION}
  cp -r ./site/* ./swagger/${APP_VERSION}/
done