#!/bin/bash
ENDPOINTS="analyse-service:8080,search-service:8080,dashboard-service:8080,data-service:8080,metadata-service:8080"

# requires https://github.com/mikefarah/yq/ -> v4.44.3

function ip () {
  HOSTNAME=$(hostname $1)
  IP=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" "dbrepo-$HOSTNAME")
  echo $IP
}

function retrieve () {
  IP=$(ip $1)
  if [[ $IP == "" ]]; then
    echo "FATAL: failed to find ip for endpoint: $1"
    exit 1
  fi
  URL=$(url $1)
  HOSTNAME=$(hostname $1)
  echo "... retrieve json api from URL: $URL"
  touch "./.docs/.openapi/api-$HOSTNAME.yaml"
  curl -sSL $URL | yq -o=json - > "./.docs/.openapi/api-$HOSTNAME.yaml"
}

function url () {
  HOSTNAME=$(hostname $1)
  PORT=$(port $1)
  if [[ "$HOSTNAME" == "analyse-service" ]] || [[ "$HOSTNAME" == "search-service" ]] || [[ "$HOSTNAME" == "dashboard-service" ]]; then
    echo "http://$IP:$PORT/api-docs.json"
  else
    echo "http://$IP:$PORT/v3/api-docs.yaml"
  fi
}

function hostname () {
  IN="$1"
  arrIN=(${IN//:/ })
  echo ${arrIN[0]}
}

function port () {
  IN="$1"
  arrIN=(${IN//:/ })
  echo ${arrIN[1]}
}

IFS=',' read -ra ARR <<< "$ENDPOINTS"
for ENDPOINT in "${ARR[@]}"; do
  echo "Request OpenAPI definition for endpoint: $ENDPOINT"
  retrieve $ENDPOINT
done