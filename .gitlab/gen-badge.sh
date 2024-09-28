#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
RAW=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/latest?ref=${CI_COMMIT_BRANCH}")
anybadge --label pipeline --value "$(echo ${RAW} | jq --raw-output .detailed_status.text)" failed=red passed=green canceled=darkgray > /tmp/pipeline.svg
anybadge --label pipeline --value "$(echo ${RAW} | jq --raw-output .coverage)" 70=red 80=orange 90=yellow 95=green > /tmp/coverage.svg