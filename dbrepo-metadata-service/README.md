# Metadata Service

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
    - Readiness: http://localhost:9099/actuator/health/readiness
    - Liveness: http://localhost:9099/actuator/health/liveness
- Prometheus: http://localhost:9099/actuator/prometheus

#### OpenAPI

- OpenAPI v3 as .yaml: http://localhost:9099/v3/api-docs.yaml

#### Broker health

`GET /api/metadata/broker/health` requires the `system` authority. It uses the
existing authenticated `brokerRestTemplate` to call RabbitMQ management
`/api/health/checks/alarms`, with five-second connect and read timeouts and no
retries. The configured system account must have management access to that
check. No broker credentials or raw upstream errors are returned.

The endpoint returns HTTP 200 for a completed probe; inspect the body's `status`:

| Status | Meaning |
| --- | --- |
| `UP` | RabbitMQ returned HTTP 200 with `status: ok`. |
| `DOWN` | RabbitMQ returned HTTP 503 with `status: failed` (resource alarms). |
| `UNAVAILABLE` | Management access failed, timed out, was denied, or returned another server error. |
| `UNKNOWN` | The check is missing or its response cannot be interpreted. |

`http_status` is the observed upstream HTTP code when available, and
`duration_ms` measures the probe. Alarm health does not verify AMQP delivery,
consumer processing, queue retention, or exactly-once writes.

Deploy metadata first, verify this endpoint with the existing system account,
then deploy the replication monitor. Update the gateway configuration for
external access (both Compose and Helm routes are provided). No schema,
installer, broker restart, or data-service changes are required. The monitor
uses the metadata service directly and populates `health.broker` in
`GET /api/replication/status`: `DOWN` or `UNAVAILABLE` makes overall health
`DOWN`; `UNKNOWN` makes it at least `DEGRADED`. An older metadata deployment
returning 404 therefore cannot silently report healthy replication.
