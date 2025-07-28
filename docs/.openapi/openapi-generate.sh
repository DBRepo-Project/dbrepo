#!/bin/bash
ENDPOINTS="search-service:8080,dashboard-service:8080,data-service:8080,metadata-service:8080"

cat <<EOF > ./docs/.openapi/api.base.yaml
components:
  securitySchemes:
    basicAuth:
      in: header
      scheme: basic
      type: http
    bearerAuth:
      bearerFormat: JWT
      in: header
      scheme: bearer
      type: http
externalDocs:
  description: Project Website
  url: https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/$DOC_VERSION/
info:
  contact:
    email: andreas.rauber@tuwien.ac.at
    name: Prof. Andreas Rauber
  description: |
    The merged REST API of DBRepo for users, developers and data stewards to be accessed publicly. Have a look at
    the [source code](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services) for non-public endpoints
    that are used between the services themselves.
  license:
    name: Apache 2.0
    url: https://www.apache.org/licenses/LICENSE-2.0
  title: REST API
  version: $APP_VERSION
openapi: 3.1.0
servers:
  - description: Test Instance
    url: https://test.dbrepo.tuwien.ac.at
  - description: Local Instance
    url: http://localhost
EOF

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
  touch "./docs/.openapi/api-$HOSTNAME.yaml"
  curl -sSL $URL | yq -o=json - > "./docs/.openapi/api-$HOSTNAME.yaml"
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

# https://www.npmjs.com/package/openapi-merge-cli
openapi-merge-cli --config ./docs/.openapi/openapi-merge.json