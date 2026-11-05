# Restrict query-store routines

Query-store procedures execute privileged service operations. Ordinary SQL users must use the API, UI or Python client to create subsets. Their direct SELECT queries continue to use their database permissions. Access changes disconnect the affected SQL account so cached grants cannot survive the change; open transactions on those connections are rolled back.

The default read and write grants no longer include EXECUTE. Access creation, updates and removal also revoke old database-level EXECUTE and the separate grants on `store_query`, `_store_query`, `hash_query` and `hash_table`. The configured Data Service account needs MariaDB CONNECTION ADMIN or SUPER permission to terminate those sessions. Custom `GRANT_DEFAULT_READ`, `GRANT_DEFAULT_WRITE` and Helm grant settings must omit EXECUTE and ALL privileges. The service rejects those broad grants.

For existing installations, stop external SQL access during migration. Run the following command for each research database, using the configured Data Service database account:

```sh
mariadb --host DATABASE_HOST --user SERVICE_USER --password RESEARCH_DATABASE \
  < .scripts/revoke-query-store-access.sql
```

The script requires a `qs_queries` table in the selected database. It removes database-level EXECUTE and explicit grants on the four internal procedures, preserving their definers and the account running the migration. Running it again makes no further changes. It does not revoke global administrator privileges.

Restart MariaDB after migration to forcibly terminate sessions that retain cached privileges. Keep external SQL access blocked until the restart is complete. Verify that SELECT still works, calls to the internal procedures fail, and subset creation through the API works before restoring access. Review any manually configured roles or global EXECUTE grants separately: ordinary research accounts should not have them.

The migration regression check runs against a disposable MariaDB container:

```sh
python3 .scripts/test-query-store-access.py
```
