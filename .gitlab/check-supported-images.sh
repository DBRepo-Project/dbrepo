#!/bin/bash
echo "Starting registry check ..."

SUPPORTED_VERSIONS="1.7.3, 1.8.0"
MAINTAINED_SERVICES="analyse-service, auth-service-init, dashboard-service, dashboard-service-init, data-service, metadata-service, search-db, search-service, search-service-init, storage-service-init, ui"
CI_REGISTRY2_URL="registry.datalab.tuwien.ac.at/dbrepo"

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
  done
done
echo "[INFO] Finished successfully."
