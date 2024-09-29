#!/bin/bash
GITLAB_URL="https://gitlab.phaidra.org"
curl -fsSL \
  -X POST \
  -H "Content-Type: application/json" \
  -H "PRIVATE-TOKEN: ${CI_TOKEN}" \
  -d '{"name": "v${APP_VERSION}", "tag_name": "v${APP_VERSION}", "ref": "${CI_COMMIT_BRANCH}", "description": "Automated release from CI/CD"}' \
  ${GITLAB_URL}/api/v4/projects/450/releases
