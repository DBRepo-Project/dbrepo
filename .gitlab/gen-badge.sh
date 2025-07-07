#!/bin/bash
anybadge --label pipeline --value "passed" failed=red passed=green canceled=darkgray > "./final/${DOC_VERSION}/images/pipeline.svg"

URL="${GITLAB_URL}/api/v4/projects/${GITLAB_PROJECT_ID}/pipelines/latest?ref=master"
echo "[DEBUG] obtaining pipeline coverage from url: $URL ..."
PIPELINE_COVERAGE=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" $URL | jq --raw-output .coverage)
echo "[INFO] pipeline coverage: ${PIPELINE_COVERAGE}"

if [ "${PIPELINE_COVERAGE}" != "null" ]; then
  anybadge --label coverage --value "${PIPELINE_COVERAGE}" coverage > "./final/${DOC_VERSION}/images/coverage.svg"
else
  echo "[WARNING] Skipping badge generation, displaying default badge text: unknown"
fi

URL="https://registry.datalab.tuwien.ac.at/api/v2.0/projects/dbrepo/repositories/data-service"
echo "[DEBUG] obtain pull count from url: $URL"
PULL_COUNT=$(curl -fsSL "$URL" | jq .pull_count)
anybadge --label "docker pulls" --color "#007ec6" --value $PULL_COUNT > "./final/${DOC_VERSION}/images/pulls.svg"