#!/bin/bash
helm schema > /dev/null
if [ $? -ne 0 ]; then
	echo "!!! Please install the Helm values schema plugin first\n"
	echo "    https://github.com/losisin/helm-values-schema-json"
fi
