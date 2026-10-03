# Replication Acceptance and Recovery

These opt-in tools exercise deployed APIs; they do not deploy, fix configuration,
delete research data, purge outboxes, or certify production readiness. They use
Python 3's standard library, SSH, and MariaDB clients. Run from the repository root.

The journal/inbox, immutable subset artifacts, and asynchronous `HISTORY_SYNC`
contracts below must all be activated by the parent before a live run. A
consumer-only rollout or mixed old/new services is not sufficient. Preparation
and offline tests do not authorize a deployment or live fault.

## Safety and Results

- `.scripts/replication-acceptance.py` requires `--allow-writes`. It creates a new
  `replication_acceptance_<random>` database on the first site and lets DBRepo
  replicate it to the other two. It never accepts an existing database/table ID
  as a test target. A new database is used for every run.
- `parent_coordinated=true` and `strict_snapshot_activation=true` are explicit
  operator attestations required before any fixture write. They are not proof
  of deployment parity; retain the parent's revision/image/migration inventory.
- Only use dedicated acceptance users. Mapping a replica user also writes an
  origin identity mapping; never supply a researcher's account. The runner does
  not create users, change realm roles, or change existing database ACLs.
- HTTPS, certificate verification, explicit site origins, no HTTP redirects,
  no ambient proxy, environment-only HTTP credentials. Do not put secrets in
  config, command arguments, reports, Git, or shell history. HTTP bodies and
  command stderr are withheld from reports. Keep evidence private nevertheless.
- Per-request timeout: 20 seconds. Convergence wait: 120 seconds. Each history-sync
  job set has a separate 900-second `--sync-wait` deadline. Whole-run
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
  "parent_coordinated": false,
  "strict_snapshot_activation": false,
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
Change the two activation attestations only after parent coordination. Existing
rollout credential helpers are references for in-memory secret handling, not
dependencies of these scripts; no workstation or VPS helper path is embedded.

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
| Duplicate delivery | Replay the retained event UUID, sequence, original HTTP method, source tuple, and source database/table payload. Only current target maps are hydrated. The original receipt, inbox, head, current rows, and local history remain unchanged. Models lost acknowledgement, not a dropped TCP response. |
| Reordering | Hold older writes on C, allow the latest full value or delete through, then replay an older retained event. C stays latest; the older event is retained with `applied=false`, no local period, and no fabricated historical version. A/B remain operational. |
| Outage and retry | Peer C misses fixture writes while A/B continue; all three operations are durably visible, only exact scoped entries are retried, and queues/current rows converge. `retried=false` fails. |
| Outage evidence | Every source event remains in each receiver inbox. Actual local versions match applied receipts and local timestamp mappings; superseded unseen events are not claimed as locally visible. The final head and current rows reflect the delete. Requires scoped SQL observers. |
| Async history sync | Database and table POSTs return HTTP 202 with durable job UUIDs covering every fixture table/peer pair. Every returned `HISTORY_SYNC` job must reach `SUCCEEDED` and expose a matching `RECONCILED` target snapshot receipt; `FAILED`, `CANCELLED`, missing/unscoped jobs, or bounded timeout fail. Page/tuple counts never imply completion. |
| Archive preservation | DELETE archives the table on all sites, removes it from active listings, and leaves each local subset reproducible with its original hash. |
| Canonical subset | The primary's original subset UUID, persistence, rows, result hash and snapshot hash replay on peers after archive, with `X-Result-Mode: immutable-snapshot`. Local equivalent queries and timestamp re-execution do not satisfy this gate. |
| Offline citation | A parent-provided scoped hook makes the origin's fixture citation return 503 while peers still serve its identical immutable artifact. Without the hook this is BLOCKED, not an offline success. |

Journal reads use `/api/v1/database/{id}/replication/journal?after=0&limit=100`.
The first `through` boundary is pinned on all subsequent pages. The runner checks
contiguous positive sequences, unique event IDs, source identities, stable
boundaries, and `legacyThrough=0` for the fresh fixture. It never uses current-row
`/data/replicate` exports to invent replay identities. `originalHttpMethod` is
retained harness provenance derived from each journal entry's `method`, not an
extra field inserted into the wire DTO. Reports retain event IDs/sequences and
source-payload hashes, not credential-bearing payload dumps.

`POST /api/replication/data/synchronise/database/{id}` must return
`202 {"status":"queued","tables":N,"jobs":["<UUID>",...]}`; the table route
adds `/table/{tableId}` and omits `tables`. The runner polls the existing outbox
list, selects only those exact job IDs, verifies fixture scope and operation type,
and waits for **all** to succeed. A job stages and verifies an immutable history
snapshot, reconciles native current state, then catches up from the retained
journal. The harness does not equate enqueue or legacy tuple/page counts with
that work being completed. It neither retries nor cancels these jobs implicitly.
After success it reads the saved source manifest and target snapshot status using
the job's durable snapshot UUID. Source IDs, origin, pre-request checkpoint,
target fixture IDs, boundary and manifest digest must agree; both
`historyVerified` and `currentReconciled` must be true. The roundtrip artifact must
contain both deleted source versions and zero current keys. Reports retain these
receipts. This checks published snapshot evidence, not an independent reimplementation
of the chunk codec or proof that unseen source history was natively visible on peers.

## Scoped Fault Injection

Only after parent coordination, add these top-level settings. The included
controller injects **fixture storage-write failures**, not network outages:

```json
{
  "scoped_faults_coordinated": true,
  "fault_hook": ["python3", ".scripts/replication-sql.py", "--report", "acceptance-result.json", "--site", "peer_c", "--ssh", "peer-c-host", "--container", "dbrepo-data-db", "--metadata-container", "dbrepo-metadata-service", "--execute", "--allow-scoped-faults"],
  "outage_wait_for": "PENDING"
}
```

These are additional top-level fields, not a replacement for `sites`. Pass
`--allow-faults` as well. Replace the SSH destination and container names with the
parent's verified inventory. The script runs locally, using existing remote
Docker/MariaDB commands; no script deployment is required. It consumes this JSON
scope on stdin and echoes that **exact object** only after successful execution:

```json
{
  "run": "replication_acceptance_<random>",
  "database": "replication_acceptance_<same random>",
  "database_id": "<peer C database UUID>",
  "table": "outage",
  "table_id": "<peer C table UUID>",
  "source_database_id": "<primary database UUID>",
  "source_table_id": "<primary table UUID>",
  "target_url": "https://peer-c.example.invalid",
  "action": "block",
  "policy": "all",
  "coordinated": true,
  "ttl_seconds": 1860
}
```

`.scripts/replication-sql.py` defaults to **PLAN**, making no SSH/SQL calls without
`--execute`. Fault execution additionally requires `--allow-scoped-faults`.
Database/table IDs, names, site, and run must match the harness-created report;
unknown fields, research databases, arbitrary SQL, and unrecorded tables are
rejected. It checks the remote metadata container's `BASE_URL` against the
manifest before SQL, preventing accidental use of another site's same-named
replica fixture. Never fabricate or reuse a manifest to target existing data.

Three `BEFORE INSERT/UPDATE/DELETE` triggers affect only the new target scenario
table. `all` rejects all three operations; `latest-only` admits only the final
`value=latest` and deletes; `delete-only` admits deletes. This lets a newer event
advance the receiver head before older events are replayed. Unseen stale events
must be retained with `applied=false` without invoking native tuple mutation.
Observer GETs, source/other-peer writes, timestamp evidence, queues, accounts,
container state, and unrelated tables/databases are not modified by the hook.

Trigger conditions expire using the database epoch after the requested TTL;
controller/server clock disagreement blocks activation. Expiration makes the
triggers inert, not deleted. `unblock` removes only exact owned trigger definitions
and refuses externally changed definitions. Trigger DDL is not transactional;
the harness always calls `unblock` in `finally`, including after partial creation.
The TTL also bounds partial faults after SIGKILL or SSH loss. Preserve and inspect
any inert triggers if cleanup fails. No queue purge or site-wide fault is allowed.

Set `outage_wait_for` to `FAILED` to require exhausted delivery events before
unblocking, using a bounded retry policy configured separately in the lab.
Allow sufficient `--wait`/`--deadline`; failure to reach that state is a failure,
not a skipped success. The exercised manual retry API is the **target delivery
outbox**, not the data-service source outbox. If automatic retry wins the race
and nothing remains to retry, the manual-retry check is blocked. A race between
list and retry cannot establish exclusive manual causation. Reorder probes allow
automatic recovery and do not claim that manual retry was necessary.

For exact recovery history, add `sql_observer` to **each site**, for example:

```json
"sql_observer": ["python3", ".scripts/replication-sql.py", "--report", "acceptance-result.json", "--site", "primary", "--ssh", "primary-host", "--container", "dbrepo-data-db", "--metadata-container", "dbrepo-metadata-service", "--execute"]
```

Use corresponding `--site` and SSH destinations for `peer_b` and `peer_c`. The
report path must exactly match the harness `--report`. Observers consume a JSON
scope, **not arbitrary SQL**, and return one object with `history`, `inbox`,
`heads`, and `timestamps` arrays. Reads use UTC, read-only transactions, statement
and metadata-lock deadlines, fixed SELECTs, row limits, and the fixture's UUID key.
Native open periods are identified by joining exact current-row membership and
reported as null ends, without assuming a MariaDB-version-specific infinity date.
The existing container `MARIADB_ROOT_PASSWORD` stays in the remote process; it is
never returned, written to argv, or printed. No account/grant changes are made.
An alternative least-privilege observer may implement the same scoped JSON
contract. SQL errors and incomplete/oversized observations never become passes.

Equal current rows or empty outboxes alone do not prove history completeness.
After reordered catch-up C may have fewer native historical versions than A/B;
the inbox retains the missing source events without asserting past local visibility.
Source canonical citations are instead reproduced from immutable result artifacts.

## Parent-Coordinated Run

### Exact Live Setup

For the first direction use `primary=https://s46.datalab.tuwien.ac.at`,
`peer_b=https://s73.datalab.tuwien.ac.at`, and
`peer_c=https://s93.datalab.tuwien.ac.at` in the configuration above. Bind A/B/C
to those sites respectively; rotate all credentials, SSH destinations and the
primary container UUID together for subsequent directions.

| Child-process environment | In-memory value from the matching site |
| --- | --- |
| `ACCEPTANCE_A_USER`, `ACCEPTANCE_A_PASSWORD` | Primary dedicated ordinary user and password |
| `ACCEPTANCE_B_USER`, `ACCEPTANCE_B_PASSWORD` | Peer B dedicated ordinary user and password |
| `ACCEPTANCE_C_USER`, `ACCEPTANCE_C_PASSWORD` | Peer C dedicated ordinary user and password |
| `ACCEPTANCE_A_SYSTEM_USER`, `ACCEPTANCE_A_SYSTEM_PASSWORD` | Primary metadata container `SYSTEM_USERNAME`, `SYSTEM_PASSWORD` |
| `ACCEPTANCE_B_SYSTEM_USER`, `ACCEPTANCE_B_SYSTEM_PASSWORD` | Peer B metadata container `SYSTEM_USERNAME`, `SYSTEM_PASSWORD` |
| `ACCEPTANCE_C_SYSTEM_USER`, `ACCEPTANCE_C_SYSTEM_PASSWORD` | Peer C metadata container `SYSTEM_USERNAME`, `SYSTEM_PASSWORD` |

Supply these via the parent's subprocess `env` dictionary, never printed exports,
argv, or config values. The reference `test_user()` grants only `create-database`;
it is insufficient unchanged. Parent provisioning must grant every attested role,
initialize each local DBRepo user, and keep all three user contexts open until
the harness exits. Do not borrow a researcher account or delete test users before
failure evidence has been reviewed. Read the selected primary container UUID from
authenticated `GET /api/v1/container`; do not copy another site's UUID.

Set each site's `sql_observer` to this argv, replacing the three uppercase
non-secret placeholders with the same absolute report path, its exact site label,
and the parent's verified SSH destination (including user where needed):

```json
["python3", ".scripts/replication-sql.py", "--report", "REPORT", "--site", "SITE", "--ssh", "SSH_DESTINATION", "--container", "dbrepo-data-db", "--metadata-container", "dbrepo-metadata-service", "--timeout", "20", "--execute"]
```

Use `SITE=primary`, `peer_b`, `peer_c` respectively. Set top-level `fault_hook` to
the **peer_c** argv plus `"--allow-scoped-faults"`. Container names and SSH host
keys must be verified from parent inventory first. No remote script upload is
needed. Leave `parent_coordinated`, `strict_snapshot_activation`, and
`scoped_faults_coordinated` false until the coordinated rollout-ready signal.
Keep config/report private and outside Git. An `offline_hook` is additionally
required for a complete offline-citation result, as specified below.

### Execution

1. Freeze and record the deployed revision/image/migration inventory for all three
   sites. Activate retained source journals, receiver inbox/head ordering, full
   origin timestamp keys, immutable subset results, and durable async history-sync
   jobs. Do not run these gates against the mixed consumer-only rollout.
2. Confirm trusted peers, dedicated users and required roles. Supply HTTP secrets
   through the named environment variables from an in-memory parent process.
   Verify SSH host keys and the two container names on each host without printing
   environment contents. No helper here deploys or changes those services.
3. Run `python3 .scripts/replication-selftest.py`. Configure all three observers
   and the C fault hook using the same new report path. Parent authorization is
   required before changing the activation/fault attestations to true.
4. Run a fresh fixture, retaining its private report:

   ```sh
   python3 .scripts/replication-acceptance.py \
     --config acceptance-sites.json --report acceptance-result.json \
     --allow-writes --allow-faults --http-timeout 60 \
     --wait 300 --sync-wait 900 --deadline 3600
   ```

5. Inspect event IDs, source-payload hashes, actual visibility evidence, and every
   returned async job state. A terminal failure, timeout, or blocked gate prevents
   acceptance. Never cancel/purge jobs to obtain a green report. Preserve fixture
   databases, inboxes, outboxes, snapshots, and failed evidence for investigation.
6. Repeat with each primary direction using fresh report/config paths. Do not
   reuse the first run's manifest or test resources.

For the actual offline gate, the parent must additionally provide `offline_hook`
as an argv array. Its JSON contract contains `run`, `database_id`, `subset_id`,
`origin_url`, `target_urls`, `ttl_seconds`, and `action` (`block`/`unblock`). It must
echo the exact scope after activation/cleanup and enforce TTL expiry. Block reads
only for that new origin database/subset API subtree, including result paths,
returning HTTP 503; do not stop a VPS, block shared peers, or alter artifact bytes.
The harness first verifies the origin data URL really returns 503, then reads
the original UUID on both peers and requires identical immutable-snapshot mode,
rows, persistence, result hash, and snapshot hash. Unblocking always runs in
`finally`. This routing hook is deliberately parent/environment-owned; it is not
implemented by the SQL write-fault controller. Without it the offline gate is
explicitly BLOCKED. Ordinary archive/canonical replay is not reported as offline.

### Preparation Evidence

The updated protocol harness and SQL/fault planner pass 27 offline self-tests,
including post-job manifest/receipt agreement, missing peer coverage, terminal
job failures, bounded timeout, and secret-safe controller planning. These doubles
are not real-SQL controller or three-site execution evidence. No live three-VPS
run, SQL fault activation, origin-offline routing
change, or deployment was performed while preparing these tools. The async
response contract is a coordinated requirement, not a claim that an older live
endpoint already implements it. Run the commands above only after activation.

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
