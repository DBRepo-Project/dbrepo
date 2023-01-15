#!/bin/bash
source ./fda-semantics-service/venv/bin/activate
cd ./fda-semantics-service/ && python -m unittest test/test_validate.py test/test_list.py test/test_app.py