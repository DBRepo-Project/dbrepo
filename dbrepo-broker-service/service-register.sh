#!/bin/bash

# $1 is used as the host name.

EUREKA_HOST="discovery-service"
EUREKA_PORT="9090"
EUREKA_URI="http://$EUREKA_HOST:$EUREKA_PORT"

SERVICE_NAME="$1"
SERVICE_PROTOCOL="http"
SERVICE_HOST="$1"
SECURE_PORT="${2:-9000}"
SERVICE_PORT="${3:-9000}"

SERVICE_URI="$SERVICE_PROTOCOL://$SERVICE_HOST:$SERVICE_PORT"
HOME_URI="$SERVICE_URI/realms/dbrepo"
HEALTH_URI="$SERVICE_URI/health"

# This is the URL shown in the "status" field in the
# instances section of the eureka dashboard.
#
# It's up to you to decide what the URL points to. Some
# information or status endpoint might be good.
STATUS_URI="$SERVICE_URI/health"

# This is the name displayed to the right of the status
# on the eureka dashbard. If the app (FAKE_SERVICE) is
# registered with more than one hostname, they will be
# displayed as a comma-separated list. This hostname
# is part of the heartbeat message.
#
# If you'll have more than one host per service,
# make sure they have different host names.
HOST_NAME="${1:-fake01}"

# Everyone of these parameters seem to be required. I don't know
# anything about secureVipAddress and vipAddress.
#
# dataCenterInfo must have a name of "MyOwn" or "Amazon".
#
# status can be UP, DOWN, STARTING, OUT_OF_SERVICE, UNKNOWN.
#   if the registration status is STARTING, then the service
#   will never be evicted. Also, simply sending a Heartbeat
#   does not change the status.
#
# The metadata fields can be any information you want associated
# with a service. I recommend keeping it short.
#

cat <<EOF > /tmp/json.json
{
  "instance": {
    "instanceId": "$SERVICE_NAME:$SERVICE_NAME:$SERVICE_PORT",
    "hostName": "$HOST_NAME",
    "app": "$SERVICE_NAME",
    "ipAddr": "$SERVICE_HOST",
    "status": "UP",
    "dataCenterInfo": {
      "@class": "com.netflix.appinfo.MyDataCenterInfo",
      "name": "MyOwn"
    },
    "healthCheckUrl": "$HEALTH_URI",
    "homePageUrl": "$HOME_URI",
    "leaseInfo": {
      "evictionDurationInSecs": 90
    },
    "metadata": {
      "zone": "default",
      "management.port": "15672"
    },
    "port": {
      "\$": "$SERVICE_PORT",
      "@enabled": "true"
    },
    "securePort": {
      "\$": "$SECURE_PORT",
      "@enabled": "false"
    },
    "vipAddress": "$SERVICE_HOST",
    "secureVipAddress": "$SERVICE_HOST",
    "statusPageUrl": "$STATUS_URI"
  }
}
EOF

curl --header "content-type: application/json" --data-binary @/tmp/json.json --silent $EUREKA_URI/eureka/apps/$SERVICE_NAME
