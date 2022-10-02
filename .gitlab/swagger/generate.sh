#!/bin/bash

declare -A services
services[5010]=units
services[9091]=container
services[9092]=database
services[9093]=query
services[9094]=table
services[9097]=authentication
services[9096]=identifier

function retrieve () {
  echo "... retrieve api"
  if [[ "$2" == units ]]; then
    wget "http://localhost:$1/api-$2.json" -O "./.gitlab/swagger/api-$2.yaml" -q
  else
    wget "http://localhost:$1/v3/api-docs.yaml" -O "./.gitlab/swagger/api-$2.yaml" -q
  fi
}

function generate () {
  echo "... generate python api"
  java -jar ./.gitlab/swagger/swagger-codegen-cli.jar generate -i "./.gitlab/swagger/api-$1.yaml" -l python -o "./.gitlab/swagger/api-$1" > /dev/null
}

function remove () {
  echo "... removing old python api"
  rm -rf "./.gitlab/api_$1" || true
  rm -rf "./.demo/api_$1" || true
}

function copy () {
  echo "... copying python api"
  cp -r "./.gitlab/swagger/api-$1/swagger_client" "./.gitlab/api_$1"
  cp -r "./.gitlab/swagger/api-$1/swagger_client" "./.demo/api_$1"
}

function replace () {
  echo "... replacing swagger client package name and gateway"
  find "./.gitlab/api_$2" -type f -exec sed -i -e "s/swagger_client/api_$2/g" {} \;
  find "./.gitlab/api_$2" -type f -exec sed -i -e "s/self.host = .*/self.host = \"http:\/\/localhost:9095\"/g" {} \;
  find "./.demo/api_$2" -type f -exec sed -i -e "s/swagger_client/api_$2/g" {} \;
  find "./.demo/api_$2" -type f -exec sed -i -e "s/self.host = .*/self.host = \"http:\/\/localhost:9095\"/g" {} \;
}

for key in "${!services[@]}"; do
  echo "Generating ${services[$key]} API"
  retrieve "$key" "${services[$key]}"
  generate "${services[$key]}"
  remove "${services[$key]}"
  copy "${services[$key]}"
  replace "$key" "${services[$key]}"
done
