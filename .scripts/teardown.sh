#!/bin/bash
echo "=== [ Stopping dbrepo-* ] ==="
docker container stop $(docker container ls -aq -f name=^/dbrepo-.*) || true
echo "=== [ Removing dbrepo-* ] ==="
docker container rm $(docker container ls -aq -f name=^/dbrepo-.*) || true
docker volume rm $(docker volume ls -q -f name=^dbrepo-.*) || true
docker network rm $(docker network ls -q -f name=^dbrepo-.*) || true
echo "=== [ Stopping * ] ==="
docker container stop $(docker container ls -aq -f name=.*-service) || true
docker container stop ui ui-proxy metadata-db || true
echo "=== [ Removing * ] ==="
docker container rm $(docker container ls -aq -f name=.*-service) || true
docker container rm ui ui-proxy metadata-db || true
docker volume rm $(docker volume ls -q) || true
echo "=== [ Stopping fda-* ] ==="
docker container stop $(docker container ls -aq -f name=^/fda-.*) || true
echo "=== [ Removing fda-* ] ==="
docker container rm $(docker container ls -aq -f name=^/fda-.*) || true
docker volume rm $(docker volume ls -q -f name=^fda-.*) || true
docker network rm $(docker network ls -q -f name=^fda-.*) || true
echo "=== [ Stopping tuw-* ] ==="
docker container stop $(docker container ls -aq -f name=^/tuw-.*) || true
echo "=== [ Removing tuw-* ] ==="
docker container rm $(docker container ls -aq -f name=^/tuw-.*) || true
docker volume rm $(docker volume ls -q -f name=^tuw-.*) || true
docker network rm $(docker network ls -q -f name=^tuw-.*) || true