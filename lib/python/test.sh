#!/bin/bash
source ./lib/python/venv/bin/activate
cd ./lib/python/ && coverage run -m pytest tests/*.py --junitxml=report.xml && coverage html --omit="test/*" && coverage report --omit="test/*" > ./coverage.txt