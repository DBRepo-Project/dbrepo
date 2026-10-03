package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Column;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Holds the verified repeatable-read snapshot until its last response byte is written. */
public final class SubsetResultReader implements AutoCloseable {
    private final ComboPooledDataSource pool;
    private final Connection connection;
    private final UUID queryId;
    private final long count;
    private final List<Column> columns;
    private final AtomicBoolean closed = new AtomicBoolean();

    SubsetResultReader(ComboPooledDataSource pool, Connection connection, UUID queryId, long count, List<Column> columns) {
        this.pool = pool;
        this.connection = connection;
        this.queryId = queryId;
        this.count = count;
        this.columns = columns;
    }

    public List<String> names() { return columns.stream().map(Column::name).toList(); }

    public void json(OutputStream output, long offset, long limit) throws IOException {
        if (offset < 0 || limit < 0) throw new IllegalArgumentException("Invalid result page");
        try (JsonGenerator json = new JsonFactory().createGenerator(output)) {
            json.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            json.writeStartArray();
            for (long row = offset; row < count && row - offset < limit; row++) {
                final Cells cells = new Cells(new RowInput(row));
                json.writeStartObject();
                for (Column column : columns) {
                    json.writeFieldName(column.name());
                    InputStream value = cells.next();
                    if (value == null) json.writeNull();
                    else if (binary(column)) json.writeBinary(value, -1);
                    else if (numeric(column)) {
                        byte[] number = value.readNBytes(129);
                        if (number.length > 128) throw new IOException("Invalid numeric cell length");
                        String text = new String(number, StandardCharsets.US_ASCII);
                        // Parse before emitting a raw JSON number; no floating-point conversion.
                        json.writeNumber(new java.math.BigDecimal(text));
                    } else json.writeString(text(value, column), -1);
                }
                cells.end();
                json.writeEndObject();
            }
            json.writeEndArray();
        }
    }

    public void csv(OutputStream output) throws IOException {
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) writer.write(',');
            quoted(writer, new StringReader(columns.get(i).name()));
        }
        writer.write('\n');
        for (long row = 0; row < count; row++) {
            Cells cells = new Cells(new RowInput(row));
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) writer.write(',');
                InputStream value = cells.next();
                if (value != null) {
                    Column column = columns.get(i);
                    if (binary(column)) {
                        writer.write('"');
                        // Hex is lossless and does not require buffering a binary cell.
                        int b;
                        while ((b = value.read()) != -1) {
                            writer.write(Character.forDigit(b >>> 4, 16));
                            writer.write(Character.forDigit(b & 15, 16));
                        }
                        writer.write('"');
                    } else quoted(writer, text(value, column));
                }
            }
            cells.end();
            writer.write('\n');
        }
        writer.flush();
    }

    private static void quoted(Writer output, Reader value) throws IOException {
        output.write('"');
        char[] buffer = new char[8192];
        int size;
        while ((size = value.read(buffer)) != -1) {
            for (int i = 0; i < size; i++) {
                if (buffer[i] == '"') output.write('"');
                output.write(buffer[i]);
            }
        }
        output.write('"');
    }

    static void validateColumn(Column column) {
        if (!binary(column)) charset(column);
    }

    private static Reader text(InputStream value, Column column) {
        return new InputStreamReader(value, charset(column).newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));
    }

    private static boolean binary(Column column) {
        return Set.of("binary", "varbinary", "tinyblob", "blob", "mediumblob", "longblob", "bit",
                "geometry", "point", "linestring", "polygon", "multipoint", "multilinestring", "multipolygon",
                "geometrycollection").contains(column.dataType());
    }

    private static boolean numeric(Column column) {
        return Set.of("tinyint", "smallint", "mediumint", "int", "bigint", "decimal", "float", "double", "year")
                .contains(column.dataType());
    }

    private static Charset charset(Column column) {
        return switch (column.charset() == null ? "ascii" : column.charset()) {
            case "utf8mb4", "utf8mb3", "utf8" -> StandardCharsets.UTF_8;
            case "ascii" -> StandardCharsets.US_ASCII;
            case "latin1" -> Charset.forName("windows-1252");
            case "utf16", "ucs2" -> StandardCharsets.UTF_16BE;
            case "utf16le" -> StandardCharsets.UTF_16LE;
            case "utf32" -> Charset.forName("UTF-32BE");
            default -> throw SubsetResultService.conflict("Unsupported snapshot character set: " + column.charset());
        };
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { connection.rollback(); }
        catch (SQLException ignored) { /* Closing the pool also discards the transaction. */ }
        try { connection.close(); }
        catch (SQLException ignored) { }
        finally { pool.close(); }
    }

    private final class RowInput extends InputStream {
        private final long row;
        private long offset;
        private byte[] buffer = new byte[0];
        private int position;

        private RowInput(long row) { this.row = row; }

        @Override
        public int read() throws IOException {
            if (position == buffer.length) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT SUBSTRING(payload,?,?) FROM qs_subset_result_rows WHERE query_id=? AND row_no=?
                        """)) {
                    statement.setLong(1, offset + 1);
                    statement.setInt(2, SubsetResultService.CHUNK_SIZE);
                    statement.setString(3, queryId.toString());
                    statement.setLong(4, row);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IOException("Verified snapshot row disappeared");
                        buffer = result.getBytes(1);
                    }
                } catch (SQLException e) { throw new IOException("Cannot read immutable result", e); }
                position = 0;
                offset += buffer.length;
                if (buffer.length == 0) return -1;
            }
            return buffer[position++] & 255;
        }
    }

    /** The canonical alphabet is deliberately smaller than JSON: only N or V followed by uppercase hex. */
    private static final class Cells {
        private final InputStream row;
        private boolean first = true;

        Cells(InputStream row) throws IOException { this.row = row; expect('['); }

        InputStream next() throws IOException {
            if (!first) expect(',');
            first = false;
            expect('"');
            int tag = row.read();
            if (tag == 'N') { expect('"'); return null; }
            if (tag != 'V') throw new IOException("Invalid canonical cell tag");
            return new InputStream() {
                private boolean done;
                @Override
                public int read() throws IOException {
                    if (done) return -1;
                    int high = row.read();
                    if (high == '"') { done = true; return -1; }
                    int low = row.read();
                    int a = Character.digit(high, 16), b = Character.digit(low, 16);
                    if (a < 0 || b < 0) throw new IOException("Invalid canonical cell hex");
                    return a * 16 + b;
                }
            };
        }

        void end() throws IOException { expect(']'); if (row.read() != -1) throw new IOException("Extra canonical cells"); }

        private void expect(int expected) throws IOException {
            int value;
            do { value = row.read(); } while (value == ' ');
            if (value != expected) throw new IOException("Invalid canonical row structure");
        }
    }
}
