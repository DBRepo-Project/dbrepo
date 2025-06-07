#!/bin/bash

echo "[DEBUG] Activate venv ..."
python3 -m venv ./lib/python/venv
source ./lib/python/venv/bin/activate
PIPENV_PIPFILE=./lib/python/Pipfile pipenv install --dev

echo "[DEBUG] Build api docs ..."
sphinx-apidoc -o ./lib/python/docs/source ./lib/python/dbrepo

echo "[DEBUG] Build html docs ..."
sphinx-build -M html ./lib/python/docs/ ./lib/python/docs/build/

echo "[DEBUG] Build markdown docs ..."
sphinx-build -M markdown ./lib/python/docs/ ./lib/python/docs/build/
cp ./docs/api/python.tpl ./docs/api/python.md

echo "[DEBUG] Parsing python.tpl docs ..."
cat ./lib/python/docs/build/markdown/guide/rest-client.md >> ./docs/api/python.md

echo "[DEBUG] Parsing python.tpl docs ..."
cat ./lib/python/docs/build/markdown/guide/amqp-client.md >> ./docs/api/python.md
