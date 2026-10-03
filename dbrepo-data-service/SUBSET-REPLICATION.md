# Canonical subset replication

## Contract

New API-created subsets retain the same query UUID, explicit origin site, original SQL,
normalized SQL, UTC microsecond selection instant, v2 result hash, and row count on peers.
`query_hash` is SHA-256 of the original SQL. Only `is_persisted` changes, under an
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
Conflicting immutable content or two different persistence flags for one revision
return 409. Only the subset's origin accepts local persistence changes; another site
returns 409 identifying the origin, without forwarding user or service credentials.
Changing a secondary-created subset at that secondary does not grant base-data writes.

## Durable Delivery

`qs_subset_outbox` is an InnoDB, per-query/per-target SQL outbox in the data database.
`_store_query` commits the canonical row and outbox together, after the temporary
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

It repeatably adds nullable `creation_location`, `replication_revision DEFAULT 0`, the
outbox, and replaces query-store routines with the transactional versions. Existing
query IDs, SQL, hashes, persistence flags, selection times, and history are preserved.
Existing local timestamps are not reassigned or rebased. Rows without known origin
remain unreplicated; no origin or v2 fixity is invented during migration. Existing v2
rows still verify locally, while old non-v2 rows remain `legacy-unverified`.
DDL is not transactional: retain a backup and rerun the endpoint after an interrupted
upgrade. Do not serve mixed-version writers during migration.

Fresh query-store schema includes the new columns. The subset service initializes
the outbox before entering its mutation transaction. Direct SQL routine callers that
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

## Replay Boundary

**This implements F4 transport and identity, not F5/F9 historical reconstruction.**
GET and HEAD keep the exact original v2 hash/count and compare them to the actual
local replay result. A mismatch throws before returning rows or schema. Stored
reference fixity is never replaced with a receiver's result to make replay succeed.
Canonical state is read from SQL, not a stale query-ID-only cache.

`X-Integrity: verified` means local result fixity matched the stored reference; it is
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

The isolated core+data reactor for this work is `/tmp/dbrepo-subset-reactor-20261003/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>at.ac.tuwien.ifs.dbrepo</groupId>
  <artifactId>subset-replication-test-reactor</artifactId>
  <version>1</version>
  <packaging>pom</packaging>
  <modules>
    <module>../dbrepo-replication-subsets-20261003/lib/java/dbrepo-core</module>
    <module>../dbrepo-replication-subsets-20261003/dbrepo-data-service</module>
  </modules>
</project>
```

Run from the detached worktree, with the isolated test password in the environment:

```sh
env JAVA_HOME=/Users/maximilianholler/Library/Java/JavaVirtualMachines/corretto-21.0.2/Contents/Home \
  SUBSET_SQL_TEST_PORT=13366 \
  /opt/homebrew/bin/mvn -f /tmp/dbrepo-subset-reactor-20261003/pom.xml \
  '-Dtest=SubsetReplication*Test,SubsetSelectionTimeUnitTest,MariaDbMapperUnitTest,SubsetEndpointUnitTest' \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Ddbrepo.replication.subset.enabled=false -Ddbrepo.replication.tupleOutbox.enabled=false \
  -Djacoco.skip=true -DargLine= test
```

`SUBSET_SQL_TEST_PASSWORD` is required. Without `SUBSET_SQL_TEST_PORT`, the isolated
SQL suite skips; unit tests still run. No existing integration suites are implicitly
enabled by these distinct environment variable names.

Verified on 2026-10-03: 66 tests passed, zero failures, errors, or skips, with Java 21
and the isolated MariaDB endpoint at localhost:13366.
