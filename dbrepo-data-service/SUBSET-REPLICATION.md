# Historical subset replication

## Reproduction contract

A subset retains its query UUID, original SQL, normalized SQL, origin site, UTC
microsecond selection time, result hash and row count. Its execution context records
the origin database/table IDs, a committed local visibility sequence, the view
definitions used for execution, and a hash/count of the visible tuple-version
identities for each referenced table.

The replication key identifies a tuple. A version ID identifies one immutable set
of tuple values. Insert and update events use their retained event UUID as version
ID. Delete closes the existing version's visibility interval.

`tuple_replication_timestamps` records each site's visibility intervals.
`tuple_replication_versions` maps version IDs to local native MariaDB history rows.
Imported versions absent from native history use a typed internal relation named
`_dbrepo_versions_<table UUID without hyphens>`. Table-history transfer remains
available; these are base-data versions, not stored subset results.

The query parser replaces table references with relations joining tuple keys and
version IDs to the original execution site's intervals. Local arrival times on the
site performing re-execution do not select the result. Aliases, joins, nested selects
and recorded view definitions use the same rewrite. Technical replication columns
are excluded from these relations.

For example, A updates a tuple at 10:02, creates a subset at 10:03, and B receives the
update at 10:04. Re-execution on B selects the updated version visible on A at 10:03.
A subset originally created on B at 10:03 selects B's older version.

The visibility sequence excludes changes committed after the original observation
even if a transaction's native timestamp precedes the selection time. Table proofs
detect missing timestamp evidence; the same proof against locally available version
values detects missing history. These proofs contain no query result rows.

Creation, JSON/CSV download, count and integrity validation use the rewritten query.
The stored v2 hash and row count are compared against an actual calculation.
Incomplete history returns HTTP 409 with `error.subset.history.incomplete`.
A different result returns HTTP 409 with `error.subset.integrity.mismatch`.

Queries without historical execution metadata remain in the query store but cannot
claim cross-site reproducibility. Tables that never had replication identities can
use native history only on their original database/site.

## Metadata delivery

`PUT /api/v1/database/{localDatabaseId}/subset/replicate` requires the dedicated
`replication` authority, an allowed canonical sender site and its configured database
mapping. The shared replication credential defines cluster trust; it does not prove
which individual peer authored a payload.

`qs_subset_outbox` stores query/target/revision references. Query metadata and enqueue
share a transaction. Retries send query and reproduction metadata only. No result
manifest, chunks or result rows are transferred. Persistence changes belong to the
subset's origin, and duplicate/older revisions do not change query history.

The dispatcher processes 25 entries per batch, retries with a 30-900 second backoff,
and resolves target UUIDs and allowlist membership on every attempt. Relay to other
configured peers excludes the sender. Duplicate detection terminates cycles.
Inspect `attempts`, `next_attempt` and `last_error` in `qs_subset_outbox`.

Before acknowledging metadata delivery, the dispatcher sends the origin site's
relevant visibility intervals in batches of 256 through the existing timestamp
endpoint. This also distributes intervals recovered from retained receipts when a
subset originates on a replica. A failed evidence batch keeps the subset pending.
The current implementation scans the relevant interval history for each delivery;
per-peer interval checkpoints can reduce that cost for large histories.

Configuration uses `BASE_URL`, `REPLICATION_USERNAME`, `REPLICATION_PASSWORD`,
`REPLICATION_ALLOWED_SITES` and `SUBSET_REPLICATION_ENABLED`.
`SUBSET_REPLICATION_ENABLED=false` pauses metadata dispatch while preserving enqueue.
The normal dispatch interval is 30 seconds.

## Migration and rollout

1. Back up data databases and metadata, including system-versioned history, and
   rehearse the migration on a restored copy.
2. Pause writes and all old replication senders on every participating site.
3. Install the same data-service and replication-service version everywhere.
4. Call the system-only maintenance endpoint for each existing database:

   `POST /api/v1/database/{databaseId}/subset/maintenance/upgrade`

5. The repeatable migration adds `execution_context`, replaces query-store routines,
   drops `qs_subset_result_rows`, `qs_subset_results`, `snapshot_hash` and
   `_capture_subset_result`. Query IDs, queries, origin, selection times and fixity
   remain. Pending metadata outbox entries remain deliverable; their old result
   transfer phase no longer exists.
6. Synchronize tuple history and site timestamp evidence. Unknown legacy mappings
   must remain incomplete; never infer them from equal values or version order.
7. Resume writers and dispatchers together. Verify actual re-execution across all
   sites before accepting the rollout.

DDL is not transactional. If migration is interrupted, keep writers paused and rerun
the maintenance endpoint. The retired `/subset/{id}/result` and child routes return
HTTP 410, so an old result sender cannot silently succeed.

Hashing uses a short-lived SQL work table, removed on success or failure.
It does not retain a materialized subset result.

Hashing and download connections use UTC. For ENUM and SET columns, small typed
domain tables preserve MariaDB's ordinal comparisons and ordering across the
combined history relation. They contain distinct domain values, not tuple versions
or subset results.

## Verification

The isolated MariaDB suites use dedicated databases and require explicit test ports:

```sh
TUPLE_VERSION_SQL_PORT=13366 TUPLE_VERSION_SQL_PASSWORD=version-test-only \
SUBSET_SQL_TEST_PORT=13366 SUBSET_SQL_TEST_PASSWORD=version-test-only \
mvn -f subset-replication-tests.pom.xml \
  '-Dtest=TupleVersionHistoryIntegrationTest,SubsetHistory*Test,SubsetReplication*Test,SubsetResultEndpointUnitTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The tests cover delayed inserts and updates, later deletes, origin-specific
visibility, joins, recorded views, transaction rollback, immutable version identity,
metadata retry and retirement of result-transfer endpoints. Deployment approval also
requires the same scenarios on the actual participating sites.
