# Resumable Remote Creation

Remote database/table creation reserves a metadata intent in its own committed
transaction before physical provisioning. Origin, source creation ID, kind and
target parent determine one local UUID. Database names use that UUID; table
names remain unchanged so replicated queries and foreign keys keep working.
Concurrent attempts lock the intent until the surrounding metadata transaction
finishes. Pending retries must have the same payload and physical name, and a
second identity cannot reserve the same table name.

The data service records name/payload receipts before replicated table DDL.
After a lost response or metadata rollback, retrying preserves existing rows and
version history. An existing table without a receipt is rejected. A newly
reserved fixed-name database may resume from an empty shell left by a crash;
nonempty databases without a receipt are rejected. Query-store initialization
retries only already-created tables/procedures; unrelated SQL errors propagate.
The existing endpoint repeats user/grant initialization after provisioning.

Intents and receipts are retained, including failed attempts. They are not
distributed transactions and must not be deleted to force a retry. Historical
orphans created before this mechanism need explicit reconciliation, not
automatic adoption. Normal local creation, access policy, receiver inboxes,
snapshots and query-routine bodies are unchanged. Dashboard/search HTTP behavior
is outside these SQL tests.

## Integration

Apply `dbrepo-metadata-db/migration/replication/resumable-creation.sql` before
deploying the metadata and data-service changes together. Fresh Docker/Helm
schema initialization includes the intent table. No DTO or HTTP contract changes
are needed. Preserve the database gateway's `creationLocation` and `replicaUrls`
fields; the physical database service validates the fixed name against them.

## Tests

Set Java 21 `JAVA_HOME` externally and use Maven on `PATH`. From the repository
root, install core once, then run the normal service reactors:

```sh
mvn -f lib/java/dbrepo-core/pom.xml -DskipTests -Djacoco.skip=true -DargLine= install
export DDL_SQL_TEST_PORT=13366
export DDL_SQL_TEST_PASSWORD='<isolated MariaDB root password>'
mvn -f dbrepo-metadata-service/pom.xml -pl :services -am \
  -Dtest=ReplicationCreationSqlTest,ReplicationServiceUnitTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -DargLine= test
mvn -f dbrepo-data-service/pom.xml -pl :services -am \
  -Dtest=ReplicaDdlMariaDbTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -DargLine= test
```

Use only an isolated localhost MariaDB server. Tests create/drop random
`ddl_retry_test_<uuid>` metadata schemas and deterministic `replica_<uuid>`
physical schemas derived from fresh random test identities. They never use
`ingest_replication_test`. Without the SQL environment variable, metadata tests
use H2 and physical-DDL tests are skipped.

Coverage includes metadata rollback after provisioning, replay after commit,
identity/payload collisions, concurrent attempts, connection failure immediately
before/after table DDL, preservation of stored rows/history, rejection of
unowned existing objects and partial query-store initialization.
