#!/bin/bash
bash /app/disable-tls.sh &
/opt/keycloak/bin/kc.sh start-dev --import-realm --metrics-enabled=true
