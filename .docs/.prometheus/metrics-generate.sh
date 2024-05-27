#!/bin/bash
mvn -f ./dbrepo-metadata-service/pom.xml install
mvn -f ./dbrepo-data-service/pom.xml -pl rest-service -Dtest="PrometheusEndpointMvcTest" test
mvn -f ./dbrepo-metadata-service/pom.xml -pl rest-service -Dtest="PrometheusEndpointMvcTest" test
