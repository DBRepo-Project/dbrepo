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
   preserves values and both period timestamps. Legacy rows receive keys during
   this one-time backfill; existing keys remain unchanged. The source journal is
   prepared before writes resume.
4. The source metadata transaction registers the peer, its pending ID mappings,
   and the new key-column metadata. It also queues the bootstrap definition.
   Table and view creation use the same metadata database lock; a changed table
   inventory causes preparation to retry before registration.
5. Dependent outbox jobs create the target database, then tables in foreign-key
   order. The existing immutable history-snapshot transfer copies each table's
   history and reconciles current rows, followed by journal catch-up. Existing
   view replication and historical subset-metadata delivery are reused afterward.

Archived tables are included for historical queries. The existing table-create
retry handler reapplies the source archive timestamp on the target; they do not
become active tables again.

### Galera preparation

History-preserving ALTER statements carry `system_versioning_alter_history=KEEP`
in `SET STATEMENT`, so Galera appliers receive the policy without changing global
settings. On a multi-node cluster, existing rows receive deterministic keys scoped
to the source site, database, table and a non-null primary/unique key. UUID defaults
are restored afterward for new inserts. Existing keys are never regenerated.
An interrupted default change can be completed by retrying preparation.

Populated historical tables without a non-null primary/unique key are rejected
before ALTER on Galera; an offline, canonical history-preserving preparation is
required for these tables. Empty tables, including their historical rows, do not
need a legacy key. Single-node preparation retains UUID backfilling.

For clusters previously affected by session-only ALTER handling, do not rerun UUID
defaults separately on each node. Pause writers, preserve each node's full history,
and repair from the node with the authoritative existing keys. Compare all original
values and microsecond periods, then exact replication keys, before resuming.

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

Canonical subsets keep their existing IDs, provenance, revisions, historical
execution context and result hashes. Older subsets without recorded origin/revision
are left untouched and are not included in the backfill; their provenance or result fixity must not be
invented during activation.

`ReplicationActivationIntegrationTest` runs against an isolated MariaDB using
`REPLICATION_ACTIVATION_SQL_TEST_PORT` and `REPLICATION_ACTIVATION_SQL_TEST_PASSWORD`.
It recreates only `replication_activation_test` and its `_target` schema. It checks
timestamp preservation, repeated activation, an in-flight CSV import, stale CRUD
requests, a lost registration response, and snapshot transfer followed by live
delivery. `DatabaseBootstrapUnitTest` checks durable dependencies and restart
recovery; `DatabaseReplicaServiceUnitTest` checks registration and target policy.

`ReplicationActivationGaleraIntegrationTest` requires three isolated Galera nodes,
exposed locally through `REPLICATION_ACTIVATION_GALERA_SQL_PORTS=port1,port2,port3`
and `REPLICATION_ACTIVATION_SQL_TEST_PASSWORD`. It recreates only
`replication_activation_galera_test`. Never point it at application database servers.
It checks exact key/value/period agreement on all nodes, composite and unique keys,
live writes from another node, unchanged history policies, unsafe legacy-key rejection
and recovery after an interrupted default change.
