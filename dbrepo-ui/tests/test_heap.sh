#!/usr/bin/env bash
set -euo pipefail

calls=${CALLS:-1000}
concurrency=${CONCURRENCY:-10}
export ENDPOINT=${ENDPOINT:-http://localhost}
export LC_ALL=C
if [ "$#" -eq 0 ]; then
  set -- / /search
fi

node "$(dirname "$0")/smoke-routes.mjs" "$@"
for route in "$@"; do
  echo "Testing ${ENDPOINT%/}$route ($calls requests, concurrency $concurrency)"
  report=$(ab -l -n "$calls" -c "$concurrency" "${ENDPOINT%/}$route")
  echo "$report"
  awk -v expected="$calls" '
    /^Complete requests:/  { complete = $NF }
    /^Failed requests:/    { failed = $NF; saw_failed = 1 }
    /^Non-2xx responses:/  { non_success = $NF }
    /^Write errors:/       { writes = $NF }
    END { exit !(saw_failed && complete == expected && failed == 0 && non_success == 0 && writes == 0) }
  ' <<< "$report"
done
