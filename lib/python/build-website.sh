#!/bin/bash
python3 -m venv ./lib/python/venv
source ./lib/python/venv/bin/activate
PIPENV_PIPFILE=./lib/python/Pipfile pipenv install --dev
sed -i -e "s/__APPVERSION__/${APP_VERSION}/g" ./lib/python/pyproject.toml ./lib/python/setup.py ./lib/python/README.md ./lib/python/docs/conf.py ./lib/python/docs/index.rst
sphinx-apidoc -o ./lib/python/docs/source ./lib/python/dbrepo
sphinx-build -M html ./lib/python/docs/ ./lib/python/docs/build/
cp -r ./lib/python/docs/build/html ./site/sphinx