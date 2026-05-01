package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.ConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.primary.PrimaryKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.unique.UniqueDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.ColumnType;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Subset;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.duckdb.DuckDBResultSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.sql.Types.*;
import static java.sql.Types.REAL;

@Mapper(componentModel = "spring")
public interface DataMapper {

    Logger log = LoggerFactory.getLogger(DataMapper.class);

    /**
     * Map the inspected schema to either an existing view/table and append e.g. column or (if not existing) create a new view/table.
     *
     * @param database  The database.
     * @param resultSet The inspected schema.
     * @return The database containing the updated view/table.
     * @throws SQLException If the result set does not contain the requested parameter.
     */
    default ViewDto schemaResultSetToView(Database database, ResultSet resultSet) throws SQLException {
        return ViewDto.builder()
                .name(resultSet.getString(1))
                .internalName(resultSet.getString(1))
                .databaseId(database.getId())
                .isInitialView(false)
                .isPublic(database.getIsPublic())
                .isSchemaPublic(database.getIsSchemaPublic())
                .query(resultSet.getString(9))
                .queryHash(DigestUtils.sha256Hex(resultSet.getString(9)))
                .columns(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .build();
    }

    default TableStatisticDto resultSetToTableStatistic(ResultSet data) throws SQLException {
        final TableStatisticDto statistic = TableStatisticDto.builder()
                .columns(new LinkedList<>())
                .build();
        while (data.next()) {
            final ColumnStatisticDto columnStatistic = ColumnStatisticDto.builder()
                    .name(data.getString(1))
                    .min(data.getBigDecimal(2))
                    .max(data.getBigDecimal(3))
                    .median(data.getBigDecimal(4))
                    .mean(data.getBigDecimal(5))
                    .stdDev(data.getBigDecimal(6))
                    .build();
            statistic.getColumns()
                    .add(columnStatistic);
        }
        return statistic;
    }

    @Mappings({
            @Mapping(target = "ownedBy", source = "owner.username")
    })
    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    ColumnBriefDto columnDtoToColumnBriefDto(ColumnDto data);

    default ColumnTypeDto columnTypeToColumnTypeDto(String data) {
        final String upper = data.toUpperCase();
        if (upper.startsWith("TINYINT(1)")) {
            return ColumnTypeDto.BOOL;
        }
        return switch (upper) {
            case "BOOLEAN" -> ColumnTypeDto.BOOL;
            case "INTEGER" -> ColumnTypeDto.INT;
            case "NUMERIC" -> ColumnTypeDto.DECIMAL;
            case "CHARACTER" -> ColumnTypeDto.CHAR;
            case "DOUBLE PRECISION" -> ColumnTypeDto.DOUBLE;
            case "CHARACTER VARYING" -> ColumnTypeDto.VARCHAR;
            case "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITHOUT TIME ZONE" -> ColumnTypeDto.TIMESTAMP;
            default -> ColumnTypeDto.valueOf(upper);
        };
    }

    default TableDto resultSetToTable(ResultSet resultSet, TableDto table) throws SQLException {
        final ColumnDto column = ColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(columnTypeToColumnTypeDto(resultSet.getString(4)))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(8))
                .internalName(resultSet.getString(8))
                .tableId(table.getId())
                .databaseId(table.getDatabaseId())
                .description(resultSet.getString(9))
                .build();
        final String dataType = resultSet.getString(4);
        if (column.getColumnType().equals(ColumnTypeDto.ENUM)) {
            column.setEnums(Arrays.stream(dataType.substring(0, resultSet.getString(8).length() - 1)
                            .replace("enum(", "")
                            .split(","))
                    .map(value -> EnumDto.builder()
                            .value(value.replace("'", ""))
                            .build())
                    .toList());
        }
        if (column.getColumnType().equals(ColumnTypeDto.SET)) {
            column.setSets(Arrays.stream(dataType.substring(0, dataType.length() - 1)
                            .replace("set(", "")
                            .split(","))
                    .map(value -> SetDto.builder()
                            .value(value.replace("'", ""))
                            .build())
                    .toList());
        }
        /* fix boolean and set size for others */
        if (resultSet.getString(5) != null) {
            column.setSize(resultSet.getLong(5));
        }
        if (resultSet.getString(6) != null) {
            column.setSize(resultSet.getLong(6));
        }
        table.getColumns()
                .add(column);
        return table;
    }

    default ViewDto resultSetToTable(ResultSet resultSet, ViewDto view) throws SQLException {
        final ViewColumnDto column = ViewColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(columnTypeToColumnTypeDto(resultSet.getString(4)))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(8))
                .internalName(resultSet.getString(8))
                .databaseId(view.getDatabaseId())
                .build();
        /* fix boolean and set size for others */
        if (resultSet.getString(8).equalsIgnoreCase("tinyint(1)")) {
            column.setColumnType(ColumnTypeDto.BOOL);
        } else if (resultSet.getString(5) != null) {
            column.setSize(resultSet.getLong(5));
        } else if (resultSet.getString(6) != null) {
            column.setSize(resultSet.getLong(6));
        }
        view.getColumns()
                .add(column);
        return view;
    }

    default SchemaAnalysisResultDto resultSetToSchemaAnalysisResult(DuckDBResultSet resultSet) throws SQLException,
            AnalyseDataTypesException {
        if (!resultSet.next()) {
            log.error("Failed to analyse data types: no result");
            throw new AnalyseDataTypesException("Failed to analyse data types: no result");
        }
        try {
            return SchemaAnalysisResultDto.builder()
                    .delimiter(resultSet.getString("Delimiter"))
                    .quote(resultSet.getString("Quote"))
                    .escape(resultSet.getString("Escape"))
                    .newlineDelimiter(resultSet.getString("NewLineDelimiter"))
                    .comment(resultSet.getString("Comment"))
                    .skipRows(resultSet.getInt("SkipRows"))
                    .hasHeader(resultSet.getBoolean("HasHeader"))
                    .columns(structListToColumnAnalysisResultDtoList(resultSet.getString("Columns")))
                    .dateFormat(resultSet.getString("DateFormat"))
                    .timestampFormat(resultSet.getString("TimestampFormat"))
                    .prompt(resultSet.getString("Prompt"))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to analyse data types: parse columns: {}", e.getMessage());
            throw new AnalyseDataTypesException("Failed to analyse data types: parse columns", e);
        }
    }

    default ColumnTypeDto duckDbDataTypeToMariaDbColumnTypeDto(String data) {
        final List<String> inCompatibleTypes = List.of("INTERVAL", "SQLNULL", "HUGEINT", "UHUGEINT");
        if (inCompatibleTypes.contains(data)) {
            log.warn("Failed to map data type: {}", data);
            return null;
        }
        if (List.of("UBIGINT").contains(data)) {
            return ColumnTypeDto.BIGINT;
        } else if (List.of("INTEGER", "UINTEGER").contains(data)) {
            return ColumnTypeDto.INT;
        } else if (data.equals("USMALLINT")) {
            return ColumnTypeDto.SMALLINT;
        } else if (data.equals("UTINYINT")) {
            return ColumnTypeDto.TINYINT;
        } else if (data.equals("BOOLEAN")) {
            return ColumnTypeDto.BOOL;
        } else if (data.equals("UUID")) {
            return ColumnTypeDto.VARCHAR;
        } else if (List.of("TIMESTAMP WITH TIME ZONE", "TIMESTAMP_NS", "TIMESTAMP_MS", "TIMESTAMP_S",
                "TIMESTAMP_TZ", "TIMESTAMP WITHOUT TIME ZONE").contains(data)) {
            return ColumnTypeDto.TIMESTAMP;
        } else if (List.of("TIME_TZ").contains(data)) {
            return ColumnTypeDto.TIME;
        }
        return ColumnTypeDto.valueOf(data);
    }

    default Map<String, ColumnAnalysisResultDto> resultSetToConstraintResult(ResultSet resultSet) throws SQLException {
        final Map<String, ColumnAnalysisResultDto> constraints = new HashMap<>();
        while (resultSet.next()) {
            final String columnName = resultSet.getString("column_name");
            final String columnType = resultSet.getString("column_type");
            final String nullAllowed = resultSet.getString("null");
            final String key = resultSet.getString("key");
            constraints.put(columnName, ColumnAnalysisResultDto.builder()
                    .name(columnName)
                    .datatype(duckDbDataTypeToMariaDbColumnTypeDto(columnType))
                    .nullAllowed(nullAllowed != null && columnName.equals("YES"))
                    .primaryKey(key != null && key.equals("YES"))
                    .build());
        }
        return constraints;
    }

    default List<ColumnAnalysisResultDto> structListToColumnAnalysisResultDtoList(String columns)
            throws JsonProcessingException {
        log.trace("raw columns: {}", columns);
        final Pattern pattern = Pattern.compile("\\{'?name'?: *([^,']+), '?type'?: *([^'}]+)");
        final Matcher matcher = pattern.matcher(columns);
        final List<ColumnAnalysisResultDto> result = new LinkedList<>();
        while (matcher.find()) {
            final ColumnAnalysisResultDto analysis = ColumnAnalysisResultDto.builder()
                    .name(matcher.group(1))
                    .datatype(columnTypeToColumnTypeDto(matcher.group(2)))
                    .nullAllowed(true)
                    .build();
            if (analysis.getDatatype().equals(ColumnTypeDto.VARCHAR)) {
                analysis.setSize(255);
            }
            result.add(analysis);
        }
        log.debug("mapped to {} column(s) analysis: {}", result.size(), result.stream().map(ColumnAnalysisResultDto::getName).toList());
        return result;
    }

    default QueryDto resultSetToQueryDto(ResultSet data) throws SQLException {
        /* note that next() is called outside this mapping function */
        return QueryDto.builder()
                .id(UUID.fromString(data.getString(1)))
                .query(data.getString(3))
                .queryNormalized(data.getString(4))
                .queryHash(data.getString(5))
                .resultHash(data.getString(6))
                .resultNumber(data.getLong(7))
                .isPersisted(data.getBoolean(8))
                .owner(UserBriefDto.builder()
                        .username(data.getString(2))
                        .build())
                .execution(data.getTimestamp(9).toInstant())
                .build();
    }

    default Subset resultSetToSubset(ResultSet data) throws SQLException {
        /* note that next() is called outside this mapping function */
        return Subset.builder()
                .id(UUID.fromString(data.getString(1)))
                .query(data.getString(3))
                .queryNormalized(data.getString(4))
                .queryHash(data.getString(5))
                .resultHash(data.getString(6))
                .resultNumber(data.getLong(7))
                .isPersisted(data.getBoolean(8))
                .ownedBy(data.getString(2))
                .execution(data.getTimestamp(9).toInstant())
                .build();
    }

    default List<TableHistoryDto> resultSetToTableHistory(ResultSet resultSet) throws SQLException {
        /* columns */
        final List<TableHistoryDto> history = new LinkedList<>();
        while (resultSet.next()) {
            history.add(TableHistoryDto.builder()
                    .timestamp(resultSet.getTimestamp(1).toInstant())
                    .event(HistoryEventTypeDto.valueOf(resultSet.getString(2).toUpperCase()))
                    .total(resultSet.getLong(3))
                    .build());
        }
        log.trace("found {} history event(s)", history.size());
        return history;
    }

    ForeignKeyBriefDto foreignKeyDtoToForeignKeyBriefDto(ForeignKeyDto data);

    default TableDto resultSetToConstraint(ResultSet resultSet, TableDto table) throws SQLException {
        final String type = resultSet.getString(2);
        final String name = resultSet.getString(3);
        final String columnName = resultSet.getString(4);
        final String referencedTable = resultSet.getString(5);
        final String referencedColumnName = resultSet.getString(6);
        final ReferenceTypeDto deleteRule = resultSet.getString(7) != null ? ReferenceTypeDto.fromType(resultSet.getString(7)) : null;
        final ReferenceTypeDto updateRule = resultSet.getString(8) != null ? ReferenceTypeDto.fromType(resultSet.getString(8)) : null;
        final Optional<ColumnDto> optional = table.getColumns().stream()
                .filter(c -> c.getInternalName().equals(columnName))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find table column: {}", columnName);
            throw new IllegalArgumentException("Failed to find table column");
        }
        final ColumnDto column = optional.get();
        if (type.equals("PRIMARY KEY")) {
            /* we can assume that there is only 1 primary key and skip finding it */
            table.getConstraints()
                    .getPrimaryKey()
                    .add(PrimaryKeyDto.builder()
                            .column(columnDtoToColumnBriefDto(column))
                            .table(tableDtoToTableBriefDto(table))
                            .build());
        } else if (type.equals("FOREIGN KEY") || type.equals("UNIQUE")) {
            final Optional<UniqueDto> optional2 = table.getConstraints()
                    .getUniques()
                    .stream()
                    .filter(u -> u.getName().equals(name))
                    .findFirst();
            if (optional2.isPresent()) {
                optional2.get()
                        .getColumns()
                        .add(columnDtoToColumnBriefDto(column));
                return table;
            }
            if (type.equals("UNIQUE")) {
                table.getConstraints()
                        .getUniques()
                        .add(UniqueDto.builder()
                                .name(name)
                                .columns(new LinkedList<>(List.of(columnDtoToColumnBriefDto(column))))
                                .build());
                return table;
            }
            final Optional<ForeignKeyDto> optional1 = table.getConstraints()
                    .getForeignKeys()
                    .stream()
                    .filter(fk -> fk.getName().equals(name))
                    .findFirst();
            final ForeignKeyReferenceDto foreignKeyReference = ForeignKeyReferenceDto.builder()
                    .column(ColumnBriefDto.builder()
                            .name(columnName)
                            .internalName(columnName)
                            .databaseId(table.getDatabaseId())
                            .build())
                    .referencedColumn(ColumnBriefDto.builder()
                            .name(referencedColumnName)
                            .internalName(referencedColumnName)
                            .databaseId(table.getDatabaseId())
                            .build())
                    .build();
            if (optional1.isPresent()) {
                foreignKeyReference.setForeignKey(foreignKeyDtoToForeignKeyBriefDto(optional1.get()));
                optional1.get()
                        .getReferences()
                        .add(foreignKeyReference);
                log.debug("found foreign key: create part ({}) referencing table {} ({})", columnName, referencedTable, referencedColumnName);
                return table;
            }
            final ForeignKeyDto foreignKey = ForeignKeyDto.builder()
                    .name(name)
                    .table(tableDtoToTableBriefDto(table))
                    .referencedTable(TableBriefDto.builder()
                            .name(referencedTable)
                            .internalName(referencedTable)
                            .databaseId(table.getDatabaseId())
                            .build())
                    .references(new LinkedList<>(List.of(foreignKeyReference)))
                    .onDelete(deleteRule)
                    .onUpdate(updateRule)
                    .build();
            foreignKey.getReferences()
                    .forEach(ref -> ref.setForeignKey(foreignKeyDtoToForeignKeyBriefDto(foreignKey)));
            table.getConstraints()
                    .getForeignKeys()
                    .add(foreignKey);
            log.debug("create foreign key: add part ({}) referencing table {} ({})", columnName, referencedTable, referencedColumnName);
            return table;
        }
        return table;
    }

    UserBriefDto userToUserBriefDto(User data);

    default TableDto schemaResultSetToTable(Database database, ResultSet resultSet) throws SQLException,
            TableNotFoundException {
        if (!resultSet.next()) {
            throw new TableNotFoundException("Failed to find table in the information schema");
        }
        return TableDto.builder()
                .name(resultSet.getString(1))
                .internalName(resultSet.getString(1))
                .isVersioned(resultSet.getString(2).equals("SYSTEM VERSIONED"))
                .numRows(resultSet.getLong(3))
                .avgRowLength(resultSet.getLong(4))
                .dataLength(resultSet.getLong(5))
                .maxDataLength(resultSet.getLong(6))
                .databaseId(database.getId())
                .queueName("dbrepo")
                .routingKey("dbrepo")
                .description(resultSet.getString(10))
                .columns(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .owner(UserBriefDto.builder()
                        .username(database.getOwnedBy())
                        .build())
                .constraints(ConstraintsDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .primaryKey(new LinkedHashSet<>())
                        .uniques(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .build())
                .isPublic(database.getIsPublic())
                .build();
    }

    default int columnTypeDtoToTypes(ColumnType data) {
        return switch (data) {
            case CHAR -> VARCHAR;
            case VARCHAR -> VARCHAR;
            case BINARY -> BINARY;
            case REAL -> REAL;
            case VARBINARY -> BINARY;
            case TINYBLOB -> BLOB;
            case TINYTEXT -> VARCHAR;
            case TEXT -> VARCHAR;
            case BLOB -> BLOB;
            case MEDIUMTEXT -> VARCHAR;
            case MEDIUMBLOB -> BLOB;
            case LONGTEXT -> VARCHAR;
            case LONGBLOB -> BLOB;
            case ENUM -> VARCHAR;
            case SET -> VARCHAR;
            case SERIAL -> BIGINT;
            case BIT -> BIT;
            case TINYINT -> BOOLEAN;
            case BOOL -> BOOLEAN;
            case SMALLINT -> INTEGER;
            case MEDIUMINT -> INTEGER;
            case INT -> INTEGER;
            case BIGINT -> BIGINT;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
            case DECIMAL -> DECIMAL;
            case DATE -> DATE;
            case DATETIME -> TIMESTAMP;
            case TIMESTAMP -> TIMESTAMP;
            case TIME -> TIME;
            case YEAR -> TIMESTAMP;
        };
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, ColumnType columnType, int idx, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, columnTypeDtoToTypes(columnType));
        }
        log.trace("mapping value {} at position {} to type: {}", value, idx, columnType);
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                if (value == null) {
                    ps.setNull(idx, BLOB);
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
                ps.setString(idx, String.valueOf(value));
                break;
            case DATE:
                ps.setString(idx, String.valueOf(value));
                break;
            case BIGINT, SERIAL:
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case TINYINT:
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case FLOAT, DOUBLE, DECIMAL:
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case SMALLINT:
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                ps.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
                ps.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIME, DATETIME, TIMESTAMP, YEAR:
                ps.setString(idx, String.valueOf(value));
                break;
            default:
                log.error("Failed to map column type {} at index {} for value {}", columnType, idx, value);
                throw new IllegalArgumentException("Failed to map column type " + columnType);
        }
    }

}
