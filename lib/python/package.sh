#!/bin/bash
PRE_RELEASE=""
if [ "${CI_COMMIT_BRANCH:8:8}" = "master" ]; then
    PRE_RELEASE="rc${CI_PIPELINE_ID}"
fi
sed -i -e "s/__APPVERSION__/${APP_VERSION}${PRE_RELEASE}/g" ./lib/python/pyproject.toml ./lib/python/setup.py ./lib/python/README.md
python -m build --sdist ./lib/python
python -m build --wheel ./lib/python
