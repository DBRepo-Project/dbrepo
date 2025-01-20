#!/bin/bash
helm schema > /dev/null
if [ $? -ne 0 ]; then
	echo "!!! Please install the Helm values schema plugin first\n"
	echo "    https://github.com/losisin/helm-values-schema-json"
fi
OUTPUT=$(cat helm/dbrepo/values.yaml | grep "registry.datalab.tuwien.ac.at/.*" | grep -v "$APP_VERSION")
if [ $? -ne 1 ]; then
  echo "[ERROR] Some image version(s) differ from APP_VERSION=${APP_VERSION} in Helm Char: ${OUTPUT}"
  exit 1
fi