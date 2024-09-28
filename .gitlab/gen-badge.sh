#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
PIPELINE_STATUS=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/latest?ref=${CI_COMMIT_BRANCH}" | jq --raw-output .detailed_status.text)
anybadge --label pipeline --value "${PIPELINE_STATUS}" failed=red passed=green canceled=darkgray > /tmp/pipeline.svg
PIPELINE_COVERAGE=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/latest?ref=${CI_COMMIT_BRANCH}" | jq --raw-output .coverage)
if [ "${PIPELINE_COVERAGE}" == "null" ]; then
  PIPELINE_COVERAGE="unknown"
fi
anybadge --label pipeline --value "${PIPELINE_COVERAGE}" unknown=darkgray 70=red 80=orange 90=yellow 95=green > /tmp/coverage.svg
