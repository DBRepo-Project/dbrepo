#!/bin/bash
HTTP_CODE=$(curl --silent --output /dev/stderr --write-out "%{http_code}" 'http://0.0.0.0:8080/realms/dbrepo')
if test $HTTP_CODE -ne 200; then
  exit 1
fi
if [ ! -f /disabled ]; then
  cd /opt/keycloak/bin || exit 1
  ./kcadm.sh config credentials --server http://localhost:8080 --realm master --user "${KEYCLOAK_ADMIN}" --password "${KEYCLOAK_ADMIN_PASSWORD}"
  ./kcadm.sh update realms/master -s sslRequired=NONE
  touch /disabled
  echo "Successfully disabled TLS/SSL for realm 'master' ..."
fi