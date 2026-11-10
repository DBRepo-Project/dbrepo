#!/usr/bin/env bash
set -euo pipefail
PYTHONPATH=./lib/python python3 ./dbrepo-broker-service/tests/test_integration_ldap.py
