#!/bin/bash
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.restart"
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.container_name"
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.hostname"
if [ -z "$IGNORE_IMAGE" ]; then
  yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.image"
fi
if [ -z "$IGNORE_VOLUMES" ]; then
  yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.volumes"
fi
if [ -z "$IGNORE_PORTS" ]; then
  yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.ports"
fi
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.environment"
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.healthcheck"
yq compare -P docker-compose.yml .docker/docker-compose.yml "services.$1.logging"