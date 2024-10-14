#!/bin/bash
cd ./dbrepo-search-service
pip install pipenv
pipenv install --dev
coverage run -m pytest ./test/test_opensearch_client.py
echo "[INFO] Start testing ./init"
(cd ./init && pipenv install --dev)
coverage run -m pytest ./init/test/test_app.py