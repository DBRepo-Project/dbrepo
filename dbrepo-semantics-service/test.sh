#!/bin/bash
source ./dbrepo-semantics-service/venv/bin/activate
#cd ./dbrepo-semantics-service/ && coverage run -m pytest test/test_validate.py test/test_list.py test/test_app.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt
cd ./dbrepo-semantics-service/ && coverage run -m pytest test/test_validate.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt