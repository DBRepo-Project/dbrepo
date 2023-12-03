#!/bin/bash
source ./dbrepo-analyse-service/venv/bin/activate
cd ./dbrepo-analyse-service/ && coverage run -m pytest test/test_determine_dt.py test/test_determine_pk.py test/test_s3_client.py --junitxml=report.xml && coverage html --omit="test/*" && coverage report --omit="test/*" > ./coverage.txt