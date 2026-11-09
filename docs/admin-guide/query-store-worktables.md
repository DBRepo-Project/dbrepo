# Query Store work-table migration

The three Query Store callers now use a fresh UUID-based work-table name for each
invocation. They create a new table without replacing an existing table, and drop
only the table that invocation successfully created. The normal and SQL-error
paths clean up their own working data. Existing user tables named `_tmp` remain
untouched. The SQL resources used for new databases are also used by the migration
generator and regression fixtures.

The work tables are ordinary MariaDB tables so `hash_table` can still discover
columns through `information_schema`. A lost connection or server crash can leave
an orphan. Before removing a suspected orphan, verify its ownership and that no
Query Store call is using it; do not delete tables merely because their names have
a particular prefix. This change preserves the existing result-hash format. The
separate collision/truncation finding #137 is not resolved by work-table isolation.

For an existing installation:

1. Stop incoming Query Store calls, including subset creation and hash/count
   requests. Keep ordinary SQL-user routine execution disabled as documented in
   [the access migration](restrict-query-store-routines.md).
2. Back up the current procedure definitions, definers, SQL modes and grants for
   each affected research database. Keep a database backup and test restoration.
3. Generate a migration for explicit internal database names:

   ```sh
   python3 .scripts/query-worktables-migration.py \
     --database RESEARCH_DATABASE --output /private/query-worktables.sql
   ```

4. Review the file and apply it using the privileged service connection and its
   protected client configuration. The helper writes SQL only; it does not connect
   to the database. Run the existing supported MariaDB SQL mode and verify the
   intended routine definers. The migration reads and preserves existing definers,
   SQL security type, SQL modes and connection character settings. New internal
   routines use the existing root-definer policy.
5. Confirm the marker-table, concurrent-call, failed-call and ordinary-user denial
   checks before resuming traffic. Repeat for every existing research database.

`CREATE OR REPLACE PROCEDURE` retains existing procedure-specific privileges, as
specified in the [MariaDB procedure documentation](https://mariadb.com/docs/server/server-usage/stored-routines/stored-procedures/create-procedure).
It does not repair stale EXECUTE grants from an installation that skipped the
earlier access migration. A service image update alone changes routines only for
newly provisioned databases; it does not migrate existing databases.

The generated script prepares the reviewed procedure body from its hex-encoded
UTF-8 source so that existing SQL modes cannot reinterpret its quoting. Review
the corresponding plaintext SQL resources before applying it. The migration
preserves the session settings of the operator connection after completion.

## Concurrent metadata writes

`store_query` and `_store_query` finish result materialization, hashing and work-table
cleanup before starting a short metadata transaction. They lock the singleton row
in the internal `qs_metadata_lock` InnoDB table, insert or reuse the query identity,
and commit. A SQL error rolls that transaction back. Both paths restore the
connection's previous lock-wait timeout; contention is limited to ten seconds.
The user query is not replayed. Query-result hashes and existing subset IDs remain
unchanged.

The guard belongs to each research database and uses its existing SQL permission
boundary. An account without access to that database cannot acquire its row lock.
No server-wide advisory-lock name is used. Calls in different databases have
separate guards. This patch assumes a single Query Store writer node; multi-node
Galera certification retries require their own deployment acceptance checks.

Run the migration again when upgrading from the initial work-table isolation
patch. It creates the guard table and replaces the routines while preserving their
execution context and grants. If a pre-existing table called `qs_metadata_lock`
does not match the reviewed table marker and schema, migration stops without
modifying that table. Resolve the naming conflict manually before continuing.
A missing guard row causes Query Store persistence to fail; restore the reviewed
singleton row during maintenance after checking why it disappeared.

The service also creates the guard when provisioning new databases. An image
update alone does not migrate existing research databases. Back up the new table
with the other database objects. Keeping it during a rollback to the previous
routine bodies is harmless; pause Query Store calls before changing routines.
