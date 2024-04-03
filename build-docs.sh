#!/bin/bash

OVERRIDES_MAIN_HTML=""
SCRIPTS_EXTRA_JS=""

function clean_cache {
  echo "Removing cache from directory ./site ./lib/python/docs/build"
  rm -rf ./site ./lib/python/docs/build
}

function generate_sphinx {
  BRANCH="release-$1"
  echo "==================================================="
  echo "Building LIB for version $1 on branch $BRANCH"
  echo "==================================================="
  git reset --hard && git checkout "$BRANCH"
  pip install -r ./requirements.txt > /dev/null
  mkdir -p ./final
  sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" ./lib/python/setup.py ./lib/python/pyproject.toml ./lib/python/docs/conf.py ./lib/python/docs/index.rst ./lib/python/docs/index.rst
  sphinx-apidoc -o ./lib/python/docs/source ./lib/python/dbrepo
  sphinx-build -M html ./lib/python/docs/ ./lib/python/docs/build/
  cp -r ./lib/python/docs/build/html "./final/$1/sphinx"
  clean_cache
}

function generate_docs {
  BRANCH="release-$1"
  echo "==================================================="
  echo "Building DOCS for version $1 on branch $BRANCH"
  echo "==================================================="
  git reset --hard && git checkout "$BRANCH"
  pip install -r ./requirements.txt > /dev/null
  mkdir -p ./final
  if [ "$1" = "latest" ]; then
    OVERRIDES_MAIN_HTML=$(cat .docs/overrides/main.html)
    sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" .docs/scripts/extra.js
    SCRIPTS_EXTRA_JS=$(cat .docs/scripts/extra.js)
  else
    echo $OVERRIDES_MAIN_HTML > .docs/overrides/main.html
    mkdir -p .docs/scripts
    echo $SCRIPTS_EXTRA_JS > .docs/scripts/extra.js
  fi
  find .docs/ -type f -exec sed -i -e "s/__APPVERSION__/$1/g" {} \;
  find .docs/ -type f -exec sed -i -e "s/__CHARTVERSION__/$1/g" {} \;
  mkdocs build > /dev/null && cp -r ./site "./final/$1"
  cp -r "./swagger/$1" "./final/$1/swagger"
  clean_cache
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
  clean_cache
}

# usage
if [ -z "$DOC_VERSIONS" ]; then
    echo "Variable DOC_VERSIONS not set"
    exit 1
fi
versions=(${DOC_VERSIONS//,/ })

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
generate_sphinx "latest"

# versions
for i in "${!versions[@]}"; do
  version="${versions[i]}"
  generate_api "$version"
  generate_docs "$version"
done


# finalization
echo "==================================================="
echo "Moving default version $APP_VERSION docs to /"
cp -r ./final/${APP_VERSION}/* ./final/
echo "==================================================="
