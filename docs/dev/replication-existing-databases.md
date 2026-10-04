# Adding a replica to an existing database

On the primary site's **Replication** page, select a database under **Add replica**,
enter a configured peer URL, and submit. An HTTP 202 response means the work is
queued. Track preparation, table creation and history transfer in the replication
outbox. On the target, map the incoming owner's identity to a normal local user
through the existing replica-access controls.

The public API is `POST /api/v1/database/{id}/replicas` with
`{"replica_url":"https://peer.example"}`. The database owner needs the
`create-database` authority; a site administrator with `system` may also submit.
Requests are accepted only on the primary and only for allowlisted, non-local
sites. Repeating a request reuses the existing jobs and target IDs. Removing a
replica is a separate operation and is not supported by this endpoint.

## Activation and initial transfer

1. The metadata transaction writes a preparation notification to its existing
   outbox. Delivery starts after commit.
2. The replication worker queues a stable `DATABASE_PREPARE` job. The source data
   service obtains an exclusive activation lock; ordinary CRUD and CSV imports
   hold shared locks until their operation finishes. Existing writers finish
   before schema preparation; later writers wait.
3. Each native timestamp-versioned InnoDB table gets a unique, non-null
   `replication_key` if missing. MariaDB's `system_versioning_alter_history=KEEP`
   preserves values and both period timestamps. Existing historical versions
   receive their own generated keys during this one-time backfill. Existing keys
   remain unchanged. The source journal is prepared before writes resume.
4. The source metadata transaction registers the peer, its pending ID mappings,
   and the new key-column metadata. It also queues the bootstrap definition.
   Table and view creation use the same metadata database lock; a changed table
   inventory causes preparation to retry before registration.
5. Dependent outbox jobs create the target database, then tables in foreign-key
   order. The existing immutable history-snapshot transfer copies each table's
   history and reconciles current rows, followed by journal catch-up. Existing
   view replication and subset-result delivery are reused afterward.

Archived tables are included for historical queries. The existing table-create
retry handler reapplies the source archive timestamp on the target; they do not
become active tables again.

The preparation flag and locks live in the source data database, so the transition
also works across data-service processes. Requests holding stale pre-activation
metadata reload it before choosing a write path. A lost registration response
does not let such requests resume unjournaled writes. Live event delivery resolves
current peer mappings, including targets added since the event was created.

The scheduler retries unavailable peers. The initial copy only contacts the newly
added target; existing replicas retain their data and learn the new site's ID
mappings. Tuple replication keeps its existing commit/outbox guarantees.

## Compatibility and testing

All peers need the new metadata, data, replication and gateway code. No global
installation-schema change is required: activation creates its internal lock and
journal tables on demand. Unversioned tables, incompatible existing key columns,
and cyclic or external foreign-key dependencies stop preparation rather than
silently producing an incomplete replica.

Canonical subsets keep their existing IDs, provenance, revisions and immutable
results. Older subsets without recorded origin/revision are left untouched and
are not included in the backfill; their provenance or result fixity must not be
invented during activation.

`ReplicationActivationIntegrationTest` runs against an isolated MariaDB using
`REPLICATION_ACTIVATION_SQL_TEST_PORT` and `REPLICATION_ACTIVATION_SQL_TEST_PASSWORD`.
It recreates only `replication_activation_test` and its `_target` schema. It checks
timestamp preservation, repeated activation, an in-flight CSV import, stale CRUD
requests, a lost registration response, and snapshot transfer followed by live
delivery. `DatabaseBootstrapUnitTest` checks durable dependencies and restart
recovery; `DatabaseReplicaServiceUnitTest` checks registration and target policy.
