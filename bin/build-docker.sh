#!/bin/bash
docker build -t dbrepo-metadata-service:build --target build dbrepo-metadata-service
docker build -t dbrepo-data-service:build --target build dbrepo-data-service
docker compose build --parallel