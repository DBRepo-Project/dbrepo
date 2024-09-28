#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
PIPELINE_ID=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines?ref=${CI_COMMIT_BRANCH}" | jq '[.[]|select(.)][0].id')
echo "[INFO] pipeline id: ${PIPELINE_ID}"
STATUS_RAW=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/${PIPELINE_ID}" | jq '.detailed_status')
echo "[DEBUG] response: ${STATUS_RAW}"
anybadge --label pipeline --value "$(echo ${STATUS_RAW} | jq --raw-output .text)" failed=red passed=green canceled=darkgray > /tmp/pipeline.svg