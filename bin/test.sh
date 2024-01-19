#!/bin/bash
mvn -f ./dbrepo-metadata-service/pom.xml clean install -DskipTests
# test java services
mvn -f ./dbrepo-metadata-service/pom.xml clean test verify
mvn -f ./dbrepo-data-service/pom.xml clean test verify
# test python services
bash ./dbrepo-analyse-service/test.sh
bash ./dbrepo-search-service/test.sh
# test ui
yarn --cwd ./dbrepo-ui install
yarn --cwd ./dbrepo-ui run test:unit
yarn --cwd ./dbrepo-ui run coverage