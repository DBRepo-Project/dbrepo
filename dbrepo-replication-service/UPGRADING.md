# Replication upgrades

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
