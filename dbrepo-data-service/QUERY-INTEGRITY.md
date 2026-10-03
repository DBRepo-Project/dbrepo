# Query integrity and upgrades

New query stores use microsecond UTC selection timestamps and `v2:` result hashes. The query identity is SHA-256 of the normalized query without its temporal clause, combined with the result hash. Equal selections with unchanged results reuse the original query ID, original selection time and saved temporal SQL; retries do not replace cited metadata.

## Hash format

The v2 digest includes the ordered column names and MariaDB column types. Each cell is encoded as `N` for NULL or `V` followed by the hexadecimal binary representation of its value. JSON arrays delimit the ordered cells unambiguously. Each encoded row is SHA-256 hashed. Sorted row digests, including duplicates, are folded into a SHA-256 chain seeded with the schema encoding and the `dbrepo:rows:v2:` domain separator. Empty results have a schema-dependent digest. Row order does not affect result fixity; the saved SQL retains requested ordering for presentation.

The algorithm never concatenates the full dataset into a bounded `GROUP_CONCAT`. It uses a session-local row-digest table and one cursor. Schema construction temporarily raises the session concatenation limit, then restores it. Query evaluation uses a uniquely named `_dbrepo_query_<uuid>` work table, removed on completion or SQL failure. Concurrent calls do not share it. Canonical query-ID allocation uses a database/query/result-scoped MariaDB lock, released after the insert commits. All routines use invoker privileges, never a root definer to execute caller-supplied SQL.

Reads of v2 subsets verify the complete result hash and row count before serving a page or export. A mismatch fails the request rather than returning a plausible partial result. `X-Integrity: verified` describes this check, not replication completeness, a cryptographic signature or an independent validation of the original measurements.

## Existing databases

Back up the query store and pause query creation while upgrading. For each database, an authenticated system operator calls:

```
POST /api/v1/database/{databaseId}/subset/maintenance/upgrade
```

This opt-in operation updates timestamp precision, the identity index and routines from the same definitions used for new databases. It is repeatable and retains system-versioned query-store history. It is not a cross-service transaction; after an interrupted upgrade, repeat the operation before resuming query creation. No global installer action runs it implicitly.

Existing query IDs, SQL and result hashes are not rewritten or automatically re-baselined. Legacy subsets remain readable and are explicitly marked `X-Integrity: legacy-unverified`. Their old ambiguous hashes cannot establish v2 integrity retroactively. Published legacy subsets require an archival review against independent reference data before claiming verified integrity. Re-running a selection creates a new v2 identity; it does not silently redefine an existing citation.

Read-only SQL users create subsets through the authenticated API, which checks database access before using the service connection. They do not receive database-write permissions merely to call query-store procedures. Direct SQL query-store routines require the caller's own SELECT, CREATE, DROP, CREATE TEMPORARY TABLES and query-store write permissions.

After a process/database crash, an orphaned `_dbrepo_query_<uuid>` table can remain. With query processing stopped, an operator may remove these work tables after confirming the exact generated name and that no query is running. Never remove base tables or `qs_queries` as part of that cleanup. Work tables are excluded from DBRepo's versioned-table catalogue.
