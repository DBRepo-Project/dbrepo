#!/bin/bash
source ./dbrepo-analyse-service/venv/bin/activate
cd ./dbrepo-analyse-service/ && coverage run -m pytest test/test_determine_dt.py test/test_determine_pk.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt