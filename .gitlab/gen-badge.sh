#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
# if we reached this script, all the tests have passed
anybadge --label pipeline --value "passed" failed=red passed=green canceled=darkgray > "./final/${DOC_VERSION}/images/pipeline.svg"
PIPELINE_COVERAGE=$(curl -fsSL -H "PRIVATE-TOKEN: ${CI_TOKEN}" "${GITLAB_URL}/api/v4/projects/450/pipelines/latest?ref=${CI_COMMIT_BRANCH}" | jq --raw-output .coverage)
echo "[INFO] pipeline coverage: ${PIPELINE_COVERAGE}"
if [ "${PIPELINE_COVERAGE}" != "null" ]; then
  anybadge --label coverage --value "${PIPELINE_COVERAGE}" coverage > "./final/${DOC_VERSION}/images/coverage.svg"
else
  echo "[WARNING] Skipping badge generation, displaying default badge text: unknown"
fi
curl "${CI_SONAR_URL}/api/project_badges/measure?project=fair-data-austria-db-repository_fda-services_a57fa043-ab99-4cdd-a721-162d9a916d77&metric=sqale_rating&token=${CI_SONAR_TOKEN}" > "./final/${DOC_VERSION}/images/maintainability.svg"
curl "${CI_SONAR_URL}/api/project_badges/measure?project=fair-data-austria-db-repository_fda-services_a57fa043-ab99-4cdd-a721-162d9a916d77&metric=security_rating&token=${CI_SONAR_TOKEN}" > "./final/${DOC_VERSION}/images/security.svg"
echo "[INFO] retrieved SonarQube badges"