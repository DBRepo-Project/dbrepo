#!/bin/bash
declare -A services
services[0]=analyse-service
services[1]=auth-service-init
services[2]=dashboard-service
services[3]=data-service
services[4]=metadata-service
services[5]=search-db
services[6]=search-service
services[7]=search-service-init
services[8]=storage-service-init
services[9]=ui

echo "Starting registry housekeeping ..."

for key in "${!services[@]}"; do
  echo "Checking ${CI_REGISTRY2_URL}/${services[$key]} tags ..."
  TAGS=$(regctl tag ls ${CI_REGISTRY2_URL}/${services[$key]})
  for tag in $TAGS; do
      res=$(echo "${SUPPORTED_VERSIONS}" | grep "$tag")
      if [[ -z $res ]]; then
        regctl tag rm ${CI_REGISTRY2_URL}/${services[$key]}:$tag
        echo "Deleted unsupported tag ${CI_REGISTRY2_URL}/${services[$key]}:$tag"
      fi
  done
done
