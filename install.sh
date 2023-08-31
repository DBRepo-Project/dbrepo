#!/bin/bash

if [ $USER != "root" ]; then
  echo "This script needs sudo privileges!"
  exit 1
fi

docker info > /dev/null
if [ $? -ne 0 ]; then
  echo "Docker is not installed (or accessible in bash) on your system:"
  echo ""
  echo "  - install docker from https://docs.docker.com/desktop/install/linux-install/"
  echo "  - make sure the docker executable is in \$PATH"
  exit 2
fi

echo "Gathering environment ..."
curl -sSL -o .env https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/.env.unix.example
curl -sSL -o docker-compose.yml https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/docker-compose.prod.yml
curl -sSL -o dbrepo.conf https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-gateway-service/dbrepo.conf
curl -sSL -o setup-schema_local.sql https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/dev/dbrepo-metadata-db/setup-schema_local.sql

echo "Pulling images ..."
docker compose pull

MAX_MAP_COUNT=$(cat /proc/sys/vm/max_map_count)
if [ "$MAX_MAP_COUNT" -lt 262144 ]; then
  echo "Preparing environment ..."
  echo "vm.max_map_count=262144" >> /etc/sysctl.conf
  sysctl -p
fi

echo "Starting DBRepo ..."
docker compose up -d

if [ $? -eq 0 ]; then
  echo "Successfully started. You can now inspect the logs with:"
  echo ""
  echo "docker compose logs -f"
fi
