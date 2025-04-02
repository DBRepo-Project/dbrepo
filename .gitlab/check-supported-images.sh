#!/bin/bash
echo "Starting registry check ..."

VERSIONS=(${SUPPORTED_VERSIONS//,/ })
SERVICES=(${MAINTAINED_SERVICES//,/ })

for SERVICE in "${SERVICES[@]}"; do
  TAGS=$(regctl tag ls "${CI_REGISTRY2_URL}/${SERVICE}")
  for VERSION in "${VERSIONS[@]}"; do
    if [[ "$VERSION" == "$APP_VERSION" ]]; then
      continue
    fi
    if [[ ! "${TAGS[*]}" =~ $VERSION ]]; then
      >&2 echo "[ERROR] Failed to find image: ${CI_REGISTRY2_URL}/${SERVICE}:${VERSION}"
      exit 1
    fi
    echo "[DEBUG] Found image: ${CI_REGISTRY2_URL}/${SERVICE}:${VERSION}"
  done
  echo "[INFO] Finished successfully."
done
