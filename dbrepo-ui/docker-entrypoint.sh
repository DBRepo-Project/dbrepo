#!/bin/sh
if [[ $REBUILD == "true" ]]; then
  echo "Re-build nuxt with new configuration ..."
  yarn build > /dev/null
fi
yarn start
