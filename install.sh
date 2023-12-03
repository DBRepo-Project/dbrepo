#!/bin/bash

# dependency
docker info > /dev/null
if [ $? -ne 0 ]; then
  echo "Docker is not installed (or accessible in bash) on your system:"
  echo ""
  echo "  - install docker from https://docs.docker.com/desktop/install/linux-install/"
  echo "  - make sure the docker executable is in \$PATH"
  exit 2
fi

# environment
echo "[🚀] Gathering environment ..."
mkdir -p ./dist
curl -sSL -o ./docker-compose.yml https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/docker-compose.prod.yml
curl -sSL -o ./dist/setup-schema_local.sql https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-metadata-db/setup-schema_local.sql
curl -sSL -o ./dist/rabbitmq.conf https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/rabbitmq.conf
curl -sSL -o ./dist/enabled_plugins https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/enabled_plugins
curl -sSL -o ./dist/cert.pem https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/cert.pem
curl -sSL -o ./dist/pubkey.pem https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/pubkey.pem
curl -sSL -o ./dist/definitions.json https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-broker-service/definitions.json
curl -sSL -o ./dist/dbrepo.conf https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-gateway-service/dbrepo.conf
curl -sSL -o ./dist/opensearch_dashboards.yml https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-search-db/opensearch_dashboards.yml
curl -sSL -o ./dist/dbrepo.config.json https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-ui/dbrepo.config.json
curl -sSL -o ./dist/s3_config.json https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-storage-service/s3_config.json

echo "[📦] Pulling images ..."
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
fi
