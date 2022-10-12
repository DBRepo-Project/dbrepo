#!/bin/bash
python3 -m venv ./.gitlab/venv
cd ./.gitlab || exit 99
. ./venv/bin/activate

export PYTHONPATH="$(pwd):$PYTHONPATH"
export PYTHONUNBUFFERED=1
printenv | grep "PYTHON"

pip install -r ./requirements.txt
python -m pytest ./tests