# Immutable table-history snapshots

This is the bounded F9 artifact layer, not a replacement for native target history,
an historical SQL query translator, or an F5 citation-replay claim. It exports all
native versions visible in one source read view, including versions predating the
journal. It cannot recover history already purged at the source. `legacyThrough`
does not establish original commit order or complete pre-journal event coverage.

## Wire contract

`HistorySnapshotDto` contains credential-free records. The root route is
`/api/v1/database/{databaseId}/replication/snapshots`; every method requires
`replication` or `system` authority.

| Method/path | Body/result |
| --- | --- |
| POST root | `Create(snapshotId, tableId, base)` -> `Envelope` |
| GET `/{snapshotId}` | Saved immutable `Envelope` |
| GET `/{snapshotId}/chunks/{index}` | `Chunk(snapshotId,index,sha256,payload)` |
| GET `/{snapshotId}/status` | `Receipt` |
| GET `/table/{tableId}/checkpoint` | Last accepted snapshot `Checkpoint`, or null |
| POST `/imports` | `Import(targetTableId,envelope)` -> `Receipt` |
| PUT `/{snapshotId}/chunks/{index}` | `Chunk` -> `Receipt` |
| POST `/{snapshotId}/verify` | `{ "tableId": "..." }` -> `Receipt` |

The reconciliation endpoint and orchestration belong to the receiver integration,
not this class. No endpoint follows a submitted URL or accepts source credentials.
Target metadata must map the manifest's origin and source database/table UUIDs.
Physical column definitions, including ENUM/SET members, signedness and collation,
must match. Unsupported JDBC types and non-timestamp native periods fail closed.

`Manifest` binds format, snapshot UUID, origin, source IDs, epoch, committed
`boundary`, its `boundaryEventId`, `legacyThrough`, the echoed `base`, ordered SQL
column descriptors, chunk bounds, counts, history root and current-key root.
`Envelope.sha256` hashes the codec's UTF-8 JSON serialization of that manifest.
Each chunk hashes its exact payload bytes. The history root hashes, in index order,
an eight-byte big-endian chunk index followed by a four-byte length-prefixed ASCII
chunk hash. The current-key root hashes four-byte length-prefixed UTF-8 keys in
artifact row order. Verification compares recomputed roots/counts against the
**saved source manifest**, not a new reference supplied with uploaded chunks.

Rows contain `replicationKey`, UTC native `rowStart`/`rowEnd` with six fractional
digits, a current-row flag, and ordered nullable cells. Binary/BLOB/BIT cells use
base64; other cells retain JDBC SQL text (exact decimal, unsigned integer, temporal
and text representations). `HistorySnapshotCodec.data(manifest,row)` returns
binary `byte[]` or SQL strings/null for typed JDBC binding; it is not an untyped
JSON-number conversion. The receiver must bind according to the column types.

## Source consistency and retry

All schema DDL precedes data transactions. Native and artifact storage must use
InnoDB. A REPEATABLE READ consistent snapshot reads journal state and
`FOR SYSTEM_TIME ALL` plus current-row membership in the same view. A metadata
lock pins the table definition. A separate writer persists bounded immutable
chunks without ending the source read view; no LIMIT/OFFSET scan of changing
tables is used. Downloads resume by immutable chunk index; current-key consumers
page by key. A failed export never publishes a manifest.

The source holds `GET_LOCK('dbrepo-history:' + snapshotUUID, 1)` on its writer
connection throughout export. A concurrent attempt receives retryable 503. A
BUILDING row retains a fingerprint of source database, origin and exact Create
request, including its base. After a crashed/failed exporter releases the lock,
an identical retry deletes only that unpublished artifact's chunks/key index and
recaptures a fresh consistent view under the same UUID. A changed request returns
409. READY artifacts are never deleted or regenerated; retries return their
original envelope even if source data subsequently changes. GET on BUILDING is
503; status remains explicitly BUILDING. A short source-lineage transaction
serializes checkpoint reservation, not the full export. No network transfer runs
under that lineage lock.

Limits are 256 rows and 4 MiB per encoded chunk, 1024 columns, and one encoded row
must fit a chunk. Oversized rows fail rather than truncate or publish partial
coverage. A source-side size preflight precedes JDBC streaming; download and HTTP
upload also enforce byte bounds. Table-sized storage/sort work stays in SQL;
application buffering is bounded by chunks. Expired/failed artifact cleanup is
not implemented; there is no automatic history retention deletion.

## Atomic receiver integration

The entry point is:

```java
Receipt reconcileImport(Database db, Table table, UUID snapshotId,
                        HistorySnapshotService.SnapshotReconciler callback);
// callback.apply(Connection transaction, Database db, Table table, Envelope snapshot)
// throws SQLException, IOException
```

Before calling it, prepare inbox/baseline schemas outside any data transaction.
The service locks lineage then snapshot, verifies all chunks and saved manifest,
builds the current-key index, invokes the callback, and publishes RECONCILED in
one transaction. The callback must use that connection, not commit/rollback it,
not execute DDL, and not perform network I/O. Any callback failure rolls back
native changes, key-index publication, receipt and checkpoint. An already
RECONCILED retry does not invoke the callback again. `/verify` alone produces
VERIFIED (history artifact only), never a claim that target current rows match.
An older VERIFIED artifact remains readable but cannot subsequently reconcile
current state after a newer snapshot checkpoint has been accepted.

The callback can page `readCurrentKeys(connection,snapshotId,afterKey,limit)` and
use each pointer's chunkIndex/rowIndex with `readChunk(connection,...)`,
`HistorySnapshotCodec.rows` and `HistorySnapshotCodec.data`. It can test whether
a target key is source-absent using the indexed
`containsCurrentKey(connection,snapshotId,replicationKey)` helper. It must acquire the
receiver's table-level baseline fence also used by ordinary event application.
Reconcile/update/delete only source-owned keys whose event heads are <= boundary;
preserve heads > boundary. Publish the table baseline to prevent a late <= boundary
event for a source-absent key from resurrecting it. Preserve all existing target
native periods; apply ordinary DML for current changes. Do not fabricate a journal
event or occupy the unique `(source database, sequence)` inbox key for a snapshot.
Before DML, the receiver must also reject a current native ROW_START ahead of its
SQL clock: ordinary MariaDB versioned updates under clock regression can discard
the expected prior version. The snapshot artifact layer cannot guard writes made
inside an external callback or ordinary inbox application.

## Restore witnesses

`Checkpoint(epoch, boundary, eventId)` is a **pre-request** observation, not a
received-stream completeness watermark. `epoch` may be null before a source
snapshot has established it; boundary > 0 always requires the exact event UUID.
Source creation compares epoch when known, rejects a regressed cursor and checks
that the exact sequence/UUID still exists in its journal read view. Source-local
lineage also checks its previous observed cursor and event witness.

The integration wrapper must capture and durably retain, keyed by snapshot UUID
and validated source identity, the higher of the last accepted snapshot boundary
and highest received inbox `(sequence,eventId)`, retaining a known epoch. Send it
as Create.base and require the returned manifest to echo it exactly. The stock
checkpoint endpoint only exposes snapshot lineage; it does not inspect the inbox.
The wrapper must combine them before requesting the source export. Do not compare
the snapshot boundary to live inbox heads at reconciliation: legitimate > boundary
events can arrive after capture and must be retained. A base ahead of the previous
snapshot is accepted. A changed known epoch, conflicting equal-boundary witness,
or regressed snapshot boundary is rejected.

Without a surviving external witness, a coordinated rollback of every source and
target record is not detectable. This protocol does not claim otherwise. Manual
source-generation replacement needs an explicit migration/reseed procedure; it
is not silently accepted.

## Checks

`HistorySnapshotIntegrationTest` requires `HISTORY_SNAPSHOT_SQL_TEST_PORT=13366`
and `HISTORY_SNAPSHOT_SQL_TEST_PASSWORD=isolated-test-only`. It recreates only
`history_snapshot_test` on localhost. The two simulated sites use different native
tables within that one authorized schema; the test transport moves artifact rows
between their shared artifact namespace. It is not a deployed multi-server test.

The checks cover consistent-view concurrent writes, exact types/microseconds,
native target-history preservation, empty and multichunk artifacts, missing and
tampered chunks (including self-consistent tampering), schema/origin binding,
source cursor/UUID regression, nullable-epoch frontier, callback rollback and
idempotency, per-snapshot export locking and failed-export same-ID recovery.
Endpoint tests check authority enforcement and bounded request-body reading.
