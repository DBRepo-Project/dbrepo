#!/bin/bash
echo "Starting registry check ..."

if [ -z $SUPPORTED_VERSIONS ]; then
  echo "[ERROR] Missing environment variable SUPPORTED_VERSIONS" > /dev/stderr
  exit 1
elif [ -z $MAINTAINED_SERVICES ]; then
  echo "[ERROR] Missing environment variable MAINTAINED_SERVICES" > /dev/stderr
  exit 1
elif [ -z $CI_REGISTRY2_URL ]; then
  echo "[ERROR] Missing environment variable CI_REGISTRY2_URL" > /dev/stderr
  exit 1
fi

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
