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
  every row, even when there are no replica peers. Supplied keys are retained.
- Remote-origin databases and tables reject imports at the service boundary.
  Existing receiver tuple APIs and their signatures are unchanged.
- CSV values retain database-side text conversion, including literal BLOB
  contents, rather than being interpreted as uploaded S3 object references.
  JDBC BLOB results become byte arrays before outbox JSON serialization.
- Successful imports delete the uploaded CSV; failed imports retain it.

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
