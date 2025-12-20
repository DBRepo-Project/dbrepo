#!/bin/bash
python3 -m venv venv
source ./venv/bin/activate
rm -rf ./dbrepo-search-service/lib/* ./dbrepo-search-service/Pipfile.lock ./dbrepo-dashboard-service/lib/* ./dbrepo-dashboard-service/Pipfile.lock
PIPENV_PIPFILE=./lib/python/Pipfile pipenv lock
python3 -m build --sdist ./lib/python
python3 -m build --wheel ./lib/python
cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-search-service/lib
PIPENV_PIPFILE=./dbrepo-search-service/Pipfile pipenv lock
cp -r ./lib/python/dist/dbrepo-${APP_VERSION}* ./dbrepo-dashboard-service/lib
PIPENV_PIPFILE=./dbrepo-dashboard-service/Pipfile pipenv lock