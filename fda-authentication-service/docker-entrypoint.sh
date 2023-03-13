#!/bin/bash

(java -Dlog4j2.formatMsgNoLookups=true -jar ./authentication-service.jar) &

/opt/keycloak/bin/kc.sh start