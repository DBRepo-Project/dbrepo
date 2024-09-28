#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
# if we reached this script, all the tests have passed
anybadge --label pipeline --value "passed" failed=red passed=green canceled=darkgray > "./final/${APP_VERSION}/images/pipeline.svg"
PIPELINE_COVERAGE=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/latest?ref=${CI_COMMIT_BRANCH}" | jq --raw-output .coverage)
echo "[INFO] pipeline coverage: ${PIPELINE_COVERAGE}"
if [ "${PIPELINE_COVERAGE}" != "null" ]; then
  anybadge --label coverage --value "${PIPELINE_COVERAGE}%" 70=red 80=orange 90=yellow 95=green > "./final/${APP_VERSION}/images/coverage.svg"
else
  echo "[WARNING] Skipping badge generation, displaying default badge text: unknown"
fi
