#!/bin/bash
PIPENV_PIPFILE=./dbrepo-search-service/Pipfile
source ./dbrepo-search-service/venv/bin/activate
pip install pipenv
pipenv install gunicorn && pipenv install --dev --system --deploy
cd ./dbrepo-search-service/ && coverage run -m pytest test/test_app.py test/test_jwt.py test/test_opensearch_client.py test/test_keycloak_client.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt
cat ./coverage.txt | grep -o 'TOTAL[^%]*%'