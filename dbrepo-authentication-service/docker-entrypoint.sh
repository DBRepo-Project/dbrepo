#!/bin/bash

/app/service-register.sh authentication-service 8443 8080
(while sleep 60; do bash /app/service-register.sh authentication-service 8443 8080; done) &

/opt/keycloak/bin/kc.sh start-dev --import-realm
