#!/bin/bash
cat ${CI_PIPYRC} | base64 -d > .pypirc
python -m twine upload --config-file .pypirc --verbose --repository pypi ./lib/python/dist/dbrepo-*
