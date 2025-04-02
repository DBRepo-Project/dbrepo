#!/bin/bash
echo "Starting registry housekeeping ..."

VERSIONS=(${SUPPORTED_VERSIONS//,/ })
SERVICES=(${MAINTAINED_SERVICES//,/ })

for SERVICE in "${SERVICES[@]}"; do
  TAGS=$(regctl tag ls "${CI_REGISTRY2_URL}/${SERVICE}")
  TAGS=(${TAGS//\n/ })
  for TAG in "${TAGS[@]}"; do
    if [[ ! "${VERSIONS[*]}" =~ $TAG ]]; then
      regctl tag rm ${CI_REGISTRY2_URL}/${SERVICE}:$TAG
      echo "[INFO] Deleted unsupported tag ${CI_REGISTRY2_URL}/${SERVICE}:$TAG"
    fi
  done
done
echo "[INFO] Finished successfully."
