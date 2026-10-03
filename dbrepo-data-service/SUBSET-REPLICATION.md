# Canonical subset replication

## Contract

New API-created subsets retain the same query UUID, explicit origin site, original SQL,
normalized SQL, UTC microsecond selection instant, v2 result hash, and row count on peers.
`query_hash` is SHA-256 of the original SQL. `is_persisted` and the one-time attachment
of an immutable result to an older canonical query change under an
origin-owned, monotonically increasing `replication_revision`. This revision orders
subset metadata, **not base-data commits, completeness, or replay coverage**.

Repeated local creation reuses an existing identity only for the same origin, canonical
SQL hash, and result hash. Its original selection instant and normalized SQL stay intact.
Independent creation on another site remains a distinct identity. A query imported
from another site never takes ownership of a local user's identity. Imported rows have
no local `created_by`; origin attribution is not a cross-site username mapping.

`PUT /api/v1/database/{localDatabaseId}/subset/replicate` accepts
`SubsetReplicationDto` only with the `replication` authority. The system credential,
ordinary users, and anonymous callers cannot use it. The authenticated peer must claim
a canonical allowed sender origin and the matching database UUID in local metadata.
The query origin must also be trusted. The existing shared replication credential is
a cluster trust boundary, not a cryptographic proof of which individual peer authored
the payload. The receiver does not execute the incoming SQL or mutate base data.

Duplicate and older revisions are read-only, including MariaDB system history.
Conflicting immutable content, snapshot references, or persistence flags for one revision
return 409. Only the subset's origin accepts local persistence changes; another site
returns 409 identifying the origin, without forwarding user or service credentials.
Changing a secondary-created subset at that secondary does not grant base-data writes.

## Durable Delivery

`qs_subset_outbox` is an InnoDB, per-query/per-target SQL outbox in the data database.
`_store_query` commits the canonical row, immutable result, and outbox together, after the temporary
hashing work has completed its implicit DDL commits. Persistence changes and received
state plus relay entries also share one transaction. No new broker or file store is
introduced. The outbox references immutable query content and coalesces pending
persistence changes to the latest revision.

Targets come only from local database replica mappings. A secondary also retains a
route to its database's primary even while that primary's database UUID is unknown.
Missing mappings stay pending. Receivers relay changed state to their own configured
peers, excluding the sender. Thus a secondary that only knows its primary can reach
other replicas through the primary. Cycles terminate because duplicates do not enqueue.
Routes and the allowlist are revalidated on every retry, and target UUIDs are resolved
from current metadata. No payload-supplied destination URL is followed.

The data service sends directly to the peer's subset endpoint using only the dedicated
replication credential. Redirects are disabled and treated as errors. A SQL connection
lock permits one dispatcher per database; disconnect/crash releases it. Delivery is
at least once. Acknowledgements remove only the delivered revision, preserving a newer
concurrent persistence change. Failed entries retry indefinitely with a 30-900 second
backoff; they never disappear into an exhausted-attempt state. The batch size is 25.

Inspect `query_id`, `target_site`, `revision`, `attempts`, `next_attempt`, and `last_error`
in `qs_subset_outbox`. `last_error` stores the exception class, not response bodies or
credentials. To retry immediately after repairing a dependency, an operator can set
the relevant rows' `next_attempt = UTC_TIMESTAMP(6)` in that database. Do not delete
canonical query rows while they have pending deliveries. Automatic peer retirement,
new-peer backfill, and consolidated monitoring remain parent integration work.

## Upgrade And Configuration

Before using this code with an existing query store, pause subset writes and execute
the existing system-only maintenance endpoint locally:

```text
POST /api/v1/database/{databaseId}/subset/maintenance/upgrade
```

It repeatably adds nullable `creation_location`, `snapshot_hash`,
`replication_revision DEFAULT 0`, the outbox and result tables, and replaces query-store
routines with the transactional versions. Existing
query IDs, SQL, hashes, persistence flags, selection times, and history are preserved.
Existing local timestamps are not reassigned or rebased. Rows without known origin
remain unreplicated; no origin or v2 fixity is invented during migration. Existing v2
rows still verify locally, while old non-v2 rows remain `legacy-unverified`.
DDL is not transactional: retain a backup and rerun the endpoint after an interrupted
upgrade. Do not serve mixed-version writers during migration.

Fresh query-store schema includes the new columns. The subset service initializes
the outbox and result tables/routine before entering its mutation transaction. Direct SQL routine callers that
do not supply the service's trusted replication session context remain origin-unknown
and are not automatically replicated. Ordinary SQL privileges are not broadened.

The data service needs its existing `BASE_URL`, `REPLICATION_USERNAME`, and
`REPLICATION_PASSWORD`, plus the same administratively configured
`REPLICATION_ALLOWED_SITES` list used by metadata/replication services. It defaults
to an empty allowlist, not implicit trust. Base URL and peers must be canonical site
origins; HTTPS is mandatory except for loopback testing.

The dispatcher runs every 30 seconds. `SUBSET_REPLICATION_ENABLED=false` pauses
dispatch, not durable enqueue. Spring properties can override
`dbrepo.replication.subset.enabled` and `dbrepo.replication.subset.retryDelayMs`.

Parent integration snippets, intentionally not applied to shared deployment files:

```diff
--- docker-compose.yml (data-service.environment only)
+++ docker-compose.yml
       REPLICATION_USERNAME: "${REPLICATION_USERNAME:-replication}"
+      REPLICATION_ALLOWED_SITES: "${REPLICATION_ALLOWED_SITES:-}"
+      SUBSET_REPLICATION_ENABLED: "${SUBSET_REPLICATION_ENABLED:-true}"
--- helm/dbrepo/templates/data-secret.yaml
+++ helm/dbrepo/templates/data-secret.yaml
   REPLICATION_USERNAME: "{{ .Values.replicationservice.auth.username }}"
+  REPLICATION_ALLOWED_SITES: {{ join "," .Values.replicationservice.allowedSites | quote }}
```

Both current gateway configurations already route `/database/{id}/subset/...` to
the data service, including PUT replication and POST maintenance. No gateway,
replication-service, consumer, tuple-service, or metadata hook is required for this
transport. Each receiving database must have a reverse sender-ID mapping; absent
mappings return 409 and keep the sender's outbox entry pending.

## Immutable Results

New API subsets capture the existing query work table once, before it is discarded.
`qs_subset_result_rows` stores the v2 canonical ASCII row encoding: `N` for NULL and
`V` followed by uppercase hex of the original MariaDB binary cell representation.
The schema records column names, exact MariaDB types, and character sets. The original
v2 hash and count are checked against this capture; they are never replaced.
`qs_subset_results` binds the schema and captured row order to a separate immutable
`snapshot_hash`. Its ordinals are artifact positions, not transaction cursors.
The temporary capture table's auto-increment counter is private to one completed
materialization; it is never used to infer a committed prefix or bootstrap watermark.

Transport uses the same trusted peer client and database-ID checks:

```text
PUT  /api/v1/database/{databaseId}/subset/{queryId}/result
PUT  /api/v1/database/{databaseId}/subset/{queryId}/result/rows/{row}/chunks/{offset}
POST /api/v1/database/{databaseId}/subset/{queryId}/result/publish
```

The manifest carries query identity, raw schema JSON, and ordered digest, never row
payloads. Row chunks are at most 64 KiB; each carries its SHA-256, original row hash,
and full encoded row length. The receiver commits its next row/offset after each
chunk. Exact retries are read-only; conflicting retries and out-of-order chunks fail.
The final publication transaction verifies every row, schema, order, original v2 hash,
and original count before setting `ready`. Metadata may arrive before its artifact;
reads then fail closed. The dispatcher acknowledges the outbox only after publication.
Relays cannot forward an artifact that is still staging. Restarts resume the persisted
position. Empty results publish a verified schema with zero rows.

HEAD and GET use the existing database read authorization. They verify the full
artifact, including rows outside the requested page, then read that same InnoDB
repeatable-read snapshot until response streaming completes. They do not execute SQL,
contact the origin, inspect base data, or reinterpret timestamps. JSON pages use the
captured immutable row order; CSV exports the complete artifact. JSON binary values
are base64, CSV binary values are lowercase hex; decimal values avoid floating-point
conversion. `X-Result-Mode: immutable-snapshot` distinguishes this path. Unknown
character sets fail closed. Supported text encodings are UTF-8, ASCII, MariaDB latin1,
UTF-16/UCS2, UTF-16LE, and UTF-32BE.

Persisting an older, explicitly local, v2 canonical query attempts materialization.
It attaches a snapshot only if the result matches the original hash/count, publishing
the reference and its outbox revision together. Persistence flag changes follow in
their own transaction; a failed flag write can leave a valid artifact attached.
Unknown-origin and legacy-hash rows are not silently promoted. Non-null snapshot
references cannot be replaced, even by a later metadata revision.

Immutability is enforced by these APIs, not against SQL administrators. Direct SQL
corruption is detected on read. Keep the result tables with query-store backups and
restrict direct writes to service/admin accounts. Storage is proportional to captured
results (hex expands binary cells); source materialization and checksum work run in
MariaDB, while application reads and network payloads stay bounded. MariaDB LONGBLOB/
LONGTEXT and packet/server limits still apply. Interrupted staging is retained for
retry, with no automatic expiry that could invalidate an outstanding citation.

## Replay Boundary

**This implements F4 transport and identity, not F5/F9 historical reconstruction.**
Artifacts provide exact offline reproduction of the captured observation, not proof
that the selection timestamp denotes a complete origin history. Missing, incomplete,
or mismatched artifacts fail before response bytes are emitted. Stored reference
fixity is never replaced with a receiver's result to make replay succeed. Canonical
state is read from SQL, not a stale query-ID-only cache.

Queries without artifacts retain the older local SQL replay path and its fixity check;
that path is not a historical completeness guarantee, and its separate verification
and data executions are not an immutable snapshot. General and legacy historical
queries still require the parent's history/completeness work.

`X-Integrity: verified` means result fixity matched the stored reference; it is
not a history-coverage certificate. In particular, timestamp `AS OF` can change when
an earlier-started transaction later commits. Neither wall-clock timestamps nor
`MAX(AUTO_INCREMENT)` establish a committed prefix. Selection instants are descriptive
metadata, not journal watermarks. The parent's reviewed serialized committed cursor,
typed per-site version history, dependency coverage, and completeness gate must be
integrated before claiming correct historical replay on any peer. Preserve canonical
SQL and original fixity when deriving a separate local execution plan.

## Verification

The dedicated SQL suite only drops/creates `subset_replication_test`. It uses the
already isolated MariaDB tunnel, never other test or deployed databases. Tests model
sites sequentially in this one schema; this is not a deployed three-site recovery
acceptance test. Coverage includes atomic failure rollback, duplicate and stale state,
origin conflicts, secondary relay, late ID mappings, lost responses, acknowledgement
races, concurrent creation/duplicates, repeatable legacy upgrade, and real local
fixity mismatch rejection. Separate tests exercise the production endpoint's method
security and live loopback HTTP credential/redirect behavior.

The checked-in `subset-replication-tests.pom.xml` builds core and data-service together
without installing shared artifacts. From the repository root, with JDK 21 selected
through `JAVA_HOME`, Maven on `PATH`, and the isolated password in
`SUBSET_SQL_TEST_PASSWORD`, run:

```sh
SUBSET_SQL_TEST_PORT=13366 mvn -f subset-replication-tests.pom.xml \
  '-Dtest=SubsetReplication*Test,SubsetResult*Test,SubsetSelectionTimeUnitTest,MariaDbMapperUnitTest,SubsetEndpointUnitTest,MariaDbReplicationBindingTest,TableServiceMariaDbImplUnitTest' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Ddbrepo.replication.subset.enabled=false -Ddbrepo.replication.tupleOutbox.enabled=false \
  -Djacoco.skip=true -DargLine= test
```

`SUBSET_SQL_TEST_PASSWORD` is required. Without `SUBSET_SQL_TEST_PORT`, the isolated
SQL suite skips; unit tests still run. No existing integration suites are implicitly
enabled by these distinct environment variable names.

Artifact tests also cover exact decimals, binary data, Unicode, NULLs, duplicates,
empty results, multi-chunk cells, restart/resume, corrupt data, incomplete publication,
reference mismatch, legacy recapture, and mutations between endpoint verification
and asynchronous response streaming.

Verified on 2026-10-03 with Java 21 and isolated MariaDB: 85 tests passed, zero
failures, errors, or skips. No live deployment or deployed multi-site acceptance test
was performed.
