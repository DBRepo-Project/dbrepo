# Replication upgrades

## Historical table preservation

Before deploying archival support, apply
`dbrepo-metadata-db/migration/replication/archive-tables.sql` to each metadata
database with a privileged database account. The migration is repeatable and
preserves existing system-versioned rows. Deploy the matching services to all
peers before resuming writes or replication delivery; older receivers still
physically delete tables.

Deleting a table now removes it from active listings, search and dashboards by
setting `archived_at`. Its physical table, historical rows, constraints, replica
mappings and identifiers are retained. Stored subsets can still refer to the
original names. Names of archived tables therefore remain reserved. There is
no automatic purge. The internal data-service DELETE route rejects physical
deletion. Withdrawals requiring destruction need an explicit preservation and
PID policy, not an ordinary table DELETE request.

`GET /api/v1/database/{id}?include_archived=true` includes the archived metadata
under the same access checks as other database metadata. Existing table URLs
remain readable; normal mutation APIs reject archived tables. Deletion events
carry the source archive timestamp, including on retry. A pre-upgrade deletion
event without a timestamp receives the target's first archive timestamp.

## Read-only replica access

Deploy matching metadata-service, data-service and core-library versions. Keep a
backup of metadata, data databases (including historical rows), replica mappings
and outboxes before upgrading. Do not discard queued work during an upgrade.

Replica owners manage local access but receive SQL `SELECT` privileges only.
The dedicated replication account retains its technical write access. Normal
mutation endpoints reject writes on secondary sites, including requests with
administrative or system roles. The authenticated replication endpoints remain
the only application write path on a replica. Missing creation locations on
legacy local databases remain supported.

After upgrading, use a site administrator with the `system` authority to:

1. Read `GET /api/v1/database/replication-access` on each site.
2. For every returned database, call
   `POST /api/v1/database/{databaseId}/replication-access/reconcile`.
3. Require HTTP 204 for each call. Investigate any error before enabling local
   users. This operation is repeatable and preserves owner mappings; it removes
   old database write grants and explicit `store_query` procedure grants from
   all local users except the dedicated replication account.
4. Verify ordinary users can read and create subsets through the application,
   but cannot insert, update, delete, create tables, or invoke definer procedures
   directly over SQL on the replica. Verify writes at the primary still work.

Direct SQL accounts or grants created outside DBRepo's access management need a
separate privilege audit. A rollback must not restore the removed replica write
permissions.

The MariaDB privilege regression test runs against an isolated MariaDB 11.3.2
instance via `REPLICA_SQL_TEST_PORT` (localhost) and
`REPLICA_SQL_TEST_PASSWORD` (root password). Run
`ReplicaSqlAccessIntegrationTest` in the data-service Maven reactor. Never point
this test at a live database server: it recreates `replica_access_test` and its
test user.
# View archival

Apply `dbrepo-metadata-db/migration/replication/archive-views.sql` to each metadata database before starting the new services. Like table archival, removing a view hides it from active listings but retains its SQL definition, columns and identifier relationships. Its internal name stays reserved. Historical queries can continue to reference the definition.

The existing view replication notification carries `archived_at`. Receiving an archive is idempotent, and receiving an older create never clears it. Upgrade every peer before allowing view removals; older peers ignore this field. Physical view deletion through the data API is rejected.
# Outbox writer ownership

The replication service holds an operating-system lock beside `outbox.json` for its entire lifetime. A second process using that directory cannot start. Stop the old writer before starting its replacement and use one replica with a persistent volume supporting file locks and atomic rename. Do not delete the lock file while a writer is active. The lock itself is released by the operating system after a crash; the next process can recover the existing queue.

Startup rejects corrupt queues. If a previously opened queue disappears, the running service fails instead of silently creating an empty queue. Writes flush both the replacement file and its parent directory. This protects the local handoff, not the separate source-data transaction; source outbox atomicity must also be enforced.
# Metadata schema and deployment parity

Before starting upgraded metadata services, back up each metadata database and apply `dbrepo-metadata-db/migration/replication/schema.sql` explicitly to that database. This cumulative, repeatable migration supports the current main schema and earlier replication schemas. It retains system-versioned history, adds nullable origin/access/archive fields, creates replica mappings, resumable creation intents and the metadata notification outbox, and permits terminal outbox entries to have no next attempt. The separate archive migrations are retained for deployments that already applied them.

Docker and Helm fresh-install metadata schemas are identical and checked in CI. Release Compose now mounts the same durable replication outbox volume as source Compose. The Helm replication deployment requires persistence, exactly one writer and `Recreate`; other services keep their configured rollout strategy. Do not run old and new replication writers simultaneously or use an `emptyDir` for queued replication work.

## Combining replication with the current maintenance release

Keep writes, replication delivery and Query Store calls paused while changing schemas and routines. Back up each site's metadata and research databases with their system-versioned history, SQL grants, routine definitions and outboxes. Keep the broker's queues and messages in the backup and rollback plan.

Apply the metadata migration above, then deploy matching core, replication, metadata, data and consumer builds. Deploy the replication service before the data service. Run the system-only `POST /api/v1/database/{databaseId}/subset/maintenance/upgrade` for each research database before allowing Query Store calls. Require HTTP 204 and repeat the call to verify that the upgrade is idempotent. The upgrade preserves existing subset IDs and historical rows, adds the database-local InnoDB metadata guard and installs the canonical hash and outbox-aware routines together. An unrelated table named `qs_metadata_lock` blocks the upgrade and needs investigation.

The legacy `.scripts/query-worktables-migration.py` intentionally refuses a Query Store with replication columns. Its older routines do not maintain replication context or the subset outbox. Use the maintenance endpoint for these databases.

Existing hashes retain their original format. New results use `v2:` hashes that include column boundaries, types, NULLs and duplicate rows, with row order ignored. Legacy results remain labelled `legacy-unverified`. An earlier incorrectly saved normalized query cannot safely be reconstructed from its result hash alone; review the original SQL, selection timestamp and available history before repairing an existing record.

Apply `.scripts/revoke-query-store-access.sql` to each research database as described in the routine-access migration, including non-replicated databases. Ordinary SQL users must not receive EXECUTE on the internal Query Store routines. API, UI and Python subset creation remain supported. Reconnect existing user sessions after privilege changes. Also run replica-access reconciliation as described above; it restricts direct SQL writes on replicated databases.

Use the current consumer-error rollout to install `dbrepo.consumer.errors` and its priority-100 policy. Retain any old `dbrepo.dead-letter` queue until its messages have been inspected and recovered. Do not purge either queue. Broker ingestion now requires the publisher's broker-validated AMQP `user_id`; replay must use an authenticated publisher with a matching identity.

Before resuming writes, check primary writes, replica write rejection, upload ownership, access revocation, historical subset replay and dashboard visibility. Use the client live runner with `--table-deletion archive` for this branch. The client endpoint inventory includes the replication APIs, but the client smoke profile does not exercise the whole replication protocol. Run the three-site checks in [ACCEPTANCE.md](ACCEPTANCE.md) separately.
