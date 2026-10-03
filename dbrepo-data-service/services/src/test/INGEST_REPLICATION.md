# Source CSV Ingest Replication (F18)

## Contract

- An import commits all rows, version history and per-input-row POST outbox
  events together. SQL, serialization and Spark iteration failures roll back
  the entire import. Outbox preparation precedes the transaction; no shared
  staging table or per-row commits are used.
- CSV columns are positional, with the known `replication_key` optionally
  omitted. Missing/null keys are generated independently. Native duplicate-key
  upserts support primary and alternate unique keys, retain existing replication
  identities and emit the returned stored row, including for no-op duplicates.
- Remote-origin tables/databases reject source imports. Legacy databases without
  an origin remain writable. Successful imports remove the upload; failures
  retain it.
- CSV BLOB fields are literal contents. Source tuple BLOB strings are S3 keys,
  resolved once; byte arrays bind directly and null remains SQL NULL. Receiver
  wire base64 must be decoded according to column type, never treated as an S3 key.
- DECIMAL uses the shared mapper's exact `BigDecimal` binding. Extraction keeps
  DECIMAL and text values exact, temporal values as native strings, and
  BIT/BINARY/VARBINARY/BLOB values as byte arrays. Wire decoding must preserve
  decimal precision and decode binary fields by type.
- UTC sessions and native temporal strings preserve TIMESTAMP(6), DATETIME(6),
  negative/multi-day TIME(6) and DATE values. Bootstrap export sets UTC on its
  own connection. UPDATE skips bindings for `IS NULL` keys; DELETE binds every
  key using the shared mapper's null-safe `<=> ?` predicates.

## Run

Set `JAVA_HOME` externally to a Java 21 installation and put Maven on `PATH`.
From the repository root, install the matching core artifact, then use the
normal data-service reactor:

```sh
mvn -f lib/java/dbrepo-core/pom.xml -DskipTests -Djacoco.skip=true -DargLine= install

export SPARK_LOCAL_IP=127.0.0.1
export INGEST_SQL_TEST_PORT=13366
export INGEST_SQL_TEST_PASSWORD='<isolated MariaDB root password>'
mvn -f dbrepo-data-service/pom.xml -pl :rest-service -am \
  -Dtest=IngestReplicationMariaDbIntegrationTest,TableServiceMariaDbImplUnitTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -DargLine= test
```

Use an isolated MariaDB server on localhost (verified with MariaDB 11.3.2).
The opt-in integration suite creates and destroys only `ingest_replication_test`;
do not use that schema for other data or run another copy concurrently. The
suite is skipped when `INGEST_SQL_TEST_PORT` is unset.

## Coverage and Limits

The integration suite uses real JDBC, system-versioned InnoDB tables, production
mapping and SQL outbox writes. Local Spark runs with two workers. A local-file
CSV adapter replaces S3 transport while retaining positional mapping and reader
error wrapping. Tests cover rollback, concurrent imports, duplicate upserts,
nullable keys, exact decimal/LOB values and temporal/binary JSON-to-JDBC replay.
A non-UTC bootstrap connection verifies the export's UTC setting. The H2 unit
fixture translates only the session-setting syntax; production SQL is unchanged.

No Docker, HTTP receiver or live S3 service is required or exercised. Spark
iteration retains one partition at a time; row writes are serial and the
transaction spans the entire import. Production throughput and lock duration
require separate load testing.
