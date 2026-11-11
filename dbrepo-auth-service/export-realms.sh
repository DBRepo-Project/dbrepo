#!/bin/bash
docker exec -it dbrepo-auth-service kc.sh export --dir /opt/bitnami/keycloak/export
docker exec -it dbrepo-auth-service cat /opt/bitnami/keycloak/export/master-realm.json > ./dbrepo-auth-service/master-realm.json
docker exec -it dbrepo-auth-service cat /opt/bitnami/keycloak/export/dbrepo-realm.json > ./dbrepo-auth-service/dbrepo-realm.json
python3 .scripts/sanitize-realm-keys.py ./dbrepo-auth-service/master-realm.json ./dbrepo-auth-service/dbrepo-realm.json
