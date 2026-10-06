package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.replication.ReplicationSites;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import at.ac.tuwien.ifs.dbrepo.service.impl.TupleVersionHistory;
import at.ac.tuwien.ifs.dbrepo.service.outbox.TupleReplicationOutboxServiceMariaDbImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

import static at.ac.tuwien.ifs.dbrepo.service.HistorySnapshotCodec.*;

@Service
public class HistorySnapshotService extends DataConnector {
    private static final String SNAPSHOTS = "replication_history_snapshots";
    private static final String CHUNKS = "replication_history_chunks";
    private static final String KEYS = "replication_history_current_keys";
    private static final String LINEAGE = "replication_history_lineage";
    private final TupleReplicationOutboxServiceMariaDbImpl journal;
    private final String baseUrl;

    public HistorySnapshotService(TupleReplicationOutboxServiceMariaDbImpl journal,
                                  @Value("${dbrepo.baseUrl}") String baseUrl) {
        this.journal = journal;
        this.baseUrl = baseUrl;
    }

    public Envelope create(Database database, Table table, Create request) throws SQLException, IOException {
        require(request != null && request.snapshotId() != null && table.getId().equals(request.tableId()),
                HttpStatus.BAD_REQUEST, "Snapshot and source table IDs are required");
        require(!ReplicationSites.isReplica(database.getCreationLocation(), baseUrl)
                        && !ReplicationSites.isReplica(table.getCreationLocation(), baseUrl),
                HttpStatus.FORBIDDEN, "Only the source primary can export history");
        try (var pool = getDataSource(database); Connection reader = pool.getConnection(); Connection writer = pool.getConnection()) {
            prepare(writer);
            journal.ensureTableExists(writer);
            writer.setAutoCommit(false);
            try {
                TupleVersionHistory.backfillSource(writer, baseUrl, database.getId(), table);
                writer.commit();
            } catch (SQLException | RuntimeException failure) { writer.rollback(); throw failure; }
            finally { writer.setAutoCommit(true); }
            require(transactionalTables(writer, List.of("tuple_replication_notification_outbox", "tuple_replication_journal_counter")) == 2,
                    HttpStatus.CONFLICT, "Source journal must use transactional storage");
            final String exportLock = "dbrepo-history:" + request.snapshotId();
            require(advisoryLock(writer, "GET_LOCK", exportLock) == 1,
                    HttpStatus.SERVICE_UNAVAILABLE, "Snapshot export is in progress; retry the same snapshot ID");
            try {
                final String requestDigest = sha256(encode(List.of(database.getId(), baseUrl, request)));
                final Stored existing = find(writer, request.snapshotId(), false);
                if (existing != null) {
                    require(existing.role().equals("SOURCE") && existing.tableId().equals(table.getId()), HttpStatus.CONFLICT, "Snapshot ID binding conflict");
                    if (existing.status().equals("READY")) {
                        final Envelope result = storedEnvelope(existing);
                        require(Objects.equals(result.manifest().base(), request.base())
                                        && result.manifest().sourceDatabaseId().equals(database.getId())
                                        && result.manifest().origin().equals(baseUrl), HttpStatus.CONFLICT, "Snapshot request changed");
                        return result;
                    }
                    require(existing.status().equals("BUILDING") && requestDigest.equals(existing.digest()),
                            HttpStatus.CONFLICT, "Unpublished snapshot request changed");
                }
                utc(reader);
                writer.setAutoCommit(false);
                try {
                    // The lineage lock orders source exports; the journal counter itself is read using MVCC, not locked.
                    execute(writer, "INSERT INTO " + LINEAGE + " (binding, epoch, boundary, event_id) VALUES ('source', ?, 0, NULL)"
                            + " ON DUPLICATE KEY UPDATE binding=binding", UUID.randomUUID().toString());
                    final Checkpoint observed = checkpoint(writer, "source", true);
                    reader.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                    reader.setAutoCommit(false);
                    execute(reader, "START TRANSACTION WITH CONSISTENT SNAPSHOT");
                    final List<Column> columns = schema(reader, table);
                    final var state = journal.readJournalState(reader);
                    require(state.committedThrough() >= observed.boundary(), HttpStatus.CONFLICT, "Source journal cursor regressed");
                    checkAnchor(reader, observed);
                    if (request.base() != null) {
                        require((request.base().epoch() == null || observed.epoch().equals(request.base().epoch()))
                                        && state.committedThrough() >= request.base().boundary(),
                                HttpStatus.CONFLICT, "Source generation or cursor does not satisfy target checkpoint");
                        checkAnchor(reader, request.base());
                    }
                    final UUID eventId = anchor(reader, state.committedThrough());
                    execute(writer, "UPDATE " + LINEAGE + " SET boundary=?, event_id=? WHERE binding='source'",
                            state.committedThrough(), text(eventId));
                    if (existing == null) {
                        execute(writer, "INSERT INTO " + SNAPSHOTS
                                        + " (snapshot_id, role, table_id, status, manifest, manifest_digest) VALUES (?, 'SOURCE', ?, 'BUILDING', NULL, ?)",
                                text(request.snapshotId()), text(table.getId()), requestDigest);
                    } else {
                        // Only an unpublished export can restart its read view. READY artifacts never change.
                        execute(writer, "DELETE FROM " + KEYS + " WHERE snapshot_id=?", text(request.snapshotId()));
                        execute(writer, "DELETE FROM " + CHUNKS + " WHERE snapshot_id=?", text(request.snapshotId()));
                    }
                    writer.commit();
                    writer.setAutoCommit(true);
                    final Export export = export(reader, writer, table, request.snapshotId(), columns);
                    final Manifest manifest = new Manifest(3, request.snapshotId(), baseUrl, database.getId(), table.getId(),
                            observed.epoch(), state.committedThrough(), eventId, state.legacyThrough(), request.base(), columns,
                            MAX_CHUNK_ROWS, MAX_CHUNK_BYTES, export.chunks(), export.rows(), export.currentKeys(),
                            export.historyDigest(), export.currentKeysDigest());
                    reader.commit();
                    final Envelope envelope = envelope(manifest);
                    execute(writer, "UPDATE " + SNAPSHOTS + " SET manifest=?, manifest_digest=?, status='READY'"
                            + " WHERE snapshot_id=? AND status='BUILDING'", new String(encode(manifest), StandardCharsets.UTF_8),
                            envelope.sha256(), text(request.snapshotId()));
                    return envelope;
                } catch (SQLException | IOException | RuntimeException e) {
                    reader.rollback();
                    writer.rollback();
                    throw e;
                }
            } finally {
                advisoryLock(writer, "RELEASE_LOCK", exportLock);
            }
        }
    }

    public Envelope manifest(Database database, UUID snapshotId) throws SQLException, IOException {
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            final Stored stored = required(c, snapshotId, false);
            require(!stored.status().equals("BUILDING"), HttpStatus.SERVICE_UNAVAILABLE, "Snapshot export is incomplete; retry the same create request");
            return storedEnvelope(stored);
        }
    }

    public Chunk readChunk(Database database, UUID snapshotId, long index) throws SQLException, IOException {
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            final Stored stored = required(c, snapshotId, false);
            require(!stored.status().equals("BUILDING"), HttpStatus.SERVICE_UNAVAILABLE, "Snapshot export is incomplete; retry the same create request");
            final Manifest manifest = storedEnvelope(stored).manifest();
            require(index >= 0 && index < manifest.chunks(), HttpStatus.NOT_FOUND, "Chunk not in manifest");
            return readChunk(c, snapshotId, index);
        }
    }

    public Receipt beginImport(Database database, Table table, Import request) throws SQLException, IOException {
        require(request != null && table.getId().equals(request.targetTableId()), HttpStatus.BAD_REQUEST, "Target table mismatch");
        validateEnvelope(request.envelope());
        final Manifest manifest = request.envelope().manifest();
        validateTarget(database, table, manifest);
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            TupleVersionHistory.prepare(c, table);
            TupleVersionHistory.prepareImported(c, table);
            require(schema(c, table).equals(manifest.columns()), HttpStatus.CONFLICT, "Source and target physical schemas differ");
            c.setAutoCommit(false);
            try {
                final String binding = binding(table.getId());
                execute(c, "INSERT INTO " + LINEAGE + " (binding, epoch, boundary, event_id) VALUES (?, ?, 0, NULL)"
                        + " ON DUPLICATE KEY UPDATE binding=binding", binding, text(manifest.epoch()));
                final Checkpoint previous = checkpoint(c, binding, true);
                final Stored existing = find(c, manifest.snapshotId(), true);
                if (existing != null) {
                    require(existing.role().equals("IMPORT") && existing.tableId().equals(table.getId())
                                    && existing.digest().equals(request.envelope().sha256()),
                            HttpStatus.CONFLICT, "Immutable snapshot binding or manifest conflict");
                    c.commit();
                    return receipt(existing);
                }
                checkTargetCheckpoint(previous, manifest);
                execute(c, "INSERT INTO " + SNAPSHOTS + " (snapshot_id, role, table_id, status, manifest, manifest_digest)"
                        + " VALUES (?, 'IMPORT', ?, 'STAGING', ?, ?)", text(manifest.snapshotId()), text(table.getId()),
                        new String(encode(manifest), StandardCharsets.UTF_8), request.envelope().sha256());
                final Receipt receipt = receipt(required(c, manifest.snapshotId(), false));
                c.commit();
                return receipt;
            } catch (SQLException | IOException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public Receipt putChunk(Database database, UUID snapshotId, Chunk chunk) throws SQLException, IOException {
        require(chunk != null && snapshotId.equals(chunk.snapshotId()), HttpStatus.BAD_REQUEST, "Chunk snapshot binding mismatch");
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            c.setAutoCommit(false);
            try {
                final Stored stored = required(c, snapshotId, true);
                require(stored.role().equals("IMPORT"), HttpStatus.CONFLICT, "Source artifacts are immutable");
                final Manifest manifest = storedEnvelope(stored).manifest();
                require(chunk.index() >= 0 && chunk.index() < manifest.chunks(), HttpStatus.BAD_REQUEST, "Chunk index outside manifest");
                rows(chunk, manifest.columns());
                final Chunk existing = findChunk(c, snapshotId, chunk.index());
                if (existing != null) {
                    require(existing.sha256().equals(chunk.sha256()) && Arrays.equals(existing.payload(), chunk.payload()),
                            HttpStatus.CONFLICT, "Immutable chunk conflict");
                } else {
                    require(stored.status().equals("STAGING"), HttpStatus.CONFLICT, "Import is already verified");
                    insertChunk(c, chunk);
                }
                c.commit();
                return receipt(stored);
            } catch (SQLException | IOException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public Receipt verifyImport(Database database, Table table, UUID snapshotId) throws SQLException, IOException {
        return finishImport(database, table, snapshotId, null);
    }

    @FunctionalInterface
    public interface SnapshotReconciler {
        void apply(Connection transaction, Database targetDatabase, Table targetTable, Envelope snapshot)
                throws SQLException, IOException;
    }

    public Receipt reconcileImport(Database database, Table table, UUID snapshotId, SnapshotReconciler callback)
            throws SQLException, IOException {
        return finishImport(database, table, snapshotId, Objects.requireNonNull(callback));
    }

    private Receipt finishImport(Database database, Table table, UUID snapshotId, SnapshotReconciler callback)
            throws SQLException, IOException {
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            c.setAutoCommit(false);
            try {
                // Lock lineage before the snapshot row, matching beginImport's lock order.
                final Checkpoint previous = checkpoint(c, binding(table.getId()), true);
                final Stored stored = required(c, snapshotId, true);
                require(stored.role().equals("IMPORT") && stored.tableId().equals(table.getId()), HttpStatus.CONFLICT, "Import target mismatch");
                final Manifest manifest = storedEnvelope(stored).manifest();
                validateTarget(database, table, manifest);
                require(schema(c, table).equals(manifest.columns()), HttpStatus.CONFLICT, "Target schema changed after staging");
                require(previous != null && previous.epoch().equals(manifest.epoch()), HttpStatus.CONFLICT, "Target generation changed");
                if (stored.status().equals("RECONCILED") || (stored.status().equals("VERIFIED") && callback == null)) {
                    c.commit(); return receipt(stored);
                }
                require(manifest.boundary() >= previous.boundary(), HttpStatus.CONFLICT,
                        "An older verified artifact cannot reconcile current state after a newer snapshot");
                if (stored.status().equals("STAGING")) checkTargetCheckpoint(previous, manifest);
                final MessageDigest history = digest();
                final MessageDigest keys = digest();
                long rowCount = 0;
                long keyCount = 0;
                for (long index = 0; index < manifest.chunks(); index++) {
                    final Chunk chunk = readChunk(c, snapshotId, index);
                    final List<Row> rows = rows(chunk, manifest.columns());
                    chunkDigest(history, index, chunk.sha256());
                    rowCount = Math.addExact(rowCount, rows.size());
                    for (int i = 0; i < rows.size(); i++) {
                        if (rows.get(i).current()) {
                            if (stored.status().equals("STAGING")) addKey(c, snapshotId, rows.get(i).replicationKey(), index, i);
                            frame(keys, rows.get(i).replicationKey().getBytes(StandardCharsets.UTF_8));
                            keyCount++;
                        }
                    }
                }
                require(rowCount == manifest.rows() && keyCount == manifest.currentKeys()
                                && HexFormat.of().formatHex(history.digest()).equals(manifest.historyDigest())
                                && HexFormat.of().formatHex(keys.digest()).equals(manifest.currentKeysDigest()),
                        HttpStatus.UNPROCESSABLE_ENTITY, "Staged history does not match the saved source manifest");
                if (manifest.format() >= 2) {
                    for (long index = 0; index < manifest.chunks(); index++) {
                        for (Row row : rows(readChunk(c, snapshotId, index), manifest.columns())) {
                            require(row.masterSiteTs() != null, HttpStatus.CONFLICT, "History version identity is missing");
                            TupleVersionHistory.retain(c, table, row.masterSiteTs(), data(manifest, row));
                            if (row.visibility() != null) {
                                for (var interval : row.visibility()) {
                                    at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationTimestampServiceMariaDbImpl.upsertTimestamp(c, interval);
                                }
                            }
                        }
                    }
                }
                if (callback != null) callback.apply(c, database, table, storedEnvelope(stored));
                execute(c, "UPDATE " + SNAPSHOTS + " SET status=? WHERE snapshot_id=?",
                        callback == null ? "VERIFIED" : "RECONCILED", text(snapshotId));
                if (stored.status().equals("STAGING")) {
                    execute(c, "UPDATE " + LINEAGE + " SET boundary=?, event_id=? WHERE binding=?",
                            manifest.boundary(), text(manifest.boundaryEventId()), binding(table.getId()));
                }
                final Receipt result = receipt(required(c, snapshotId, false));
                c.commit();
                return result;
            } catch (SQLException | IOException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public List<CurrentKey> readCurrentKeys(Connection c, UUID snapshotId, String afterKey, int limit) throws SQLException {
        require(limit > 0 && limit <= MAX_CHUNK_ROWS, HttpStatus.BAD_REQUEST, "Invalid current-key page size");
        final List<CurrentKey> result = new ArrayList<>();
        try (PreparedStatement statement = c.prepareStatement("SELECT replication_key, chunk_index, row_index FROM " + KEYS
                + " WHERE snapshot_id=?" + (afterKey == null ? "" : " AND replication_key > ?") + " ORDER BY replication_key LIMIT ?")) {
            statement.setString(1, text(snapshotId));
            if (afterKey != null) statement.setString(2, afterKey);
            statement.setInt(afterKey == null ? 2 : 3, limit);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new CurrentKey(rows.getString(1), rows.getLong(2), rows.getInt(3)));
            }
        }
        return result;
    }

    public boolean containsCurrentKey(Connection c, UUID snapshotId, String replicationKey) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT 1 FROM " + KEYS
                + " WHERE snapshot_id=? AND replication_key=?")) {
            statement.setString(1, text(snapshotId));
            statement.setString(2, replicationKey);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    public Checkpoint targetCheckpoint(Database database, Table table) throws SQLException {
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            return checkpoint(c, binding(table.getId()), false);
        }
    }

    public Receipt status(Database database, UUID id) throws SQLException, IOException {
        try (var pool = getDataSource(database); Connection c = pool.getConnection()) {
            prepare(c);
            return receipt(required(c, id, false));
        }
    }

    private record Export(long chunks, long rows, long currentKeys, String historyDigest, String currentKeysDigest) { }

    private Export export(Connection reader, Connection writer, Table table, UUID id, List<Column> columns)
            throws SQLException, IOException {
        final String relation = quote(table.getInternalName());
        final String names = String.join(",", columns.stream().map(column -> "h." + quote(column.name())).toList());
        final String rawSize = String.join("+", columns.stream()
                .map(column -> "COALESCE(OCTET_LENGTH(" + quote(column.name()) + "),0)").toList());
        try (PreparedStatement bound = reader.prepareStatement("SELECT 1 FROM " + relation
                + " FOR SYSTEM_TIME ALL WHERE (" + rawSize + ") > " + MAX_CHUNK_BYTES + " LIMIT 1");
             ResultSet oversized = bound.executeQuery()) {
            require(!oversized.next(), HttpStatus.PAYLOAD_TOO_LARGE, "History row exceeds snapshot chunk bound");
        }
        final String query = "SELECT " + names + ",h.ROW_START,h.ROW_END,(c.ROW_START IS NOT NULL) FROM " + relation
                + " FOR SYSTEM_TIME ALL h LEFT JOIN " + relation
                + " c ON h.replication_key=c.replication_key AND h.ROW_START=c.ROW_START AND h.ROW_END=c.ROW_END"
                + " ORDER BY h.replication_key,h.ROW_START,h.ROW_END";
        final MessageDigest history = digest();
        final MessageDigest keys = digest();
        long count = 0, current = 0, index = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.write('[');
        final List<Row> pending = new ArrayList<>();
        final int keyIndex = java.util.stream.IntStream.range(0, columns.size()).filter(i -> columns.get(i).name().equals("replication_key"))
                .findFirst().orElseThrow();
        try (PreparedStatement statement = reader.prepareStatement(query)) {
            statement.setFetchSize(64);
            try (ResultSet source = statement.executeQuery()) {
                while (source.next()) {
                    final List<String> cells = new ArrayList<>();
                    int rowBytes = 0;
                    for (int i = 0; i < columns.size(); i++) {
                        final String value = cell(source, i + 1, columns.get(i));
                        rowBytes += value == null ? 4 : value.getBytes(StandardCharsets.UTF_8).length;
                        require(rowBytes <= MAX_CHUNK_BYTES, HttpStatus.PAYLOAD_TOO_LARGE, "Encoded history row exceeds snapshot chunk bound");
                        cells.add(value);
                    }
                    final String start = period(source, columns.size() + 1);
                    final java.time.Instant version = TupleVersionHistory.findVersion(reader, table.getId(), cells.get(keyIndex),
                            source.getTimestamp(columns.size() + 1, TupleVersionHistory.utc()).toInstant());
                    require(version != null, HttpStatus.CONFLICT, "Native history version mapping is incomplete");
                    final Row row = new Row(cells.get(keyIndex), start,
                            period(source, columns.size() + 2), source.getBoolean(columns.size() + 3), cells,
                            version, TupleVersionHistory.visibility(reader, table, cells.get(keyIndex), version));
                    final byte[] encoded = encode(row);
                    require(encoded.length + 2 <= MAX_CHUNK_BYTES, HttpStatus.PAYLOAD_TOO_LARGE, "History row exceeds snapshot chunk bound");
                    if (!pending.isEmpty() && (pending.size() == MAX_CHUNK_ROWS || bytes.size() + encoded.length + 2 > MAX_CHUNK_BYTES)) {
                        persistExportChunk(writer, id, index++, bytes, pending, columns, history);
                        pending.clear(); bytes = new ByteArrayOutputStream(); bytes.write('[');
                    }
                    if (!pending.isEmpty()) bytes.write(',');
                    bytes.write(encoded);
                    pending.add(row);
                    count++;
                    if (row.current()) {
                        require(row.replicationKey() != null && !row.replicationKey().isBlank(), HttpStatus.CONFLICT, "Current row lacks a replication key");
                        current++; frame(keys, row.replicationKey().getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
        }
        if (!pending.isEmpty()) persistExportChunk(writer, id, index++, bytes, pending, columns, history);
        return new Export(index, count, current, HexFormat.of().formatHex(history.digest()), HexFormat.of().formatHex(keys.digest()));
    }

    private void persistExportChunk(Connection writer, UUID id, long index, ByteArrayOutputStream bytes, List<Row> pending,
                                    List<Column> columns, MessageDigest history) throws SQLException, IOException {
        bytes.write(']');
        final byte[] payload = bytes.toByteArray();
        final Chunk chunk = new Chunk(id, index, sha256(payload), payload);
        rows(chunk, columns);
        writer.setAutoCommit(false);
        try {
            insertChunk(writer, chunk);
            for (int i = 0; i < pending.size(); i++) {
                if (pending.get(i).current()) addKey(writer, id, pending.get(i).replicationKey(), index, i);
            }
            writer.commit();
        } catch (SQLException | RuntimeException e) { writer.rollback(); throw e; }
        finally { writer.setAutoCommit(true); }
        chunkDigest(history, index, chunk.sha256());
    }

    private List<Column> schema(Connection c, Table table) throws SQLException {
        utc(c);
        require(transactionalTables(c, List.of(table.getInternalName())) == 1,
                HttpStatus.CONFLICT, "Native snapshot table must exist and use InnoDB");
        // Pin the table definition for the reader transaction and reject transaction-ID periods.
        try (PreparedStatement pin = c.prepareStatement("SELECT ROW_START,ROW_END FROM " + quote(table.getInternalName())
                + " FOR SYSTEM_TIME ALL LIMIT 0"); ResultSet rows = pin.executeQuery()) {
            require(rows.getMetaData().getColumnType(1) == Types.TIMESTAMP && rows.getMetaData().getColumnType(2) == Types.TIMESTAMP,
                    HttpStatus.CONFLICT, "Only native timestamp-versioned table history is supported");
        }
        final Map<String, String> collations = new LinkedHashMap<>();
        final Map<String, String> types = new HashMap<>();
        try (PreparedStatement columns = c.prepareStatement("SELECT COLUMN_NAME,COLLATION_NAME,COLUMN_TYPE FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? ORDER BY ORDINAL_POSITION")) {
            columns.setString(1, table.getInternalName());
            try (ResultSet rows = columns.executeQuery()) {
                while (rows.next()) {
                    final String name = rows.getString(1);
                    if (!name.equalsIgnoreCase("ROW_START") && !name.equalsIgnoreCase("ROW_END")) {
                        collations.put(name, rows.getString(2));
                        types.put(name, rows.getString(3));
                    }
                    require(collations.size() <= MAX_COLUMNS, HttpStatus.CONFLICT, "Snapshot column limit exceeded");
                }
            }
        }
        require(collations.containsKey("replication_key"), HttpStatus.CONFLICT, "Snapshot requires a replication_key column");
        final List<Column> result = new ArrayList<>();
        try (PreparedStatement inspect = c.prepareStatement("SELECT " + String.join(",", collations.keySet().stream()
                .map(HistorySnapshotService::quote).toList()) + " FROM " + quote(table.getInternalName()) + " LIMIT 0");
             ResultSet rows = inspect.executeQuery()) {
            final ResultSetMetaData meta = rows.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                binary(meta.getColumnType(i));
                result.add(new Column(meta.getColumnName(i), meta.getColumnType(i), types.get(meta.getColumnName(i)),
                        meta.getPrecision(i), meta.getScale(i), meta.isNullable(i) != ResultSetMetaData.columnNoNulls,
                        meta.isSigned(i), collations.get(meta.getColumnName(i))));
            }
        }
        return List.copyOf(result);
    }

    private void validateTarget(Database database, Table table, Manifest manifest) {
        require(manifest.origin() != null && !manifest.origin().isBlank()
                        && ReplicationSites.isReplica(manifest.origin(), baseUrl)
                        && same(database.getCreationLocation(), manifest.origin())
                        && (table.getCreationLocation() == null || table.getCreationLocation().isBlank()
                            || same(table.getCreationLocation(), manifest.origin()))
                        && mapped(database.getReplicaUrls(), manifest.origin(), manifest.sourceDatabaseId())
                        && mapped(table.getReplicaUrls(), manifest.origin(), manifest.sourceTableId()),
                HttpStatus.FORBIDDEN, "Snapshot origin/database/table do not match registered replica metadata");
    }

    private static void validateEnvelope(Envelope envelope) throws IOException {
        require(envelope != null && envelope.manifest() != null, HttpStatus.BAD_REQUEST, "Snapshot manifest required");
        final Manifest m = envelope.manifest();
        require((m.format() == 1 || m.format() == 3) && m.snapshotId() != null && m.sourceDatabaseId() != null && m.sourceTableId() != null
                        && m.epoch() != null && m.boundary() >= 0 && m.legacyThrough() >= 0 && m.legacyThrough() <= m.boundary()
                        && (m.boundary() == 0 ? m.boundaryEventId() == null : m.boundaryEventId() != null)
                        && m.rows() >= 0 && m.currentKeys() >= 0 && m.currentKeys() <= m.rows() && m.chunks() >= 0
                        && (m.chunks() == 0 ? m.rows() == 0 : m.rows() >= m.chunks())
                        && m.chunkRows() == MAX_CHUNK_ROWS && m.chunkBytes() == MAX_CHUNK_BYTES
                        && m.columns() != null && !m.columns().isEmpty() && m.columns().size() <= MAX_COLUMNS
                        && m.historyDigest() != null && m.historyDigest().matches("[0-9a-f]{64}")
                        && m.currentKeysDigest() != null && m.currentKeysDigest().matches("[0-9a-f]{64}"),
                HttpStatus.BAD_REQUEST, "Invalid snapshot manifest bounds or format");
        require(envelope(m).sha256().equals(envelope.sha256()), HttpStatus.UNPROCESSABLE_ENTITY, "Manifest digest mismatch");
    }

    private static void checkTargetCheckpoint(Checkpoint previous, Manifest manifest) {
        require(previous != null && previous.epoch().equals(manifest.epoch()) && manifest.boundary() >= previous.boundary(),
                HttpStatus.CONFLICT, "Snapshot source generation changed or cursor regressed");
        final Checkpoint base = manifest.base();
        if (base == null) {
            require(previous.boundary() == 0 && previous.eventId() == null, HttpStatus.CONFLICT,
                    "Snapshot does not prove the target's previous checkpoint");
        } else {
            require((base.epoch() == null || base.epoch().equals(previous.epoch()))
                            && base.boundary() >= previous.boundary() && base.boundary() <= manifest.boundary()
                            && (base.boundary() == 0 ? base.eventId() == null : base.eventId() != null)
                            && (base.boundary() != previous.boundary() || Objects.equals(base.eventId(), previous.eventId())),
                    HttpStatus.CONFLICT, "Snapshot base predates or contradicts the target checkpoint");
        }
    }

    private static UUID anchor(Connection c, long sequence) throws SQLException {
        if (sequence == 0) return null;
        try (PreparedStatement statement = c.prepareStatement("SELECT id FROM tuple_replication_notification_outbox WHERE event_sequence=?")) {
            statement.setLong(1, sequence);
            try (ResultSet rows = statement.executeQuery()) {
                require(rows.next(), HttpStatus.CONFLICT, "Journal checkpoint event is missing");
                return UUID.fromString(rows.getString(1));
            }
        }
    }

    private static void checkAnchor(Connection reader, Checkpoint checkpoint) throws SQLException {
        require(checkpoint.boundary() >= 0 && Objects.equals(anchor(reader, checkpoint.boundary()), checkpoint.eventId()),
                HttpStatus.CONFLICT, "Source journal checkpoint changed after restore");
    }

    private static Checkpoint checkpoint(Connection c, String binding, boolean lock) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT epoch,boundary,event_id FROM " + LINEAGE
                + " WHERE binding=?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setString(1, binding);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new Checkpoint(UUID.fromString(rows.getString(1)), rows.getLong(2), uuid(rows.getString(3))) : null;
            }
        }
    }

    private record Stored(UUID id, String role, UUID tableId, String status, String manifest, String digest) { }
    private static Stored find(Connection c, UUID id, boolean lock) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT role,table_id,status,manifest,manifest_digest FROM " + SNAPSHOTS
                + " WHERE snapshot_id=?" + (lock ? " FOR UPDATE" : ""))) {
            statement.setString(1, text(id));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? new Stored(id, rows.getString(1), UUID.fromString(rows.getString(2)), rows.getString(3),
                        rows.getString(4), rows.getString(5)) : null;
            }
        }
    }
    private static Stored required(Connection c, UUID id, boolean lock) throws SQLException {
        final Stored stored = find(c, id, lock);
        require(stored != null, HttpStatus.NOT_FOUND, "Snapshot not found");
        return stored;
    }
    private static Envelope storedEnvelope(Stored stored) throws IOException {
        require(stored.manifest() != null, HttpStatus.CONFLICT, "Snapshot manifest is not complete");
        final Envelope result = new Envelope(decode(stored.manifest().getBytes(StandardCharsets.UTF_8), Manifest.class), stored.digest());
        validateEnvelope(result);
        return result;
    }
    private static Receipt receipt(Stored stored) throws IOException {
        final Manifest manifest = stored.manifest() == null ? null : storedEnvelope(stored).manifest();
        return new Receipt(stored.id(), stored.tableId(), stored.status(), manifest == null ? 0 : manifest.boundary(),
                manifest == null ? 0 : manifest.legacyThrough(), manifest == null ? null : stored.digest(),
                Set.of("READY", "VERIFIED", "RECONCILED").contains(stored.status()), stored.status().equals("RECONCILED"));
    }
    public Chunk readChunk(Connection c, UUID id, long index) throws SQLException {
        final Chunk chunk = findChunk(c, id, index);
        require(chunk != null, HttpStatus.CONFLICT, "Required snapshot chunk is missing");
        require(chunk.payload().length <= MAX_CHUNK_BYTES && sha256(chunk.payload()).equals(chunk.sha256()),
                HttpStatus.UNPROCESSABLE_ENTITY, "Stored chunk integrity failure");
        return chunk;
    }
    private static Chunk findChunk(Connection c, UUID id, long index) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT sha256,"
                + "IF(OCTET_LENGTH(payload)<=?,payload,NULL) FROM " + CHUNKS + " WHERE snapshot_id=? AND chunk_index=?")) {
            statement.setInt(1, MAX_CHUNK_BYTES); statement.setString(2, text(id)); statement.setLong(3, index);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return null;
                final byte[] payload = rows.getBytes(2);
                require(payload != null, HttpStatus.UNPROCESSABLE_ENTITY, "Stored chunk exceeds its byte bound");
                return new Chunk(id, index, rows.getString(1), payload);
            }
        }
    }
    private static void insertChunk(Connection c, Chunk chunk) throws SQLException {
        execute(c, "INSERT INTO " + CHUNKS + " (snapshot_id,chunk_index,sha256,payload) VALUES (?,?,?,?)",
                text(chunk.snapshotId()), chunk.index(), chunk.sha256(), chunk.payload());
    }
    private static void addKey(Connection c, UUID id, String key, long chunk, int row) throws SQLException {
        execute(c, "INSERT INTO " + KEYS + " (snapshot_id,replication_key,chunk_index,row_index) VALUES (?,?,?,?)",
                text(id), key, chunk, row);
    }
    private static void prepare(Connection c) throws SQLException {
        require(c.getAutoCommit(), HttpStatus.CONFLICT, "Snapshot schema preparation must precede transactions");
        utc(c);
        final List<String> tables = List.of(SNAPSHOTS, CHUNKS, KEYS, LINEAGE);
        if (transactionalTables(c, tables) == tables.size()) return;
        execute(c, "CREATE TABLE IF NOT EXISTS " + SNAPSHOTS + " (snapshot_id VARCHAR(36) PRIMARY KEY,role VARCHAR(8) NOT NULL,"
                + "table_id VARCHAR(36) NOT NULL,status VARCHAR(16) NOT NULL,manifest LONGTEXT,manifest_digest CHAR(64))"
                + " ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
        execute(c, "CREATE TABLE IF NOT EXISTS " + CHUNKS + " (snapshot_id VARCHAR(36) NOT NULL,chunk_index BIGINT NOT NULL,"
                + "sha256 CHAR(64) NOT NULL,payload LONGBLOB NOT NULL,PRIMARY KEY(snapshot_id,chunk_index)) ENGINE=InnoDB");
        execute(c, "CREATE TABLE IF NOT EXISTS " + KEYS + " (snapshot_id VARCHAR(36) NOT NULL,"
                + "replication_key VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,chunk_index BIGINT NOT NULL,"
                + "row_index INT NOT NULL,PRIMARY KEY(snapshot_id,replication_key)) ENGINE=InnoDB");
        execute(c, "CREATE TABLE IF NOT EXISTS " + LINEAGE + " (binding VARCHAR(64) PRIMARY KEY,epoch VARCHAR(36) NOT NULL,"
                + "boundary BIGINT NOT NULL,event_id VARCHAR(36)) ENGINE=InnoDB");
        require(transactionalTables(c, tables) == tables.size(), HttpStatus.CONFLICT, "Snapshot storage is incomplete");
    }
    private static int transactionalTables(Connection c, List<String> tables) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES"
                + " WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN (" + String.join(",", Collections.nCopies(tables.size(), "?")) + ")")) {
            for (int i = 0; i < tables.size(); i++) statement.setString(i + 1, tables.get(i));
            int count = 0;
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    require("InnoDB".equalsIgnoreCase(rows.getString(2)), HttpStatus.CONFLICT,
                            "Snapshot transaction requires InnoDB: " + rows.getString(1));
                    count++;
                }
            }
            return count;
        }
    }
    private static void execute(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            statement.executeUpdate();
        }
    }
    private static int advisoryLock(Connection c, String function, String name) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement("SELECT " + function + "(?"
                + (function.equals("GET_LOCK") ? ",1" : "") + ")")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { return rows.next() ? rows.getInt(1) : 0; }
        }
    }
    private static String quote(String name) { return "`" + name.replace("`", "``") + "`"; }
    private static String text(UUID value) { return value == null ? null : value.toString(); }
    private static UUID uuid(String value) { return value == null ? null : UUID.fromString(value); }
    private static String binding(UUID table) { return "target:" + table; }
    private static void utc(Connection c) throws SQLException { execute(c, "SET time_zone='+00:00'"); }
    private static boolean same(String a, String b) { return a != null && !a.isBlank() && !ReplicationSites.isReplica(a, b); }
    private static boolean mapped(Map<String, UUID> mapping, String origin, UUID id) {
        return mapping != null && mapping.entrySet().stream().anyMatch(entry -> same(entry.getKey(), origin) && id.equals(entry.getValue()));
    }
    private static void require(boolean condition, HttpStatus status, String message) {
        if (!condition) throw new ResponseStatusException(status, message);
    }
}
