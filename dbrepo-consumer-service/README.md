# Consumer Service

## Delivery Reliability

The consumer forwards broker tuples to the normal data-service insert endpoint. Deploy the broker retention policy
**before** starting this consumer version. See [the rollout and recovery procedure](../dbrepo-broker-service/CONSUMER-RELIABILITY.md)
for retry classification, parking-queue installation, verification and the at-least-once limitations.

The Docker-free regression tests are `DataServiceGatewayUnitTest` and `RabbitListenerUnitTest` in `amqp-service`.
The latter starts the actual Spring listener container with a mocked AMQP channel and checks ACK/NACK behavior.

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
in [`DatabaseServiceIntegrationTest.java`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/master/dbrepo-consumer-service/rest-service/src/test/java/at/tuwien/service/DatabaseServiceIntegrationTest.java):

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
