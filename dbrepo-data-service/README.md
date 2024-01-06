# Data Service

## Actuator

- Info: http://localhost:9093/actuator/info
- Health: http://localhost:9093/actuator/health
    - Readiness: http://localhost:9093/actuator/health/readiness
    - Liveness: http://localhost:9093/actuator/health/liveness
- Prometheus: http://localhost:9093/actuator/prometheus

## Swagger UI Endpoints

- Swagger UI: http://localhost:9093/swagger-ui/index.html

## OpenAPI Endpoints

- OpenAPI v3 as .yaml: http://localhost:9093/v3/api-docs.yaml

## Build

```shell
mvn -f ../dbrepo-metadata-service/pom.xml clean install -DskipTests
mvn clean package -DskipTests
```