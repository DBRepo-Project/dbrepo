# Semantics Service

## Test

Run all unit and integration tests and create an HTML+TXT coverage report located in the `report` module:

```bash
mvn -pl rest-service clean test verify
```

Or run only unit tests 
in [`KeycloakGatewayUnitTest.java`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/master/dbrepo-metadata-service/rest-service/src/test/java/at/tuwien/gateway/BrokerServiceGatewayTest.java):

```bash
mvn -pl rest-service -Dtest="KeycloakGatewayUnitTest" clean test
```

## Run

Start the Metadata Database before and then run the Metadata Service:

```bash
mvn -pl rest-service clean spring-boot:run -Dspring-boot.run.profiles=local
```

### Endpoints

#### Actuator

- Info: http://localhost:9099/actuator/info
- Health: http://localhost:9099/actuator/health
- Prometheus: http://localhost:9099/actuator/prometheus

#### Swagger UI

- Swagger UI: http://localhost:9099/swagger-ui/index.html

#### OpenAPI

- OpenAPI v3 as .yaml: http://localhost:9099/v3/api-docs.yaml