#!/bin/bash
anybadge --label pipeline --value "passed" failed=red passed=green canceled=darkgray > "./final/${DOC_VERSION}/images/pipeline.svg"
PIPELINE_COVERAGE=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/${GITLAB_PROJECT_ID}/pipelines/latest?ref=${CI_COMMIT_BRANCH}" | jq --raw-output .coverage)

echo "[INFO] pipeline coverage: ${PIPELINE_COVERAGE}"
if [ "${PIPELINE_COVERAGE}" != "null" ]; then
  anybadge --label coverage --value "${PIPELINE_COVERAGE}" coverage > "./final/${DOC_VERSION}/images/coverage.svg"
else
  echo "[WARNING] Skipping badge generation, displaying default badge text: unknown"
fi

curl -fsSL "${CI_SONAR_URL}/api/project_badges/measure?project=${CI_SONAR_PROJECT_KEY}&metric=sqale_rating&token=${CI_SONAR_BADGE_TOKEN}" > "./final/${DOC_VERSION}/images/maintainability.svg"
curl -fsSL "${CI_SONAR_URL}/api/project_badges/measure?project=${CI_SONAR_PROJECT_KEY}&metric=security_rating&token=${CI_SONAR_BADGE_TOKEN}" > "./final/${DOC_VERSION}/images/security.svg"

echo "[INFO] retrieved badges"