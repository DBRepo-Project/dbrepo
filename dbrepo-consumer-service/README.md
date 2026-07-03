# Data Service

## Build

Before testing, it is recommended to (re-)build the `MapStruct` mappers in case they were modified using the `package`
target:

```shell
mvn clean package
```

## Test

Run all unit and integration tests and create an HTML+TXT coverage report located in the `report` module:

```bash
mvn -pl rest-service clean test verify
```

Or run only tests 
in [`DatabaseServiceIntegrationTest.java`](https://github.com/DBRepo-Project/dbrepo/blob/master/dbrepo-consumer-service/rest-service/src/test/java/at/tuwien/service/DatabaseServiceIntegrationTest.java):

```bash
mvn -pl rest-service -Dtest="DatabaseServiceIntegrationTest" clean test
```

## Run

Start the Metadata Database, Data Database, Broker Service before and then run the Data Service:

```bash
mvn -pl rest-service clean spring-boot:run -Dspring-boot.run.profiles=local
```

### Endpoints

#### Actuator

- Info: http://localhost/actuator/info
- Health: http://localhost/actuator/health
    - Readiness: http://localhost/actuator/health/readiness
    - Liveness: http://localhost/actuator/health/liveness
- Prometheus: http://localhost/actuator/prometheus

#### Swagger UI

- Swagger UI: http://localhost/swagger-ui/index.html

#### OpenAPI

- OpenAPI v3 as .yaml: http://localhost/v3/api-docs.yaml