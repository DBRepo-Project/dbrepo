#!/bin/bash
while [ ! -f /opt/keycloak/tls_disabled ]; do
  cd /opt/keycloak/bin || exit 1
  ./kcadm.sh config credentials --server http://localhost:8080 --realm master --user "${KEYCLOAK_ADMIN}" --password "${KEYCLOAK_ADMIN_PASSWORD}"
  if [ "$?" -ne 0 ]; then
    echo "Keycloak not yet ready ..."
    echo "Wait 5s ..."
    sleep 5
  else
    ./kcadm.sh update realms/master -s sslRequired=NONE
    touch /opt/keycloak/tls_disabled
  fi
done