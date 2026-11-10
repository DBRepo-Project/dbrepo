# Live-check contracts

Use the same client and runner on both targets. Record each backend image digest;
the version shown by the frontend does not identify the Metadata-Service build.
Keep unsupported operations visible in the report instead of counting them as
successful requests.

## Database batch lookup

`GET /api/v1/database/batch?id=UUID&id=UUID` is available in main builds containing
commit `e42d312e9` (PR #163). Earlier builds without that change do not support
this endpoint. A release version alone is insufficient for unreleased or mixed
deployments; verify the Metadata-Service source revision or image provenance.

The endpoint accepts 1 to 100 IDs, removes duplicates and returns the requested
databases that the caller may read. Missing and inaccessible databases are omitted.
The result contains full database metadata; its order follows the first occurrence
of each requested ID. Invalid UUIDs or an invalid raw number of IDs return HTTP 400.

Older builds can route the literal `batch` to `/database/{databaseId}` and reject
it as an invalid UUID with HTTP 400. This explains a comparison with HTTP 200 on
main without establishing a regression or an additional fix in that comparison.
For these older versions, use the supported individual database reads. A fallback
must not be reported as successful batch-endpoint coverage.

The live `database.batch` check must request its own synthetic database and assert
that the returned list contains that database. Do not accept an arbitrary HTTP 200
or silently switch to individual reads after an error. The server regression suite
also covers multiple IDs, duplicate IDs, missing IDs, current permissions and the
literal HTTP route.

## Identifier metadata updates

Metadata updates retain the identifier's existing DOI in both the default and
`doi` profiles. Omitting `doi` or sending JSON null leaves it unchanged. A supplied
DOI must equal the existing value; this endpoint cannot assign, replace or remove
DOIs. The body ID must match the URL and the identifier must remain in its database.

An existing local draft without a DOI remains local when its metadata is edited,
including with the `doi` profile enabled. No provider update is sent. DOI creation
and publication keep their separate workflows. Draft creation on a DOI-enabled
server can already contact DataCite, even without the runner's `--publish` flag.
Use a provider stub or an approved sandbox for those tests.

The DOI-profile persistence tests stub the provider transport and verify the exact
resource and request ID, local readback and rollback on a provider failure. They
must run alongside the default-profile tests; disabling DOI does not exercise the
provider update path.

## Table removal

The portable runner and its offline tests are in `.scripts/client-live-test.py`
and `.scripts/test-client-live-test.py`. Select `--table-deletion physical` for main
or `--table-deletion archive` for an integration build with commit `f39b2cbb9`.
The default is physical deletion. See the [runner guide](../../.scripts/client-live-test.md)
for required fixtures and historical-read assertions. The selected contract is
recorded, and a mismatch cannot silently change the expected outcome.

## AMQP authentication

The default broker authenticates ordinary publishers against the shared LDAP
service. Use the same LDAP-backed ordinary account for the REST fixture and AMQP
publishing. A Keycloak-only account or an OIDC bearer token is not automatically
an LDAP password. A dedicated internal service account remains available for the
Consumer; do not substitute that account for the ordinary publisher in acceptance
checks.

RabbitMQ 3.13 expands environment variables in `rabbitmq.conf` using `$(NAME)`.
The LDAP plugin separately substitutes `${username}` in user-DN patterns. The
broker's bind DN and password use the former syntax. Using `${NAME}` for those
credentials leaves literal strings in the effective broker configuration and
prevents a valid LDAP account from authenticating.

See [RabbitMQ configuration](https://www.rabbitmq.com/docs/3.13/configure#environment-variable-interpolation)
and [LDAP DN patterns](https://www.rabbitmq.com/docs/ldap#usernames-and-dns).
The disposable LDAP integration test uses the shipped configuration and checks
ordinary-account publication of a row and a dataframe, their queue readback and
rejection of invalid credentials. It creates its own network and containers and
removes only those resources. It does not change authorization policy or prove
that every database/table routing key is authorized. The full runner additionally
requires the real Consumer path and database row readback.

The Consumer has two separate connections: `BROKER_USERNAME`/`BROKER_PASSWORD`
read the queue, while `SYSTEM_USERNAME`/`SYSTEM_PASSWORD` authenticate its HTTP
writes to Data-Service. The normal row-write endpoint requires `insert-table-data`
and the applicable database write grant. An internal account carrying only the
`system` authority does not satisfy that endpoint's method permission. Broker
login can therefore succeed while no rows arrive. Diagnose that HTTP failure
separately from LDAP authentication.

For a disposable end-to-end fixture, the Consumer's HTTP identity may be the same
ordinary owner that has the write role and grant on its synthetic database. Record
this fixture setting explicitly. Keep the broker reader separate and do not change
a shared customer Consumer's identity from the live runner. The default worker's
permission mismatch still needs a separate application fix; changing LDAP bind
syntax does not fix it or establish production-wide ingestion readiness.
