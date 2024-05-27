#!/bin/bash
python3 -m venv ./lib/python/venv
source ./lib/python/venv/bin/activate
PIPENV_PIPFILE=./lib/python/Pipfile pipenv install --dev
sphinx-apidoc -o ./lib/python/docs/source ./lib/python/dbrepo
sphinx-build -M html ./lib/python/docs/ ./lib/python/docs/build/