package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;

public final class HistorySnapshotCodec {
    public static final int MAX_CHUNK_BYTES = 4 * 1024 * 1024;
    public static final int MAX_CHUNK_ROWS = 256;
    public static final int MAX_COLUMNS = 1024;
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSS")
            .withResolverStyle(ResolverStyle.STRICT);
    private HistorySnapshotCodec() { }

    public static byte[] encode(Object value) throws IOException { return JSON.writeValueAsBytes(value); }
    public static <T> T decode(byte[] value, Class<T> type) throws IOException { return JSON.readValue(value, type); }
    public static String sha256(byte[] value) { return HexFormat.of().formatHex(digest().digest(value)); }
    public static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static void frame(MessageDigest digest, byte[] value) {
        digest.update(ByteBuffer.allocate(4).putInt(value.length).array());
        digest.update(value);
    }
    public static void chunkDigest(MessageDigest digest, long index, String hash) {
        digest.update(ByteBuffer.allocate(8).putLong(index).array());
        frame(digest, hash.getBytes(StandardCharsets.US_ASCII));
    }
    public static Envelope envelope(Manifest manifest) throws IOException {
        return new Envelope(manifest, sha256(encode(manifest)));
    }
    public static boolean binary(int type) {
        return switch (type) {
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB, Types.BIT -> true;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                 Types.CLOB, Types.NCLOB, Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.NUMERIC,
                 Types.DECIMAL, Types.FLOAT, Types.REAL, Types.DOUBLE, Types.BOOLEAN, Types.DATE, Types.TIME,
                 Types.TIMESTAMP -> false;
            default -> throw new IllegalArgumentException("Unsupported snapshot JDBC type: " + type);
        };
    }
    public static String cell(ResultSet row, int index, Column column) throws SQLException, IOException {
        if (column.jdbcType() == Types.BIT) {
            final byte[] bits = row.getBytes(index);
            return bits == null ? null : Base64.getEncoder().encodeToString(bits);
        }
        if (binary(column.jdbcType())) {
            try (InputStream stream = row.getBinaryStream(index)) {
                if (stream == null) return null;
                final byte[] bytes = stream.readNBytes(MAX_CHUNK_BYTES + 1);
                if (bytes.length > MAX_CHUNK_BYTES) throw new IOException("Snapshot cell exceeds bounded chunk size");
                return Base64.getEncoder().encodeToString(bytes);
            }
        }
        if (switch (column.jdbcType()) {
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR,
                 Types.CLOB, Types.NCLOB -> false;
            default -> true;
        }) return row.getString(index);
        try (Reader reader = row.getCharacterStream(index)) {
            if (reader == null) return null;
            final StringBuilder value = new StringBuilder();
            final char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (value.length() + read > MAX_CHUNK_BYTES) throw new IOException("Snapshot cell exceeds bounded chunk size");
                value.append(buffer, 0, read);
            }
            return value.toString();
        }
    }
    public static String period(ResultSet rows, int index) throws SQLException {
        final Timestamp timestamp = rows.getTimestamp(index, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        if (timestamp == null) throw new SQLException("Missing native system-time period");
        return PERIOD.format(LocalDateTime.ofInstant(timestamp.toInstant(), java.time.ZoneOffset.UTC));
    }
    public static List<Row> rows(Chunk chunk, List<Column> columns) throws IOException {
        if (chunk == null || chunk.payload() == null || chunk.payload().length > MAX_CHUNK_BYTES
                || !sha256(chunk.payload()).equals(chunk.sha256())) throw new IOException("Invalid snapshot chunk digest or size");
        final List<Row> rows = JSON.readValue(chunk.payload(), new TypeReference<>() { });
        if (rows == null || rows.isEmpty() || rows.size() > MAX_CHUNK_ROWS) throw new IOException("Invalid chunk row count");
        final int key = java.util.stream.IntStream.range(0, columns.size())
                .filter(i -> columns.get(i).name().equals("replication_key")).findFirst().orElseThrow();
        for (Row row : rows) {
            try {
                if (row == null || row.cells() == null || row.cells().size() != columns.size()
                        || !Objects.equals(row.replicationKey(), row.cells().get(key))) throw new IOException("Snapshot row schema/key mismatch");
                if (row.visibility() != null) {
                    for (var interval : row.visibility()) {
                        if (row.versionId() == null || !row.versionId().equals(interval.getVersionId())
                                || !Objects.equals(row.replicationKey(), interval.getReplicationId())
                                || interval.getSiteUrl() == null || interval.getDatabaseId() == null || interval.getTableId() == null
                                || interval.getRowStart() == null || (interval.getRowEnd() != null
                                    && interval.getRowEnd().isBefore(interval.getRowStart()))) {
                            throw new IOException("History visibility does not identify its values version");
                        }
                    }
                }
                final LocalDateTime start = LocalDateTime.parse(row.rowStart(), PERIOD);
                final LocalDateTime end = LocalDateTime.parse(row.rowEnd(), PERIOD);
                if (end.isBefore(start)) throw new IOException("Reversed native history period");
                if (row.current() && (row.replicationKey() == null || row.replicationKey().isBlank()
                        || row.replicationKey().length() > 255)) throw new IOException("Invalid current replication key");
                for (int i = 0; i < columns.size(); i++) {
                    if (row.cells().get(i) != null && binary(columns.get(i).jdbcType())) {
                        Base64.getDecoder().decode(row.cells().get(i));
                    }
                }
            } catch (RuntimeException e) { throw new IOException("Invalid snapshot row", e); }
        }
        return rows;
    }
    /** Values suitable for typed JDBC binding: binary bytes, otherwise lossless SQL text, including NULL. */
    public static Map<String, Object> data(Manifest manifest, Row row) {
        final Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < manifest.columns().size(); i++) {
            final Column column = manifest.columns().get(i);
            final String value = row.cells().get(i);
            values.put(column.name(), value != null && binary(column.jdbcType()) ? Base64.getDecoder().decode(value) : value);
        }
        return values;
    }
}
