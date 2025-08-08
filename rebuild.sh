#!/usr/bin/env bash
set -euo pipefail

services=(dbrepo-metadata-service dbrepo-data-service dbrepo-replication-service dbrepo-gateway-service)

stop_and_rm () {
  local name="$1"
  if docker container inspect "$name" >/dev/null 2>&1; then
    docker container stop "$name" 2>/dev/null || true
    docker container rm "$name"   2>/dev/null || true
    echo "Removed container: $name"
  else
    echo "Container $name does not exist, skipping."
  fi
}

for s in "${services[@]}"; do
  stop_and_rm "$s"
done

echo "Building services: ${services[*]}"
docker compose build "${services[@]}"

echo "Done."