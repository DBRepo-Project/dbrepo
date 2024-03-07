#!/bin/bash

INDEX_HTML=""
OVERRIDES_MAIN_HTML=""
SCRIPTS_EXTRA_JS=""

function generate_docs {
  BRANCH="release-$1"
  echo "==================================================="
  echo "Building DOCS for version $1 on branch $BRANCH"
  echo "==================================================="
  git reset --hard && git checkout "$BRANCH"
  pip install -r ./requirements.txt > /dev/null
  mkdir -p ./final
  find .docs/ -type f -exec sed -i -e "s/__APPVERSION__/$1/g" {} \;
  find .docs/ -type f -exec sed -i -e "s/__CHARTVERSION__/$1/g" {} \;
  if [ "$1" = "latest" ]; then
    INDEX_HTML=$(cat .docs/redirect.html)
    OVERRIDES_MAIN_HTML=$(cat .docs/overrides/main.html)
    SCRIPTS_EXTRA_JS=$(cat .docs/scripts/extra.js)
  else
    echo $OVERRIDES_MAIN_HTML > .docs/overrides/main.html
    mkdir -p .docs/scripts
    echo $SCRIPTS_EXTRA_JS > .docs/scripts/extra.js
  fi
  mkdocs build > /dev/null && cp -r ./site "./final/$1"
  cp -r "./swagger/$1" "./final/$1/swagger"
}

function generate_api {
  BRANCH="release-$1"
  echo "==================================================="
  echo "Building API for version $1 on branch $BRANCH"
  echo "==================================================="
  git reset --hard && git checkout "$BRANCH"
  bash .docs/.swagger/swagger-site.sh
  find ./site -type f -exec sed -i -e "s/__APPVERSION__/$1/g" {} \;
  mkdir -p "./swagger/$1"
  cp -r ./site/* "./swagger/$1/"
}

# usage
if [ -z "$VERSIONS" ]; then
    echo "Variable VERSIONS not set"
    exit 1
fi
versions=(${VERSIONS//,/ })

# usage
if [ -z "$APP_VERSION" ]; then
    echo "Variable APP_VERSION not set"
    exit 2
fi
echo "==================================================="
echo "APP_VERSION=$APP_VERSION"
echo "==================================================="

# ensure branches exist on machine
git fetch

generate_api "latest"
generate_docs "latest"

# versions
for i in "${!versions[@]}"; do
  version="${versions[i]}"
  generate_api "$version"
  generate_docs "$version"
done


# finalization
echo "==================================================="
echo "Adding index.html from branch release-latest"
echo $INDEX_HTML > .docs/redirect.html
sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" .docs/redirect.html
cp ./.docs/redirect.html ./final/index.html
echo "==================================================="
