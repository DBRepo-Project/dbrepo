#!/bin/bash
echo "Starting registry housekeeping ..."

SUPPORTED_VERSIONS="1.7.3, 1.8.0"
MAINTAINED_SERVICES="analyse-service, auth-service-init, dashboard-service, dashboard-service-init, data-service, metadata-service, search-db, search-service, search-service-init, storage-service-init, ui"
VERSIONS=(${SUPPORTED_VERSIONS//,/ })
SERVICES=(${MAINTAINED_SERVICES//,/ })

for SERVICE in "${SERVICES[@]}"; do
  TAGS=$(regctl tag ls "registry.datalab.tuwien.ac.at/dbrepo/${SERVICE}")
  TAGS=(${TAGS//\n/ })
  for TAG in "${TAGS[@]}"; do
    if [[ ! "${VERSIONS[*]}" =~ $TAG ]]; then
      echo "===> ${TAG}"
    fi
  done
done
#for key in "${!services[@]}"; do
#  echo "Checking ${CI_REGISTRY2_URL}/${services[$key]} tags ..."
#  TAGS=$(regctl tag ls ${CI_REGISTRY2_URL}/${services[$key]})
#  for tag in $TAGS; do
#      res=$(echo "${SUPPORTED_VERSIONS}" | grep "$tag")
#      if [[ -z $res ]]; then
#        regctl tag rm ${CI_REGISTRY2_URL}/${services[$key]}:$tag
#        echo "Deleted unsupported tag ${CI_REGISTRY2_URL}/${services[$key]}:$tag"
#      fi
#  done
#done
