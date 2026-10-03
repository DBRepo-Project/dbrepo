package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Column;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Progress;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryExecutionException;
import at.ac.tuwien.ifs.dbrepo.mapper.MariaDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataConnector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubsetResultService extends DataConnector {
    public static final int CHUNK_SIZE = 65536;
    private final MariaDbMapper mapper;
    private final ObjectMapper json;

    public SubsetResultService(MariaDbMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    public void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(mapper.queryStoreCreateResultsRawQuery());
            statement.execute(mapper.queryStoreCreateResultRowsRawQuery());
            statement.execute(mapper.queryStoreCreateCaptureResultProcedureRawQuery()
                    .replaceFirst("CREATE PROCEDURE", "CREATE PROCEDURE IF NOT EXISTS"));
        }
    }

    public SubsetResultManifestDto manifest(Connection connection, SubsetReplicationDto query) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_json,order_hash,snapshot_hash,ready FROM qs_subset_results WHERE query_id=?")) {
            statement.setString(1, query.queryId().toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !row.getBoolean("ready") || !Objects.equals(query.snapshotHash(), row.getString("snapshot_hash"))) {
                    throw conflict("Subset result is missing or not published");
                }
                return new SubsetResultManifestDto(query, row.getString("schema_json"), row.getString("order_hash"));
            }
        }
    }

    public Progress begin(Database database, SubsetResultManifestDto manifest) throws SQLException {
        final var query = manifest.query();
        final List<Column> columns = columns(manifest.schemaJson());
        if (columns.isEmpty() || !snapshotHash(manifest).equals(query.snapshotHash())) {
            throw conflict("Snapshot manifest does not match the canonical query reference");
        }
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireQuery(connection, query.queryId(), query.snapshotHash(), query.resultHash(), query.resultCount());
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT * FROM qs_subset_results WHERE query_id=? FOR UPDATE")) {
                    statement.setString(1, query.queryId().toString());
                    try (ResultSet row = statement.executeQuery()) {
                        if (row.next()) {
                            if (!query.snapshotHash().equals(row.getString("snapshot_hash"))
                                    || !manifest.schemaJson().equals(row.getString("schema_json"))
                                    || !manifest.orderHash().equals(row.getString("order_hash"))) {
                                throw conflict("Existing immutable snapshot manifest conflicts");
                            }
                            final var progress = progress(row);
                            connection.commit();
                            return progress;
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO qs_subset_results(query_id,schema_json,order_hash,snapshot_hash) VALUES (?,?,?,?)
                        """)) {
                    statement.setString(1, query.queryId().toString());
                    statement.setString(2, manifest.schemaJson());
                    statement.setString(3, manifest.orderHash());
                    statement.setString(4, query.snapshotHash());
                    statement.executeUpdate();
                }
                connection.commit();
                return new Progress(false, 0, 0);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            pool.close();
        }
    }

    public Progress append(Database database, UUID queryId, long rowNo, long offset, long rowLength,
                           String rowHash, String chunkHash, byte[] bytes) throws SQLException {
        if (rowNo < 0 || offset < 0 || rowLength < 2 || rowLength > 4294967295L || bytes.length == 0
                || bytes.length > CHUNK_SIZE || offset > rowLength - bytes.length
                || !isDigest(rowHash) || !Objects.equals(chunkHash, sha256(bytes))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid result chunk");
        }
        for (byte value : bytes) {
            if (value < 0 || !(value == '[' || value == ']' || value == '"' || value == ',' || value == ' '
                    || value == 'N' || value == 'V' || value >= '0' && value <= '9' || value >= 'A' && value <= 'F')) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Result chunks must use canonical cell encoding");
            }
        }
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                final Progress progress;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT r.*,q.result_number FROM qs_subset_results r JOIN qs_queries q ON q.id=r.query_id
                        WHERE r.query_id=? FOR UPDATE
                        """)) {
                    statement.setString(1, queryId.toString());
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next() || rowNo >= row.getLong("result_number")) throw conflict("Missing manifest or invalid row number");
                        progress = progress(row);
                    }
                }
                if (rowNo < progress.nextRow() || rowNo == progress.nextRow() && offset < progress.nextOffset()) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            SELECT row_hash,byte_length,SHA2(SUBSTRING(payload,?,?),256) AS chunk_hash
                            FROM qs_subset_result_rows WHERE query_id=? AND row_no=?
                            """)) {
                        statement.setLong(1, offset + 1);
                        statement.setInt(2, bytes.length);
                        statement.setString(3, queryId.toString());
                        statement.setLong(4, rowNo);
                        try (ResultSet row = statement.executeQuery()) {
                            if (!row.next() || !rowHash.equals(row.getString(1)) || rowLength != row.getLong(2)
                                    || !chunkHash.equals(row.getString(3))) throw conflict("Conflicting immutable result chunk");
                        }
                    }
                    connection.commit();
                    return progress;
                }
                if (progress.ready() || rowNo != progress.nextRow() || offset != progress.nextOffset()) {
                    throw conflict("Result chunk is not the next staging position");
                }
                if (offset == 0) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO qs_subset_result_rows(query_id,row_no,row_hash,byte_length,payload) VALUES (?,?,?,?,?)
                            """)) {
                        statement.setString(1, queryId.toString());
                        statement.setLong(2, rowNo);
                        statement.setString(3, rowHash);
                        statement.setLong(4, rowLength);
                        statement.setString(5, new String(bytes, StandardCharsets.US_ASCII));
                        statement.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE qs_subset_result_rows SET payload=CONCAT(payload,?)
                            WHERE query_id=? AND row_no=? AND row_hash=? AND byte_length=? AND OCTET_LENGTH(payload)=?
                            """)) {
                        statement.setString(1, new String(bytes, StandardCharsets.US_ASCII));
                        statement.setString(2, queryId.toString());
                        statement.setLong(3, rowNo);
                        statement.setString(4, rowHash);
                        statement.setLong(5, rowLength);
                        statement.setLong(6, offset);
                        if (statement.executeUpdate() != 1) throw conflict("Result row staging state conflicts");
                    }
                }
                final boolean complete = offset + bytes.length == rowLength;
                if (complete) {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            SELECT SHA2(payload,256),JSON_VALID(payload) FROM qs_subset_result_rows WHERE query_id=? AND row_no=?
                            """)) {
                        statement.setString(1, queryId.toString());
                        statement.setLong(2, rowNo);
                        try (ResultSet row = statement.executeQuery()) {
                            if (!row.next() || !rowHash.equals(row.getString(1)) || !row.getBoolean(2)) {
                                throw conflict("Completed result row fails checksum or encoding validation");
                            }
                        }
                    }
                }
                final var next = new Progress(false, complete ? rowNo + 1 : rowNo, complete ? 0 : offset + bytes.length);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE qs_subset_results SET next_row=?,next_offset=? WHERE query_id=? AND ready=FALSE")) {
                    statement.setLong(1, next.nextRow());
                    statement.setLong(2, next.nextOffset());
                    statement.setString(3, queryId.toString());
                    statement.executeUpdate();
                }
                connection.commit();
                return next;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            pool.close();
        }
    }

    public void publish(Database database, UUID queryId) throws SQLException {
        final var pool = getDataSource(database);
        try (Connection connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                final Subset subset;
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT q.*,r.next_row,r.next_offset FROM qs_queries q JOIN qs_subset_results r ON q.id=r.query_id
                        WHERE q.id=? FOR UPDATE
                        """)) {
                    statement.setString(1, queryId.toString());
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next() || row.getLong("next_row") != row.getLong("result_number")
                                || row.getLong("next_offset") != 0) throw conflict("Result staging is incomplete");
                        subset = Subset.builder().id(queryId).snapshotHash(row.getString("snapshot_hash"))
                                .resultHash(row.getString("result_hash")).resultNumber(row.getLong("result_number")).build();
                    }
                }
                verify(connection, subset, false);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE qs_subset_results SET ready=TRUE WHERE query_id=? AND ready=FALSE")) {
                    statement.setString(1, queryId.toString());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } finally {
            pool.close();
        }
    }

    public SubsetResultReader open(Database database, Subset subset) throws SQLException, QueryExecutionException {
        final var pool = getDataSource(database);
        Connection connection = null;
        try {
            connection = pool.getConnection();
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            final List<Column> columns = verify(connection, subset, true);
            return new SubsetResultReader(pool, connection, subset.getId(), subset.getResultNumber(), columns);
        } catch (SQLException | RuntimeException e) {
            try { if (connection != null) connection.close(); }
            catch (SQLException closeFailure) { e.addSuppressed(closeFailure); }
            finally { pool.close(); }
            throw new QueryExecutionException("Immutable subset result is unavailable or failed verification", e);
        }
    }

    private List<Column> verify(Connection connection, Subset subset, boolean requireReady) throws SQLException {
        final String schema;
        final String orderHash;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM qs_subset_results WHERE query_id=?")) {
            statement.setString(1, subset.getId().toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || requireReady && !row.getBoolean("ready")
                        || !Objects.equals(subset.getSnapshotHash(), row.getString("snapshot_hash"))) {
                    throw conflict("Missing or unpublished immutable subset result");
                }
                schema = row.getString("schema_json");
                orderHash = row.getString("order_hash");
            }
        }
        final List<Column> columns = columns(schema);
        final String schemaEncoding = columns.stream().map(c -> hex(c.name()) + ":" + hex(c.columnType()))
                .collect(Collectors.joining(";"));
        String digest = sha256("dbrepo:rows:v2:" + schemaEncoding);
        String ordered = sha256("dbrepo:subset-order:v1:");
        long count = 0;
        String lastHash = "";
        long lastRow = -1;
        while (true) {
            int fetched = 0;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT row_no,row_hash,SHA2(payload,256) AS actual_hash,byte_length,OCTET_LENGTH(payload) AS actual_length
                    FROM qs_subset_result_rows WHERE query_id=? AND (row_hash>? OR (row_hash=? AND row_no>?))
                    ORDER BY row_hash,row_no LIMIT 256
                    """)) {
                statement.setString(1, subset.getId().toString());
                statement.setString(2, lastHash);
                statement.setString(3, lastHash);
                statement.setLong(4, lastRow);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        lastHash = rows.getString("row_hash");
                        lastRow = rows.getLong("row_no");
                        if (!lastHash.equals(rows.getString("actual_hash"))
                                || rows.getLong("byte_length") != rows.getLong("actual_length")) throw conflict("Corrupted result row");
                        digest = sha256(digest + lastHash);
                        fetched++;
                        count++;
                    }
                }
            }
            if (fetched == 0) break;
        }
        if (count != subset.getResultNumber() || !("v2:" + digest).equals(subset.getResultHash())) {
            throw conflict("Snapshot does not reproduce the original result hash and count");
        }
        for (long offset = 0; offset < count;) {
            final long before = offset;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT row_no,row_hash FROM qs_subset_result_rows WHERE query_id=? AND row_no>=? ORDER BY row_no LIMIT 256
                    """)) {
                statement.setString(1, subset.getId().toString());
                statement.setLong(2, offset);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        if (rows.getLong(1) != offset++) throw conflict("Snapshot row sequence is incomplete");
                        ordered = sha256(ordered + rows.getString(2));
                    }
                }
            }
            if (offset == before) throw conflict("Snapshot row sequence is incomplete");
        }
        if (!ordered.equals(orderHash) || !sha256("dbrepo:subset-artifact:v1:" + schema + ":" + ordered + ":"
                + subset.getResultHash() + ":" + count).equals(subset.getSnapshotHash())) {
            throw conflict("Snapshot schema or captured order differs from the original manifest");
        }
        return columns;
    }

    public List<Column> columns(String schema) {
        if (schema == null || schema.getBytes(StandardCharsets.UTF_8).length > 1048576) throw conflict("Invalid result schema size");
        try {
            final List<Column> columns = json.readValue(schema, new TypeReference<>() { });
            if (columns == null || columns.isEmpty() || columns.size() > 4096
                    || columns.stream().anyMatch(c -> c == null || c.name() == null || c.columnType() == null || c.dataType() == null)
                    || columns.stream().map(Column::name).distinct().count() != columns.size()) throw conflict("Invalid result schema");
            columns.forEach(SubsetResultReader::validateColumn);
            return columns;
        } catch (IOException e) {
            throw conflict("Invalid result schema JSON");
        }
    }

    private void requireQuery(Connection connection, UUID id, String snapshotHash, String resultHash, long resultCount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM qs_queries WHERE id=? FOR UPDATE")) {
            statement.setString(1, id.toString());
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next() || !Objects.equals(snapshotHash, row.getString("snapshot_hash"))
                        || !Objects.equals(resultHash, row.getString("result_hash")) || resultCount != row.getLong("result_number")) {
                    throw conflict("Result manifest conflicts with the canonical query");
                }
            }
        }
    }

    private static Progress progress(ResultSet row) throws SQLException {
        return new Progress(row.getBoolean("ready"), row.getLong("next_row"), row.getLong("next_offset"));
    }

    private static String snapshotHash(SubsetResultManifestDto manifest) {
        return sha256("dbrepo:subset-artifact:v1:" + manifest.schemaJson() + ":" + manifest.orderHash() + ":"
                + manifest.query().resultHash() + ":" + manifest.query().resultCount());
    }

    static String sha256(String value) { return sha256(value.getBytes(StandardCharsets.UTF_8)); }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(String value) { return HexFormat.of().withUpperCase().formatHex(value.getBytes(StandardCharsets.UTF_8)); }
    private static boolean isDigest(String value) { return value != null && value.matches("[0-9a-f]{64}"); }
    static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
