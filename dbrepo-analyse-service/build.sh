#!/bin/bash

# needed for MariaDB Connector/C 
apt update && apt install -y curl gcc libmariadb3 libmariadb-dev

python3 -m venv ./dbrepo-analyse-service/venv
source ./dbrepo-analyse-service/venv/bin/activate
PIPENV_PIPFILE=./dbrepo-analyse-service/Pipfile pipenv install --dev