#!/bin/bash
source ./fda-analyse-service/venv/bin/activate
cd ./fda-analyse-service/ && python -m unittest test/test_determine_dt.py test/test_determine_pk.py