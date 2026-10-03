# Replication Acceptance and Recovery

These opt-in tools exercise deployed APIs; they do not deploy, fix configuration,
delete research data, purge outboxes, or certify production readiness. They use
Python 3's standard library, SSH, and MariaDB clients. Run from the repository root.

## Safety and Results

- `.scripts/replication-acceptance.py` requires `--allow-writes`. It creates a new
  `replication_acceptance_<random>` database on the first site and lets DBRepo
  replicate it to the other two. It never accepts an existing database/table ID
  as a test target. A new database is used for every run.
- Only use dedicated acceptance users. Mapping a replica user also writes an
  origin identity mapping; never supply a researcher's account. The runner does
  not create users, change realm roles, or change existing database ACLs.
- HTTPS, certificate verification, explicit site origins, no HTTP redirects,
  no ambient proxy, environment-only HTTP credentials. Do not put secrets in
  config, command arguments, reports, Git, or shell history. HTTP bodies and
  command stderr are withheld from reports. Keep evidence private nevertheless.
- Per-request timeout: 20 seconds. Convergence wait: 120 seconds. Whole-run
  deadline: 1,800 seconds. These are configurable within hard limits. A failed
  write is never automatically repeated: its outcome may be ambiguous.
- Reports are exclusively created with private permissions and updated after
  each check. Exit `0` means all implemented checks passed, `1` means a failed
  assertion, `2` means blocked/incomplete. A blocker is never a pass. A report
  still marked `RUNNING` is incomplete, including after SIGKILL or machine loss.
- No automatic teardown. Keep fixture IDs, persisted selections, failed events,
  and dump evidence for investigation. Archive is an explicit tested action,
  not cleanup. DBRepo has no general database-delete API at this baseline.
  Review fixture resources before any separately authorized manual cleanup.

## Three-Site Configuration

Provide a private JSON file outside Git. Replace the example HTTPS origins and
primary container UUID. The first site is primary. Every user must already exist
on its site, be initialized in DBRepo, and have the attested realm roles. The
primary also needs `create-database` and `delete-table`. Role attestations are
operator input, not independently verified Keycloak grants; a rejection test
without these grants would only demonstrate a missing role.

```json
{
  "primary_container_id": "11111111-1111-4111-8111-111111111111",
  "dedicated_test_users": true,
  "sites": [
    {
      "name": "primary",
      "url": "https://primary.example.invalid",
      "user_env": "ACCEPTANCE_A_USER",
      "user_password_env": "ACCEPTANCE_A_PASSWORD",
      "system_env": "ACCEPTANCE_A_SYSTEM_USER",
      "system_password_env": "ACCEPTANCE_A_SYSTEM_PASSWORD",
      "attested_user_roles": ["create-database", "create-table", "delete-table", "insert-table-data", "delete-table-data", "persist-query"]
    },
    {
      "name": "peer_b",
      "url": "https://peer-b.example.invalid",
      "user_env": "ACCEPTANCE_B_USER",
      "user_password_env": "ACCEPTANCE_B_PASSWORD",
      "system_env": "ACCEPTANCE_B_SYSTEM_USER",
      "system_password_env": "ACCEPTANCE_B_SYSTEM_PASSWORD",
      "attested_user_roles": ["create-table", "insert-table-data", "delete-table-data", "persist-query"]
    },
    {
      "name": "peer_c",
      "url": "https://peer-c.example.invalid",
      "user_env": "ACCEPTANCE_C_USER",
      "user_password_env": "ACCEPTANCE_C_PASSWORD",
      "system_env": "ACCEPTANCE_C_SYSTEM_USER",
      "system_password_env": "ACCEPTANCE_C_SYSTEM_PASSWORD",
      "attested_user_roles": ["create-table", "insert-table-data", "delete-table-data", "persist-query"]
    }
  ]
}
```

Supply the named variables from a secret manager or an in-memory parent process.
The system account is site-local and separate from the ordinary test account.
Existing trusted-peer configuration must already allow exactly these intended
targets. No technical credentials are copied between sites by this runner.

```sh
python3 .scripts/replication-selftest.py
python3 .scripts/replication-acceptance.py \
  --config acceptance-sites.json --report acceptance-result.json --allow-writes
```

Without fault configuration, the outage gates are **BLOCKED**, even if other
checks succeed. Rotating the first site in three separate runs tests all primary
directions. Each run leaves fresh fixtures. Capture deployed revisions, image
digests, schema migrations, and peer configuration independently before running;
the acceptance runner does not infer deployment parity from container health.

## What Is Exercised

| Check | Observable acceptance condition |
| --- | --- |
| Primary CRUD | POST/PUT/DELETE become exact expected current rows on both peers. |
| Replica ordinary user | Authenticated, mapped users can read; tuple insert/update/delete and table creation return 403; history/current rows remain unchanged. |
| Local historical selections | A persisted selection made on each site retains original rows and verified `v2:` result hash after updates/deletes. |
| Duplicate delivery | Exact committed insert payload replay does not change current rows or history events. Models a lost acknowledgement, not an actual dropped TCP response. |
| Reordering | Old update after newer update cannot reset values; old insert after delete cannot resurrect a tuple. History must not grow. |
| Outage and retry | Peer C misses fixture writes while A/B continue; all three operations are durably visible, only exact scoped entries are retried, and queues/current rows converge. `retried=false` fails. |
| Outage history | All sites retain both historical values; complete closed per-site version/timestamp maps are identical, with two versions for each site. Requires SQL observers. |
| Archive preservation | DELETE archives the table on all sites, removes it from active listings, and leaves each local subset reproducible with its original hash. |
| Canonical subset | The primary's original subset UUID, persistence, rows, and hash replay on both peers after archive. Local equivalent queries do not satisfy this gate. |

The canonical-subset and ordering gates are expected to expose remaining gaps at
`fcea06aeb`; they are deliberately not inverted to expect broken behavior. Tests
continue in separate fixture tables after ordinary assertion failures.

## Scoped Fault Injection

Only in a separately authorized isolated fault environment, add:

```json
{
  "isolated_fault_environment": true,
  "fault_hook": ["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "lab-control", "fixture-fault-controller"],
  "outage_wait_for": "PENDING"
}
```

These are additional top-level fields, not a replacement for `sites`. Pass
`--allow-faults` as well. The environment-specific controller must already exist;
no default command stops containers, changes firewalls, or modifies a live VPS.
The hook consumes one JSON object on stdin and must echo that **exact object**
as its only stdout after applying the change:

```json
{
  "run": "replication_acceptance_<random>",
  "database_id": "<primary UUID>",
  "table_id": "<primary UUID>",
  "remote_database_id": "<peer C UUID>",
  "remote_table_id": "<peer C UUID>",
  "target_url": "https://peer-c.example.invalid",
  "action": "block",
  "ttl_seconds": 1860
}
```

Implement `block`/`unblock` through the lab proxy's configuration API or an SSH
controller. Reject only primary-to-C replication writes for these fixture IDs,
including timestamps; leave observer GETs and all other database traffic intact.
Apply atomically, make unblock idempotent, and enforce automatic TTL expiry.
The hook is trusted operator code: its acknowledgement does not itself prove
isolation; the runner also requires a missing row and durable queued events.
An unblock call runs in `finally`, including after a partial/failed block. The
TTL is necessary for SIGKILL, power loss, or failed SSH cleanup. Never use a
site-wide outage hook on a shared/live site.

Set `outage_wait_for` to `FAILED` to require exhausted delivery events before
unblocking, using a bounded retry policy configured separately in the lab.
Allow sufficient `--wait`/`--deadline`; failure to reach that state is a failure,
not a skipped success. The exercised manual retry API is the **target delivery
outbox**, not the data-service source outbox. If automatic retry wins the race
and nothing remains to retry, the manual-retry check is blocked. At this baseline
the API also returns true for an already-succeeded entry, so a race between list
and retry cannot establish exclusive manual causation.

For exact recovery history, add `sql_observer` to **each site**, for example:

```json
"sql_observer": ["ssh", "-o", "BatchMode=yes", "-o", "ConnectTimeout=10", "lab-a", "mariadb --defaults-extra-file=/secure/acceptance-observer.cnf --batch --skip-column-names --raw"]
```

The command consumes SQL on stdin and returns one JSON object per result row,
without banners. Credentials belong in a protected client file or in-memory
wrapper, never argv. The runner sends only read-only-transaction SELECTs against
its new fixture database, the outage table, and its replication key's timestamp
records. Use an account with SELECT access, and a command that does not suppress
SQL errors. Exact histories are stronger evidence than equal current rows or
empty outboxes. Current-data replication export is not a history backup API.

## History Backup and Restore Probe

`--dump-history` is a **mariadb-dump** option, not a `mariadb` SQL-client option.
MariaDB documents history support from 10.11 onward:
[system-versioned tables](https://mariadb.com/docs/server/reference/sql-structure/temporal-tables/system-versioned-tables)
and [mariadb-dump](https://mariadb.com/docs/server/clients-and-utilities/backup-restore-and-import-clients/mariadb-dump).
The probe checks server identity and client option support before writing.

```sh
read -r -s ACCEPTANCE_DB_PASSWORD
export ACCEPTANCE_DB_PASSWORD
python3 .scripts/replication-restore.py --allow-isolated-writes \
  --port 13366 --output restore-evidence-new
unset ACCEPTANCE_DB_PASSWORD
```

Local mode only connects to `127.0.0.1`, refuses port 3306, and requires installed
`mariadb` and `mariadb-dump` clients (override their paths with `--client`/`--dump`).
It writes a temporary mode-0600 option file and removes it afterward. Do not
substitute MySQL's mysqldump: history support must actually be present.

Where the parent provides an isolated Docker test container over SSH, use its
installed MariaDB clients without installing local clients:

```sh
python3 .scripts/replication-restore.py --allow-isolated-writes \
  --ssh lab-host --container acceptance-mariadb-test --output restore-evidence-new
```

SSH mode accepts only container names prefixed `acceptance-` or
`replication-access-test-`. Password is read from the selected environment
variable and sent over encrypted stdin, not process arguments. The operator must
verify that the explicitly named instance is disposable; naming alone is not an
isolation boundary. SSH mode connects inside that container, not through `--port`.

The probe creates exactly two unique databases named
`acceptance_restore_test_<random>_source` and `_restored`; it never drops or reuses
a database. It creates two system-versioned fixture tables (explicit and implicit
period columns), a saved selection time, then updates and deletes a selected row.
It dumps only those three tables with `--dump-history --single-transaction`,
without `--databases` or global statements, into a private evidence directory.
It imports into the separate empty target and verifies:

1. All three retained versions per table, including the deleted row, are equal.
2. Every row-start/end value matches at microsecond precision.
3. A historical JOIN at the saved selection returns the original two rows.
4. Current rows match, while the deleted row remains absent from current data.

Evidence is `history.sql` plus `report.json` with server version, created database
names, selected time, and SHA-256 hashes. Every SQL/client invocation has a bounded
timeout. Failed attempts also retain their own uniquely named databases for
inspection; no unrelated database is touched.

### Verification Recorded on 2026-10-03

On the parent-provided isolated MariaDB 11.3.2 instance, the real dump/restore
probe passed all four conditions. Source and target:

- `acceptance_restore_test_f0c39d1257b0_source`
- `acceptance_restore_test_f0c39d1257b0_restored`
- History SHA-256: `acb27bc5a53b0572bf44742415f3c9a25220c7d96bfc4133585a36c7ac9bee23`
- Historical JOIN SHA-256: `3951ffacaa379315f0bb283e3616deb907a117ba2432f7f0af76173e2ac580ec`

An earlier probe stopped on an incorrect fixture-count assertion, corrected from
four to three versions per table. Its `acceptance_restore_test_efbcf502b45f_*`
databases were left intact. This is a fixture recovery proof, **not** a restored
production DBRepo installation or a live three-site acceptance result.

## Operational Recovery Verification

For real recovery, first quiesce application writes/dispatch and preserve a
consistent set of data databases **with history**, query stores and routines,
metadata including archive flags and mappings, all three outbox stores, object
storage, identity/ACL data, schema migrations, versions, and protected secrets.
A data-only/current-row dump is insufficient. Coordinate multi-database and
nontransactional stores: `--single-transaction` alone is not a distributed backup.

Restore into an isolated environment with outgoing replication disabled. Never
load a `--databases`/`--all-databases` dump into a shared instance assuming the
chosen client database redirects its qualified statements. Check schema names,
views, routine definers, grants, and cross-database references before restoring;
this fixture probe deliberately has no such production dependencies.

Compare full version rows, period timestamps, replication maps, query IDs,
normalized selections, original result hashes/counts, archived-table access,
object bytes, and unresolved outbox IDs/statuses against the backup manifest.
Replay persisted subsets through the restored application, including historical
JOINs and archived relations. Preserve pending and failed work; never mark it
succeeded or purge it to obtain green monitoring. Reconnect peers only after
reviewing duplicate/order handling and verifying catch-up history and all-site
timestamp convergence. Repeat the three-site checks with new fixtures.

Additional release gates remain outside these tools: direct SQL/Broker user
rejection, actual TCP response loss, source-outbox FAILED retry, process-crash
atomicity, full/damaged disks, dependency/tombstone recovery, CSV/BLOB ingress,
historical bootstrap, concurrent writers, and complete production backup recovery.
Run their focused service/integration tests and operational rehearsals separately.
Offline self-tests validate the tooling, not those system guarantees.
