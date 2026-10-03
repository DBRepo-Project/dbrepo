# Source CSV ingest replication (F18)

`IngestReplicationMariaDbIntegrationTest` is opt-in and destructive only to
`ingest_replication_test`. It uses real MariaDB JDBC, system-versioned InnoDB
tables, the production mapper, replication service and SQL outbox. Spark runs
locally with two workers. The test CSV adapter substitutes local files for S3
and preserves the production reader's positional column mapping/error wrapping.
There are no mocked SQL statements or outbox writes.

## Contract

- One import is one data transaction, including every per-row POST outbox entry.
  Outbox DDL runs before the transaction. Rows, history and pending notifications
  all roll back on SQL, serialization or later Spark partition failure.
- There is no staging table to share, overwrite, leak or implicitly commit.
- CSV columns remain positional. Supply all table columns, or omit the known
  `replication_key` column. Omitted/null keys are generated independently for
  inserted row, even when there are no replica peers. Supplied keys are retained
  for inserts; duplicate rows retain their existing replication identity.
- Remote-origin databases and tables reject imports at the service boundary.
  Existing receiver tuple APIs and their signatures are unchanged.
- CSV values retain database-side text conversion, including literal BLOB
  contents, rather than being interpreted as uploaded S3 object references.
  JDBC BLOB results become byte arrays before outbox JSON serialization.
- Successful imports delete the uploaded CSV; failed imports retain it.
- The original `ON DUPLICATE KEY UPDATE` behavior is retained for both primary
  and alternate unique-key conflicts, except that existing `replication_key`
  values are never overwritten. MariaDB `RETURNING` supplies the stored row and
  timestamps for a full-row POST upsert event per input row. Both existing and
  parent receiver POST paths are identity-keyed upserts. Repeated rows and
  no-op duplicates are supported; a later failure rolls back the entire import.
  Legacy databases without an origin remain writable.

## Run

Use a core+data reactor POM containing `lib/java/dbrepo-core` and
`dbrepo-data-service` modules, rooted at this worktree. This task's external
reactor is `/tmp/dbrepo-ingest-reactor-20261003.xml`.

```sh
export JAVA_HOME=/Users/maximilianholler/Library/Java/JavaVirtualMachines/corretto-21.0.2/Contents/Home
export SPARK_LOCAL_IP=127.0.0.1
export INGEST_SQL_TEST_PORT=13366
export INGEST_SQL_TEST_PASSWORD='<isolated database password>'
/opt/homebrew/bin/mvn -f /tmp/dbrepo-ingest-reactor-20261003.xml \
  -pl :services -am -Dtest=IngestReplicationMariaDbIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -DargLine= test
```

No Docker, public services, receiver HTTP calls or production deployments are
needed. S3 transport is not exercised. Spark partitions are iterated without
collecting the whole dataset, but the driver still holds one partition and the
database transaction spans the entire import. JDBC insertion and event creation
are serial per import; production-scale throughput and lock duration need
separate load testing. Do not introduce per-row commits to increase throughput.

## BLOB and DECIMAL Integration

The table service now binds all four BLOB types through one local helper:
`byte[]` binds directly, a source string is an S3 object key resolved once, and
null remains SQL NULL. It does not mutate the caller's BLOB value. DECIMAL binds
as `BigDecimal`, never through `double`. This also covers update/delete lookup
keys and upsert paths. Source extraction converts JDBC `Blob` objects to bytes;
DECIMAL and LONGTEXT retain their JDBC `BigDecimal` and `String` representations.

The shared `MariaDbMapper`, endpoints, outbox and DTOs are deliberately unchanged.
The parent can move the helper behavior into the mapper and remove the local
special cases once the shared ownership work is merged. The relevant mapper
switch replacements are:

```java
case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
    if (value == null) {
        statement.setNull(idx, Types.BLOB);
    } else if (value instanceof byte[] bytes) {
        statement.setBytes(idx, bytes);
    } else if (value instanceof String key) {
        statement.setBytes(idx, storageService.getBytes(key));
    } else {
        throw new IllegalArgumentException("BLOB must be an object key or byte array");
    }
    break;
case DECIMAL:
    if (value == null) {
        statement.setNull(idx, Types.DECIMAL);
    } else {
        statement.setBigDecimal(idx, value instanceof BigDecimal decimal
                ? decimal : new BigDecimal(value.toString()));
    }
    break;
```

The parent-owned receiver must decode JSON base64 strings only for BLOB-typed
replication fields before calling tuple APIs (or bind them directly in its new
receiver). Invalid base64 must fail rather than fall back to S3 lookup. Preserve
null and empty bytes, and accept already-decoded `byte[]`. Do not guess whether
ordinary source strings are base64: source object keys may also look like base64.
With the current endpoint, the conversion belongs in both
`tupleFromReplicationPayload` and `tupleUpdateFromReplicationPayload`; DELETE
only needs the replication key.

Typed wire parsing also needs to preserve DECIMAL as `BigDecimal` before values
reach the binder. Jackson's default untyped `Map<String,Object>` number handling
can already have rounded a decimal to `Double`; no binder can recover those
digits. The test wire round-trip enables `USE_BIG_DECIMAL_FOR_FLOATS`. The new
parent-owned receiver should use an equivalent typed/decimal-preserving parser.
The real HTTP receiver and S3 service remain outside this test's coverage.

## Temporal and Binary Fields

The shared source extractor uses JDBC metadata type names to read DATE, TIME,
TIMESTAMP and DATETIME as native strings, preserving all fractional digits and
negative/multi-day TIME values. BIT, BINARY, VARBINARY and BLOB types are byte
arrays. This applies to mutation events, CSV `RETURNING` rows and bootstrap
exports, without DTO changes. System-version timestamps are parsed from native
strings as UTC rather than through JVM-default-zone `java.sql.Timestamp` values.

Bootstrap export sets `time_zone = '+00:00'` on its own connection before SELECT.
Source mutations require the parent-owned UTC setting in
`ReplicationService.prepare`; receiver writes require the corresponding parent
receiver setting. Tests configure real source JDBC sessions in UTC to model that
pending parent change, and separately start bootstrap connections in `+05:45` to
verify that export actively restores UTC. Wire tests replay native temporal
strings and decoded binary bytes into a second MariaDB table, verifying
TIMESTAMP(6), DATETIME(6), negative TIME(6), DATE, BIT(8), padded BINARY and VARBINARY.

## Nullable Predicate Integration

Both UPDATE caller loops now skip null lookup keys because the existing mapper
emits `IS NULL` without a placeholder. Null SET values are still bound. Tests
cover null keys before/after a non-null key, exact DECIMAL assignments, and null
SET values with replication both enabled and disabled.

DELETE callers still bind every key, as required by their current parameter
contract. The parent/mapper owner must replace the mapper's invalid `IS ?` with
null-safe equality for every DELETE key:

```java
.append("` <=> ?")
```

Do not change DELETE to `IS NULL` without also adjusting its caller loops.
The service's replication-key locking query already uses `<=> ?`. Null DELETE
integration remains pending that mapper change; this branch does not edit it.

## Verification on 2026-10-03

The initial core+data/rest reactor selection passed on Java 21 with
`-Djacoco.skip=true -DargLine=`: 24 real MariaDB/Spark ingest cases and 8 existing
unit cases (`TableServiceMariaDbImplUnitTest`,
`TupleReplicationOutboxServiceMariaDbImplUnitTest`,
`BasicAuthenticationProviderUnitTest`). No cases in that selection were skipped.
Only `ingest_replication_test` was used on the isolated SQL server. Full output
is in `/tmp/dbrepo-ingest-final-20261003.log` on the task host.

After the CSV compatibility and temporal/binary additions, all 32 real
MariaDB/Spark cases and 7 existing unit cases passed. One existing H2-only case,
`TableServiceMariaDbImplUnitTest.getReplicationData_pagedRows_succeeds`, cannot
execute MariaDB's `SET time_zone` syntax. That pre-existing fixture is outside
the ingest write scope and needs adaptation by its owner; the real MariaDB
bootstrap case verifies the UTC requirement. The complete later run is in
`/tmp/dbrepo-ingest-precision-final-20261003.log`. Tests used MariaDB 11.3.2.
