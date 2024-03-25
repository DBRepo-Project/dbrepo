#!/bin/bash

# needed for MariaDB Connector/C
apt update && apt install -y curl gcc libmariadb3 libmariadb-dev

python3 -m venv ./lib/python/venv
source ./lib/python/venv/bin/activate
PIPENV_PIPFILE=./lib/python/Pipfile pipenv install --dev