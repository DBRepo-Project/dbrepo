#!/bin/bash
source ./lib/python/venv/bin/activate
cd ./lib/python/ && coverage run -m pytest tests/*.py --junitxml=report.xml && coverage html && coverage report > ./coverage.txt