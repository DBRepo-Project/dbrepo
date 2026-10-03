# Ordered tuple delivery

Source data and the retained source event are committed in one transaction. Each
event carries a stable UUID and a source-database-local committed sequence. See
[the journal contract](services/SOURCE_JOURNAL.md) for migration and retention.

The receiver commits three things together: the target mutation, a per-table/key
ordering fence, and the immutable event payload plus acknowledgement. Retrying an
acknowledged event returns its original local timestamps without another write.
Reusing an event UUID with different content is rejected. Source identities and
the destination mapping must match the configured primary and target.

A full-row update can arrive before its insert. An earlier event is retained but
cannot replace a later version. A delete arriving before its insert leaves a
durable ordering fence even when no local row exists. Neither case invents a
local visibility interval: the response has `applied=false`, and the orchestrator
does not distribute fabricated receiver timestamps. Deleted local versions stay
in native system history. Routing-map changes do not alter event identity.

Binary values travel as base64-encoded bytes, not source-local storage object
keys. DECIMAL values retain decimal precision through JSON and JDBC. Replication
connections use UTC. Received payloads must contain all target columns for an
insert/update; incompatible schemas fail before mutation.

## Read API

`GET /api/v1/database/{id}/replication/journal?after=0&limit=100` returns retained
events and a committed `through` boundary. Supply that same `through` value on
later pages and advance using `nextAfter`, not an offset. Both system and
dedicated replication credentials may read this endpoint. Ordinary users cannot.
Pages are bounded to 1-1000 events. `legacyThrough` identifies migrated entries
whose original commit order is not proved. Missing journal entries and corrupt
payloads fail closed rather than returning an apparently complete empty page.

## Upgrade Gate

Human SQL accounts on replicated databases have SELECT only, including at the
primary. Authorized application writes continue through UI/API, where mutation
and journal append share a transaction. Direct SQL writes and the legacy
`store_query` definer procedure bypass that contract and are not supported for
replicated databases. Non-replicated databases keep their existing SQL grants.
The container service account and dedicated replication account remain technical
writers and must not be handed to users. Reconcile existing grants on both primary
and replica sites before reopening writes; reconciliation preserves primary API
write permissions while removing direct SQL write/procedure grants.

Do not activate the strict receiver independently of source journal migration and
the snapshot/catch-up path. Identity-free requests are rejected. In particular,
the old current-row export is not an ordered source event and must not be
relabelled as one. Existing source/receiver state requires a validated baseline
before new delivery fences can establish completeness. Quiesce old writers and
dispatchers, retain database/history and outbox backups, and follow the coordinated
release procedure. Never silently delete old failed jobs to clear health status.

These delivery guarantees are per event/key. They do not by themselves establish
historical coverage, cross-site multirow transaction visibility, origin-aware
query replay, or safe source reseeding after a backup rollback. Snapshot and
citation-result validation are separate release gates. Preserve existing local
history rather than replacing its timestamps with those of another site.

## Checks

`ReplicationInboxIntegrationTest` uses an isolated MariaDB at loopback and only
the `replication_inbox_test` database. Set `REPLICA_SQL_TEST_PORT` and
`REPLICA_SQL_TEST_PASSWORD` to enable it. It checks lost acknowledgements,
reordered events, tombstones, atomic rollback, concurrent duplicates, identity
conflicts, destination validation, decimals and binary wire values.

`ReplicationJournalServiceUnitTest` covers fixed page boundaries and failure
propagation; `ReplicationDependencyUnitTest` checks that unapplied events do not
create receiver timestamp evidence. The gateway route check is runnable with
`python3 .scripts/test-replication-gateway-routes.py` from the repository root.
