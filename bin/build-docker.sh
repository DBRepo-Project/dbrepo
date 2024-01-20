#!/bin/bash
docker build --network=host -t dbrepo-metadata-service:build --target build dbrepo-metadata-service
docker build --network=host -t dbrepo-data-service:build --target build dbrepo-data-service
docker compose build --parallel