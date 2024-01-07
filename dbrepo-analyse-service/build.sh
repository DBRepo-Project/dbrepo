#!/bin/bash
python3 -m venv ./dbrepo-analyse-service/venv
source ./dbrepo-analyse-service/venv/bin/activate
PIPENV_PIPFILE=./dbrepo-analyse-service/Pipfile pipenv install --dev