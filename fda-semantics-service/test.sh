#!/bin/bash
source ./fda-semantics-service/venv/bin/activate
cd ./fda-semantics-service/ && coverage run -m pytest test/test_validate.py test/test_list.py test/test_app.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt