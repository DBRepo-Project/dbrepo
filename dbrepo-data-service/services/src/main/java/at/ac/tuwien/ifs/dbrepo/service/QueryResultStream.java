package at.ac.tuwien.ifs.dbrepo.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An executed query whose rows are fetched from the database while they are written to the response. Holds the
 * connection open until {@link #close()}, which both write methods call when they finish, fail or the client goes
 * away.
 */
@Slf4j
public class QueryResultStream implements AutoCloseable {

    /* the format Spark used to write timestamps when results were staged in the storage service */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final Connection connection;
    private final ResultSet resultSet;
    private final int[] types;
    private final String[] typeNames;
    private final int[] precisions;

    @Getter
    private final List<String> columns;

    public QueryResultStream(Connection connection, ResultSet resultSet) throws SQLException {
        this.connection = connection;
        this.resultSet = resultSet;
        final ResultSetMetaData metadata = resultSet.getMetaData();
        final int count = metadata.getColumnCount();
        this.types = new int[count];
        this.typeNames = new String[count];
        this.precisions = new int[count];
        this.columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            types[i] = metadata.getColumnType(i + 1);
            typeNames[i] = metadata.getColumnTypeName(i + 1);
            precisions[i] = metadata.getPrecision(i + 1);
            columns.add(metadata.getColumnLabel(i + 1));
        }
    }

    /**
     * Writes the rows as CSV with the given header line and closes the result.
     *
     * @param out    The output stream, e.g. the response body.
     * @param header The header column names.
     */
    public void writeCsv(OutputStream out, List<String> header) throws IOException {
        try {
            /* not closed, that would close the response stream */
            final Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
            writer.write(header.stream()
                    .map(QueryResultStream::escapeCsv)
                    .collect(Collectors.joining(",")));
            writer.write(System.lineSeparator());
            while (resultSet.next()) {
                for (int i = 0; i < types.length; i++) {
                    if (i > 0) {
                        writer.write(',');
                    }
                    final Object value = getValue(i);
                    writer.write(escapeCsv(value == null ? "" : toCsvString(value)));
                }
                writer.write(System.lineSeparator());
            }
            writer.flush();
        } catch (SQLException e) {
            throw new IOException("Failed to read row: " + e.getMessage(), e);
        } finally {
            close();
        }
    }

    /**
     * Writes the rows as JSON array of objects (column name to value) and closes the result.
     *
     * @param out The output stream, e.g. the response body.
     */
    public void writeJson(OutputStream out) throws IOException {
        try {
            final JsonGenerator generator = JSON_FACTORY.createGenerator(out);
            generator.writeStartArray();
            while (resultSet.next()) {
                generator.writeStartObject();
                for (int i = 0; i < types.length; i++) {
                    generator.writeFieldName(columns.get(i));
                    writeJsonValue(generator, getValue(i));
                }
                generator.writeEndObject();
            }
            generator.writeEndArray();
            generator.flush();
        } catch (SQLException e) {
            throw new IOException("Failed to read row: " + e.getMessage(), e);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Failed to close connection: {}", e.getMessage());
        }
    }

    /**
     * Maps the value of the current row in the column with zero-based index to a JSON-compatible Java value.
     */
    Object getValue(int i) throws SQLException {
        final int column = i + 1;
        final Object value;
        switch (types[i]) {
            case Types.BIT -> value = precisions[i] > 1 ? (Object) resultSet.getLong(column) : resultSet.getBoolean(column);
            case Types.BOOLEAN -> value = resultSet.getBoolean(column);
            case Types.DECIMAL, Types.NUMERIC -> value = resultSet.getBigDecimal(column);
            case Types.DATE -> value = "YEAR".equalsIgnoreCase(typeNames[i]) ? (Object) resultSet.getInt(column) : resultSet.getString(column);
            case Types.TIMESTAMP -> {
                final LocalDateTime timestamp = resultSet.getObject(column, LocalDateTime.class);
                value = timestamp == null ? null : TIMESTAMP_FORMATTER.format(timestamp);
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                final byte[] bytes = resultSet.getBytes(column);
                value = bytes == null ? null : Base64.getEncoder().encodeToString(bytes);
            }
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.REAL, Types.FLOAT, Types.DOUBLE ->
                    value = resultSet.getObject(column);
            default -> value = resultSet.getString(column);
        }
        return resultSet.wasNull() ? null : value;
    }

    private static String toCsvString(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        return String.valueOf(value);
    }

    private static void writeJsonValue(JsonGenerator generator, Object value) throws IOException {
        switch (value) {
            case null -> generator.writeNull();
            case Boolean b -> generator.writeBoolean(b);
            case BigDecimal d -> generator.writeNumber(d);
            case BigInteger n -> generator.writeNumber(n);
            case Float f -> generator.writeNumber(f);
            case Double d -> generator.writeNumber(d);
            case Number n -> generator.writeNumber(n.longValue());
            default -> generator.writeString(value.toString());
        }
    }

    static String escapeCsv(String value) {
        if (value.contains("\"") || value.contains(",") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

}
