package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnStatisticDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.ConstraintsDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyBriefDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.exception.TableNotFoundException;
import org.apache.hadoop.shaded.com.google.common.hash.Hashing;
import org.apache.hadoop.shaded.org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataMapper.class);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
            .withZone(ZoneId.of("UTC"));

    /* redundant */
    ColumnBriefDto columnDtoToColumnBriefDto(ColumnDto data);

    /* redundant */
    @Mappings({
            @Mapping(target = "databaseId", source = "tdbid")
    })
    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    /* redundant */
    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ForeignKeyBriefDto foreignKeyDtoToForeignKeyBriefDto(ForeignKeyDto data);

    default String rabbitMqTupleToInsertOrUpdateQuery(TableDto table, Map<String, Object> data) {
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
        return statement.toString();
    }

    /**
     * Map the inspected schema to either an existing view/table and append e.g. column or (if not existing) create a new view/table.
     *
     * @param database  The database.
     * @param resultSet The inspected schema.
     * @return The database containing the updated view/table.
     * @throws SQLException If the result set does not contain the requested parameter.
     */
    default ViewDto schemaResultSetToView(DatabaseDto database, ResultSet resultSet) throws SQLException {
        return ViewDto.builder()
                .name(resultSet.getString(1))
                .internalName(resultSet.getString(1))
                .vdbid(database.getId())
                .isInitialView(false)
                .isPublic(database.getIsPublic())
                .isSchemaPublic(database.getIsSchemaPublic())
                .query(resultSet.getString(9))
                .queryHash(Hashing.sha256()
                        .hashString(resultSet.getString(9), StandardCharsets.UTF_8)
                        .toString())
                .columns(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .owner(database.getOwner())
                .build();
    }

    default TableStatisticDto resultSetToTableStatistic(ResultSet data) throws SQLException {
        final TableStatisticDto statistic = TableStatisticDto.builder()
                .columns(new LinkedHashMap<>())
                .build();
        while (data.next()) {
            final ColumnStatisticDto columnStatistic = ColumnStatisticDto.builder()
                    .min(data.getBigDecimal(2))
                    .max(data.getBigDecimal(3))
                    .median(data.getBigDecimal(4))
                    .mean(data.getBigDecimal(5))
                    .stdDev(data.getBigDecimal(6))
                    .build();
            statistic.getColumns().put(data.getString(1), columnStatistic);
        }
        return statistic;
    }

    default TableDto resultSetToTable(ResultSet resultSet, TableDto table) throws SQLException {
        final ColumnDto column = ColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(ColumnTypeDto.valueOf(resultSet.getString(4).toUpperCase()))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(10))
                .internalName(resultSet.getString(10))
                .tableId(table.getId())
                .databaseId(table.getTdbid())
                .description(resultSet.getString(11))
                .build();
        final String dataType = resultSet.getString(8);
        if (column.getColumnType().equals(ColumnTypeDto.ENUM)) {
            column.setEnums(Arrays.stream(dataType.substring(0, resultSet.getString(8).length() - 1)
                            .replace("enum(", "")
                            .split(","))
                    .map(value -> value.replace("'", ""))
                    .toList());
        }
        if (column.getColumnType().equals(ColumnTypeDto.SET)) {
            column.setSets(Arrays.stream(dataType.substring(0, dataType.length() - 1)
                            .replace("set(", "")
                            .split(","))
                    .map(value -> value.replace("'", ""))
                    .toList());
        }
        /* fix boolean and set size for others */
        if (dataType.startsWith("tinyint(1)")) {
            column.setColumnType(ColumnTypeDto.BOOL);
        } else if (resultSet.getString(5) != null) {
            column.setSize(resultSet.getLong(5));
        } else if (resultSet.getString(6) != null) {
            column.setSize(resultSet.getLong(6));
        }
        /* constraints */
        if (resultSet.getString(9) != null && resultSet.getString(9).equals("PRI")) {
            table.getConstraints().getPrimaryKey().add(PrimaryKeyDto.builder()
                    .table(tableDtoToTableBriefDto(table))
                    .column(columnDtoToColumnBriefDto(column))
                    .build());
        }
        table.getColumns()
                .add(column);
        return table;
    }

    default ViewDto resultSetToTable(ResultSet resultSet, ViewDto view) throws SQLException {
        final ViewColumnDto column = ViewColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(ColumnTypeDto.valueOf(resultSet.getString(4).toUpperCase()))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(10))
                .internalName(resultSet.getString(10))
                .databaseId(view.getVdbid())
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

    default QueryDto resultSetToQueryDto(@NotNull ResultSet data) throws SQLException {
        /* note that next() is called outside this mapping function */
        final QueryDto subset = QueryDto.builder()
                .id(UUID.fromString(data.getString(1)))
                .query(data.getString(4))
                .queryHash(data.getString(5))
                .resultHash(data.getString(6))
                .resultNumber(data.getLong(7))
                .isPersisted(data.getBoolean(8))
                .execution(LocalDateTime.parse(data.getString(9), mariaDbFormatter)
                        .atZone(ZoneId.of("UTC"))
                        .toInstant())
                .build();
        if (data.getString(3) != null) {
            subset.setOwner(UserBriefDto.builder()
                    .id(UUID.fromString(data.getString(3)))
                    .build());
        }
        return subset;
    }

    default List<TableHistoryDto> resultSetToTableHistory(ResultSet resultSet) throws SQLException {
        /* columns */
        final List<TableHistoryDto> history = new LinkedList<>();
        while (resultSet.next()) {
            history.add(TableHistoryDto.builder()
                    .timestamp(LocalDateTime.parse(resultSet.getString(1), mariaDbFormatter)
                            .atZone(ZoneId.of("UTC"))
                            .toInstant())
                    .event(HistoryEventTypeDto.valueOf(resultSet.getString(2).toUpperCase()))
                    .total(resultSet.getLong(3))
                    .build());
        }
        log.trace("found {} history event(s)", history.size());
        return history;
    }

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
        if (type.equals("FOREIGN KEY") || type.equals("UNIQUE")) {
            final Optional<UniqueDto> optional2 = table.getConstraints().getUniques().stream().filter(u -> u.getName().equals(name)).findFirst();
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
                            .databaseId(table.getTdbid())
                            .build())
                    .referencedColumn(ColumnBriefDto.builder()
                            .name(referencedColumnName)
                            .internalName(referencedColumnName)
                            .databaseId(table.getTdbid())
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
                            .databaseId(table.getTdbid())
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

    default TableDto schemaResultSetToTable(DatabaseDto database, ResultSet resultSet) throws SQLException,
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
                .tdbid(database.getId())
                .queueName("dbrepo")
                .routingKey("dbrepo")
                .description(resultSet.getString(10))
                .columns(new LinkedList<>())
                .identifiers(new LinkedList<>())
                .owner(database.getOwner())
                .constraints(ConstraintsDto.builder()
                        .foreignKeys(new LinkedList<>())
                        .primaryKey(new LinkedHashSet<>())
                        .uniques(new LinkedList<>())
                        .checks(new LinkedHashSet<>())
                        .build())
                .isPublic(database.getIsPublic())
                .build();
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement ps, ColumnTypeDto columnType, int idx, Object value) throws SQLException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
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
                if (value == null) {
                    ps.setNull(idx, Types.VARCHAR);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            case DATE:
                if (value == null) {
                    ps.setNull(idx, Types.DATE);
                    break;
                }
                ps.setString(idx, String.valueOf(value));
                break;
            case BIGINT, SERIAL:
                if (value == null) {
                    ps.setNull(idx, Types.BIGINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                if (value == null) {
                    ps.setNull(idx, Types.INTEGER);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case TINYINT:
                if (value == null) {
                    ps.setNull(idx, Types.TINYINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case SMALLINT:
                if (value == null) {
                    ps.setNull(idx, Types.SMALLINT);
                    break;
                }
                ps.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case DECIMAL:
                if (value == null) {
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case FLOAT:
                if (value == null) {
                    ps.setNull(idx, Types.FLOAT);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case DOUBLE:
                if (value == null) {
                    ps.setNull(idx, Types.DOUBLE);
                    break;
                }
                ps.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                if (value == null) {
                    ps.setNull(idx, Types.DECIMAL);
                    break;
                }
                ps.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
                if (value == null) {
                    ps.setNull(idx, Types.BOOLEAN);
                    break;
                }
                ps.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIME, DATETIME, TIMESTAMP, YEAR:
                if (value == null) {
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
