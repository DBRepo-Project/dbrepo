#!/bin/bash
function compare () {
  diff <(yq ".$1" docker-compose.yml) <(yq ".$1" .docker/docker-compose.yml)
}

compare "services.$1.restart"
compare "services.$1.container_name"
compare "services.$1.hostname"
compare "services.$1.environment"
compare "services.$1.healthcheck"
compare "services.$1.logging"

if [ -z "$IGNORE_IMAGE" ]; then
  compare "services.$1.image"
fi
if [ -z "$IGNORE_VOLUMES" ]; then
  compare "services.$1.volumes"
fi
if [ -z "$IGNORE_PORTS" ]; then
  compare "services.$1.ports"
fi
