# Source tuple journal

The existing `tuple_replication_notification_outbox` is also the retained source
journal. A successful handoff changes its delivery status to `SUCCEEDED`; it does
not delete the event. Dispatch/claim/failure paths never rewrite `id`,
`event_sequence`, `payload`, database ID, table ID, or HTTP method. Retention is
intentional. Do not add successful-event cleanup without a separate durable
journal and an explicit citation/recovery retention policy.

## Writer contract

Call `ensureTableExists(connection)` with JDBC auto-commit enabled, before any
data transaction. It performs schema migration and refuses a JDBC transaction.
Then disable auto-commit, perform all source mutations, and call
`enqueue(connection, ...)` for every affected tuple before committing. No DDL is
performed by enqueue. This contract is already used by the tuple write service.
The same InnoDB transaction owns source data, version history, journal inserts,
and the singleton row in `tuple_replication_journal_counter`.

The counter row is locked with `SELECT ... FOR UPDATE` at enqueue time and held
until commit or rollback. Multiple enqueues in one transaction occupy consecutive
sequences; a later writer cannot allocate past an uncommitted transaction. A
rollback also rolls back sequence allocation. `AUTO_INCREMENT` cannot provide
this property. `eventId` is the immutable UUID of the persisted outbox row;
`eventSequence` is its source-database-local sequence. Retry delivery uses that
stored payload. A new source enqueue is a new event, not an idempotency endpoint.

This database-wide lock serializes journal-producing commits. Source mutations
happen before acquisition, so ordinary row-lock/counter-lock conflicts can still
deadlock. On **any** SQL or serialization failure, roll back the whole source
transaction. A deadlock/lock-timeout retry must redo the entire mutation and
enqueue transaction, never retry just enqueue in a partially failed transaction.
Do not hold the counter across network calls. No automatic mutation retry is
introduced here; existing callers roll back and propagate the failure.

## Reader contract for later snapshot/catch-up endpoints

The concrete service exposes two connection-based, read-only methods (they do
not run migrations or modify delivery state):

```java
JournalState readJournalState(Connection reader);
List<JournalEntry> readRange(Connection reader, long after, long through, int limit);
```

Use a separate `REPEATABLE READ` connection with
`START TRANSACTION WITH CONSISTENT SNAPSHOT`, never an active source writer and
never `READ UNCOMMITTED`. Capture `JournalState.committedThrough()` in the same
read view as the data/history export. Page by exclusive sequence, not OFFSET.
`JournalEntry` includes sequence, event UUID, database/table UUIDs, method and
unchanged payload JSON, irrespective of delivery status. Capture a later
committed boundary to catch up after the snapshot; retain every intervening event.
The reader rejects dirty-read isolation, boundaries beyond its read view, and
missing sequences after the migrated legacy prefix. This detects gaps, not
arbitrary content corruption or missing pre-migration history.

A transport page may split a multirow source transaction. Its last sequence is
**not** a publishable transaction boundary. Stage all pages and publish only at
a boundary captured by `readJournalState` on a reader. This schema deliberately
does not infer transaction IDs from clocks or connection IDs. Transaction-wise
streaming publication would additionally need explicit durable transaction
membership; the bounded snapshot/catch-up interface does not provide that.

## Upgrade and historical limits

Quiesce old writers and dispatchers and resolve already-in-flight requests before
upgrading. Mixed old/new source processes are unsupported: old successful-delivery
code deletes journal rows and old enqueue code bypasses the counter. Deployment
and receiver activation must stay gated until this migration boundary is safe.

The migration adds a nullable unique sequence column to old outboxes, removes
`AUTO_INCREMENT` from the prototype schema, and assigns missing sequences in
`created, id` order above the existing maximum. Original row UUIDs become payload
event IDs. Already assigned IDs/sequences and fully identified payload bytes are
preserved. Unknown payload fields are preserved. Invalid JSON, IDs or conflicting
embedded identity/sequence fail closed. Payload backfill, counter initialization
and the migration marker commit atomically; the schema DDL may survive a failed
migration and is retryable. A ready marker is written only after all rows pass.
Non-InnoDB journal/counter tables are rejected before payload backfill.

`JournalState.legacyThrough()` marks the entire migrated prefix, including any
prototype gaps. These synthetic legacy positions do **not** establish original
commit order, restore previously deleted outbox events, or prove historical
completeness. Even an empty old outbox does not establish an empty data history.
Bootstrap must separately export native historical payloads and per-site
observations and validate its source manifest. Do not relabel a legacy sync INSERT
as a historical event or invent receiver timestamps for unapplied stale events.

Keep the counter, journal and source data in the same backup/restore boundary.
Sequences are scoped to this source database, not a global site order. An origin
epoch/database generation contract is still needed before reseeding or replacing
a source database. These reader methods are not HTTP endpoints and do not claim
F5/F9 historical replay or cross-site transaction application is implemented.

## Tests

`TupleReplicationOutboxServiceMariaDbImplUnitTest` uses H2. The actual MariaDB
tests are `TupleReplicationJournalIntegrationTest` in this services module. They
run only with `JOURNAL_SQL_TEST_PORT=13366` and
`JOURNAL_SQL_TEST_PASSWORD` set for the isolated server. They drop and recreate
**only** `journal_replication_test` at `127.0.0.1:13366`; never run them against a
production database. Build `dbrepo-core` and data-service modules in the same
Maven reactor so the new DTO fields are used.

Run the reactor's services target with:

```sh
mvn -pl :services -am test \
  -Dtest=TupleReplicationOutboxServiceMariaDbImplUnitTest,TupleReplicationJournalIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Djacoco.skip=true -DargLine=
```
