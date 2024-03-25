#!/bin/bash

# preset
VERSION="latest"
MIN_CPU=8
MIN_RAM=8
SKIP_CHECKS=${SKIP_CHECKS:-0}

# checks
if [[ $SKIP_CHECKS -eq 0 ]]; then
  echo "[✨] Startup check ..."
  docker info > /dev/null
  if [[ $? -ne 0 ]]; then
    echo "Docker is not installed (or accessible in bash) on your system:"
    echo ""
    echo "  - install docker from https://docs.docker.com/desktop/install/linux-install/"
    echo "  - make sure the docker executable is in \$PATH"
    exit 2
  else
    echo "$(docker --version) OK"
  fi
  CPU=$(cat /proc/cpuinfo | grep processor | wc -l)
  if [[ $CPU -lt $MIN_CPU ]]; then
    echo "You do not have enough vCPU resources available:"
    echo ""
    echo "  - we found ${CPU} vCPU cores instead of necessary ${MIN_CPU}"
    echo "  - if you believe this is a mistake, skip startup checks with the SKIP_CHECKS=1 flag"
    exit 3
  else
    echo "vCPU ${CPU} OK"
  fi
  RAM=$(free -g -t | awk 'NR==2 {print $7}')
  if [[ $RAM -lt $MIN_RAM ]]; then
    echo "You do not have enough RAM free resources:"
    echo ""
    echo "  - we found ${RAM}GB RAM (free) instead of necessary ${RAM}GB"
    echo "  - if you believe this is a mistake, skip startup checks with the SKIP_CHECKS=1 flag"
    exit 4
  else
    echo "RAM ${RAM}GB OK"
  fi
else
  echo "[✨] Skipping checks ..."
fi

# environment
echo "[🚀] Gathering environment ..."
mkdir -p ./dist
curl -sSL -o ./docker-compose.yml "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/docker-compose.prod.yml"
curl -sSL -o ./dist/2_setup-data.sql "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-metadata-db/2_setup-data.sql"
curl -sSL -o ./dist/rabbitmq.conf "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-broker-service/rabbitmq.conf"
curl -sSL -o ./dist/enabled_plugins "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-broker-service/enabled_plugins"
curl -sSL -o ./dist/cert.pem "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-broker-service/cert.pem"
curl -sSL -o ./dist/pubkey.pem "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-broker-service/pubkey.pem"
curl -sSL -o ./dist/definitions.json "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-broker-service/definitions.json"
curl -sSL -o ./dist/dbrepo.conf "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-gateway-service/dbrepo.conf"
curl -sSL -o ./dist/opensearch_dashboards.yml "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-search-db/opensearch_dashboards.yml"
curl -sSL -o ./dist/dbrepo.config.json "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-ui/dbrepo.config.json"
curl -sSL -o ./dist/s3_config.json "https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/release-${VERSION}/dbrepo-storage-service/s3_config.json"

echo "[📦] Pulling images for version ${VERSION} ..."
docker compose pull

MAX_MAP_COUNT=$(cat /proc/sys/vm/max_map_count)
if [ "$MAX_MAP_COUNT" -lt 262144 ]; then
  echo "[🚀] Preparing environment ..."
  sudo echo "vm.max_map_count=262144" >> /etc/sysctl.conf
  sudo sysctl -p
fi

echo "[✨] Starting DBRepo ..."
docker compose up -d

if [ $? -eq 0 ]; then
  echo "[🎉] Successfully started!"
  echo ""
  echo "You can now inspect the logs with:"
  echo ""
  echo "  docker compose logs -f"
  echo ""
  echo "Read about next steps online: https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/${VERSION}/deployment-docker-compose/#next-steps"
fi
