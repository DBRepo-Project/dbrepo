package at.tuwien.mapper;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import org.apache.commons.io.FileUtils;
import org.mapstruct.Mapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataMapper.class);

    default PreparedStatement rabbitMqTupleToInsertOrUpdateQuery(Connection connection, Table table,
                                                                 Map<String, Object> data) throws SQLException {
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(table.getInternalName())
                .append("` (")
                .append(data.keySet()
                        .stream()
                        .map(column -> "`" + column + "`")
                        .collect(Collectors.joining(",")))
                .append(") VALUES (");
        final int[] idx = new int[]{1, 0, 1};
        data.values()
                .forEach(c -> statement.append(idx[1]++ > 0 ? "," : "")
                        .append("?"));
        statement.append(");");
        log.trace("generated statement: {}", statement);
        final PreparedStatement preparedStatement = connection.prepareStatement(statement.toString());
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            final Optional<TableColumn> optional = table.getColumns().stream().filter(c -> c.getInternalName().equals(entry.getKey())).findFirst();
            if (optional.isEmpty()) {
                log.error("Failed to find column with name {} in table with name {}, available columns are {}", entry.getKey(), table.getInternalName(), table.getColumns().stream().map(TableColumn::getInternalName).toList());
                continue;
            }
            prepareStatementWithColumnTypeObject(preparedStatement, optional.get().getColumnType(), idx[2]++,
                    entry.getValue());
        }
        return preparedStatement;
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, TableColumnType columnType, int idx, Object value) throws SQLException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                log.trace("prepare statement idx {} blob", idx);
                if (value == null) {
                    ps.setNull(idx, Types.BLOB);
                    break;
                }
                try {
                    ps.setBlob(idx, FileUtils.openInputStream(new File(String.valueOf(value))));
                } catch (IOException e) {
                    log.error("Failed to set blob: {}", e.getMessage());
                    throw new SQLException("Failed to set blob: " + e.getMessage(), e);
                }
                break;
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.VARCHAR);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            case DATE:
                log.trace("prepare statement idx {} date {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DATE);
                    break;
                }
                ps.setDate(idx, Date.valueOf(String.valueOf(value)));
                break;
            case BIGINT:
                log.trace("prepare statement idx {} bigint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.BIGINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.INTEGER);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case TINYINT:
                log.trace("prepare statement idx {} tinyint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TINYINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case SMALLINT:
                log.trace("prepare statement idx {} smallint {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.SMALLINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case DECIMAL:
                log.trace("prepare statement idx {} decimal {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case FLOAT:
                log.trace("prepare statement idx {} float {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.FLOAT);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case DOUBLE:
                log.trace("prepare statement idx {} double {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DOUBLE);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                log.trace("prepare statement idx {} {} {}", idx, columnType, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
                log.trace("prepare statement idx {} boolean {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.BOOLEAN);
                    break;
                }
                ps.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIMESTAMP:
                log.trace("prepare statement idx {} timestamp {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                ps.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
                break;
            case DATETIME:
                log.trace("prepare statement idx {} datetime {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                ps.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
                break;
            case TIME:
                log.trace("prepare statement idx {} time {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIME);
                    break;
                }
                ps.setTime(idx, Time.valueOf(String.valueOf(value)));
                break;
            case YEAR:
                log.trace("prepare statement idx {} year {}", idx, value);
                if (value == null) {
                    log.trace("idx {} is null, prepare with null value", idx);
                    ps.setNull(idx, Types.TIME);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            default:
                log.error("Failed to map column type {} at index {} for value {}", columnType, idx, value);
                throw new IllegalArgumentException("Failed to map column type " + columnType);
        }
    }

}
