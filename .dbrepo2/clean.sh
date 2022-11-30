#!/bin/bash
echo "=== [ Stopping dbrepo-* ] ==="
docker container stop $(docker container ls -aq -f name=^/dbrepo-.*) || true
echo "=== [ Removing dbrepo-* ] ==="
docker container rm $(docker container ls -aq -f name=^/dbrepo-.*) || true
docker volume rm $(docker volume ls -q -f name=^dbrepo-.*) || true
docker network rm $(docker network ls -q -f name=^dbrepo-.*) || true