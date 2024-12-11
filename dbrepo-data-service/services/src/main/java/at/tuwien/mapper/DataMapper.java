package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.TableHistoryDto;
import at.tuwien.api.database.table.TableStatisticDto;
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
import at.tuwien.config.QueryConfig;
import at.tuwien.exception.QueryNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.hadoop.shaded.com.google.common.hash.Hashing;
import org.apache.hadoop.shaded.org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.shaded.org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
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
     * @throws SQLException
     */
    default ViewDto schemaResultSetToView(DatabaseDto database, ResultSet resultSet) throws SQLException {
        return ViewDto.builder()
                .name(resultSet.getString(1))
                .internalName(resultSet.getString(1))
                .vdbid(database.getId())
                .database(database)
                .isInitialView(false)
                .isPublic(database.getIsPublic())
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

    default TableDto resultSetToTable(ResultSet resultSet, TableDto table, QueryConfig queryConfig) throws SQLException {
        final ColumnDto column = ColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(ColumnTypeDto.valueOf(resultSet.getString(4).toUpperCase()))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(10))
                .internalName(resultSet.getString(10))
                .table(table)
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

    default ViewDto resultSetToTable(ResultSet resultSet, ViewDto view, QueryConfig queryConfig) throws SQLException {
        final ViewColumnDto column = ViewColumnDto.builder()
                .ordinalPosition(resultSet.getInt(1) - 1) /* start at zero */
                .autoGenerated(resultSet.getString(2) != null && resultSet.getString(2).startsWith("nextval"))
                .isNullAllowed(resultSet.getString(3).equals("YES"))
                .columnType(ColumnTypeDto.valueOf(resultSet.getString(4).toUpperCase()))
                .d(resultSet.getString(7) != null ? resultSet.getLong(7) : null)
                .name(resultSet.getString(10))
                .internalName(resultSet.getString(10))
                .databaseId(view.getDatabase().getId())
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
        log.trace("parsed view {}.{} column: {}", view.getDatabase().getInternalName(), view.getInternalName(), column.getInternalName());
        return view;
    }

    /**
     * Parse columns from a SQL statement of a known database.
     *
     * @param databaseId The database id.
     * @param tables     The list of tables.
     * @param query      The SQL statement.
     * @return The list of columns.
     * @throws JSQLParserException The table/view or column was not found in the database.
     */
    default List<ColumnDto> parseColumns(Long databaseId, List<TableDto> tables, String query) throws JSQLParserException {
        final List<ColumnDto> columns = new ArrayList<>();
        final CCJSqlParserManager parserRealSql = new CCJSqlParserManager();
        final net.sf.jsqlparser.statement.Statement statement = parserRealSql.parse(new StringReader(query));
        log.trace("parse columns from query: {}", query);
        /* bi-directional mapping */
        tables.forEach(table -> table.getColumns()
                .forEach(column -> column.setTable(table)));
        /* check */
        if (!(statement instanceof Select selectStatement)) {
            log.error("Query attempts to update the dataset, not a SELECT statement");
            throw new JSQLParserException("Query attempts to update the dataset");
        }
        /* start parsing */
        final PlainSelect ps = (PlainSelect) selectStatement.getSelectBody();
        final List<SelectItem> clauses = ps.getSelectItems();
        log.trace("columns referenced in the from-clause: {}", clauses);
        /* Parse all tables */
        final List<FromItem> fromItems = new ArrayList<>(fromItemToFromItems(ps.getFromItem()));
        if (ps.getJoins() != null && !ps.getJoins().isEmpty()) {
            log.trace("query contains join items: {}", ps.getJoins());
            for (net.sf.jsqlparser.statement.select.Join j : ps.getJoins()) {
                if (j.getRightItem() != null) {
                    fromItems.add(j.getRightItem());
                }
            }
        }
        final List<ColumnDto> allColumns = tables.stream()
                .map(TableDto::getColumns)
                .flatMap(List::stream)
                .toList();
        log.trace("columns referenced in the from-clause and join-clause(s): {}", clauses);
        /* Checking if all columns exist */
        for (SelectItem clause : clauses) {
            final SelectExpressionItem item = (SelectExpressionItem) clause;
            final Column column = (Column) item.getExpression();
            final String columnName = column.getColumnName().replace("`", "");
            final List<ColumnDto> filteredColumns = allColumns.stream()
                    .filter(c -> (c.getAlias() != null && c.getAlias().equals(columnName)) || c.getInternalName().equals(columnName))
                    .toList();
            String tableOrView = null;
            for (Table t : fromItems.stream().map(t -> (net.sf.jsqlparser.schema.Table) t).toList()) {
                if (column.getTable() == null) {
                    /* column does not reference a specific table, find out */
                    final List<String> filteredTables = filteredColumns.stream()
                            .map(c -> c.getTable().getInternalName())
                            .filter(table -> fromItems.stream().map(f -> (Table) f).anyMatch(otherTable -> otherTable.getName().replace("`", "").equals(table)))
                            .toList();
                    if (filteredTables.size() != 1) {
                        log.error("Failed to filter column {} to exactly one match: {}", columnName, filteredTables.stream().map(table -> table + "." + columnName).toList());
                        throw new JSQLParserException("Failed to filter column " + columnName + " to exactly one match");
                    }
                    if (tableMatches(t, filteredTables.get(0))) {
                        tableOrView = t.getName().replace("`", "");
                        break;
                    }
                }
                /* column references a specific table */
                final String tableOrAlias = (t.getAlias() != null ? t.getAlias().getName() : column.getTable().getName())
                        .replace("`", "");
                if (tableMatches(t, tableOrAlias)) {
                    tableOrView = t.getName().replace("`", "");
                    break;
                }
            }
            if (tableOrView == null) {
                log.error("Failed to find table/view {} (with designator {})", column.getTable().getName(), column.getTable().getAlias());
                throw new JSQLParserException("Failed to find table/view " + column.getTable().getName() + " (with alias " + column.getTable().getAlias() + ")");
            }
            final String finalTableOrView = tableOrView;
            final List<ColumnDto> selectColumns = filteredColumns.stream()
                    .filter(c -> c.getTable().getInternalName().equals(finalTableOrView))
                    .toList();
            final ColumnDto resultColumn;
            if (selectColumns.size() != 1) {
                if (filteredColumns.size() != 1) {
                    log.error("Failed to filter column {} to exactly one match: {}", columnName, selectColumns.stream().map(c -> c.getTable().getInternalName() + "." + c.getInternalName()).toList());
                    throw new JSQLParserException("Failed to filter column " + columnName + " to exactly one match");
                }
                resultColumn = filteredColumns.get(0);
            } else {
                resultColumn = selectColumns.get(0);
            }
            if (item.getAlias() != null) {
                resultColumn.setAlias(item.getAlias().getName().replace("`", ""));
            }
            resultColumn.setDatabaseId(databaseId);
            resultColumn.setTable(resultColumn.getTable());
            resultColumn.setTableId(resultColumn.getTable().getId());
            log.trace("found column with internal name {} and alias {}", resultColumn.getInternalName(), resultColumn.getAlias());
            columns.add(resultColumn);
        }
        return columns;
    }

    default boolean tableMatches(net.sf.jsqlparser.schema.Table table, String tableOrDesignator) {
        final String tableName = table.getName()
                .trim()
                .replace("`", "");
        if (table.getAlias() == null) {
            /* table does not have designator */
            log.trace("table '{}' has no designator", tableName);
            return tableName.equals(tableOrDesignator);
        }
        /* has designator */
        final String designator = table.getAlias()
                .getName()
                .trim()
                .replace("`", "");
        log.trace("table '{}' has designator {}", tableName, designator);
        return designator.equals(tableOrDesignator);
    }

    default List<FromItem> fromItemToFromItems(FromItem data) throws JSQLParserException {
        return fromItemToFromItems(data, 0);
    }

    default List<FromItem> fromItemToFromItems(FromItem data, Integer level) throws JSQLParserException {
        final List<FromItem> fromItems = new LinkedList<>();
        if (data instanceof net.sf.jsqlparser.schema.Table table) {
            fromItems.add(data);
            log.trace("from-item {} is of type table: level ~> {}", table.getName(), level);
            return fromItems;
        }
        if (data instanceof SubJoin subJoin) {
            log.trace("from-item is of type sub-join: level ~> {}", level);
            for (Join join : subJoin.getJoinList()) {
                final List<FromItem> tmp = fromItemToFromItems(join.getRightItem(), level + 1);
                if (tmp == null) {
                    log.error("Failed to find right sub-join table: {}", join.getRightItem());
                    throw new JSQLParserException("Failed to find right sub-join table");
                }
                fromItems.addAll(tmp);
            }
            final List<FromItem> tmp = fromItemToFromItems(subJoin.getLeft(), level + 1);
            if (tmp == null) {
                log.error("Failed to find left sub-join table: {}", subJoin.getLeft());
                throw new JSQLParserException("Failed to find left sub-join table");
            }
            fromItems.addAll(tmp);
            return fromItems;
        }
        log.warn("unknown from-item {}", data);
        return null;
    }

    default QueryDto resultSetToQueryDto(@NotNull ResultSet data) throws SQLException, QueryNotFoundException {
        /* note that next() is called outside this mapping function */
        return QueryDto.builder()
                .id(data.getLong(1))
                .owner(UserBriefDto.builder()
                        .id(UUID.fromString(data.getString(3)))
                        .build())
                .query(data.getString(4))
                .queryHash(data.getString(5))
                .resultHash(data.getString(6))
                .resultNumber(data.getLong(7))
                .isPersisted(data.getBoolean(8))
                .execution(LocalDateTime.parse(data.getString(9), mariaDbFormatter)
                        .atZone(ZoneId.of("UTC"))
                        .toInstant())
                .build();
    }

    default List<TableHistoryDto> resultSetToTableHistory(ResultSet resultSet) throws SQLException {
        /* columns */
        final List<TableHistoryDto> history = new LinkedList<>();
        while (resultSet.next()) {
            history.add(TableHistoryDto.builder()
                    .timestamp(LocalDateTime.parse(resultSet.getString(1), mariaDbFormatter)
                            .atZone(ZoneId.of("UTC"))
                            .toInstant())
                    .event(resultSet.getString(2))
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
                        .add(column);
                return table;
            }
            if (type.equals("UNIQUE")) {
                table.getConstraints()
                        .getUniques()
                        .add(UniqueDto.builder()
                                .name(name)
                                .columns(new LinkedList<>(List.of(column)))
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

    default Object dataColumnToObject(Object data, ColumnDto column) {
        if (data == null) {
            return null;
        }
        /* boolean encoding fix */
        if (column.getColumnType().equals(ColumnTypeDto.TINYINT) && column.getSize() == 1) {
            column.setColumnType(ColumnTypeDto.BOOL);
        }
        switch (column.getColumnType()) {
            case DATE -> {
                final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .parseCaseInsensitive() /* case insensitive to parse JAN and FEB */
                        .appendPattern("yyyy-MM-dd")
                        .toFormatter(Locale.ENGLISH);
                final LocalDate date = LocalDate.parse(String.valueOf(data), formatter);
                return date.atStartOfDay(ZoneId.of("UTC"))
                        .toInstant();
            }
            case TIMESTAMP, DATETIME -> {
                return Timestamp.valueOf(data.toString())
                        .toInstant();
            }
            case BINARY, VARBINARY, BIT -> {
                return Long.parseLong(String.valueOf(data), 2);
            }
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET -> {
                return String.valueOf(data);
            }
            case BIGINT, SERIAL -> {
                return new BigInteger(String.valueOf(data));
            }
            case INT, SMALLINT, MEDIUMINT, TINYINT -> {
                return Integer.parseInt(String.valueOf(data));
            }
            case DECIMAL, FLOAT, DOUBLE -> {
                return Double.valueOf(String.valueOf(data));
            }
            case BOOL -> {
                return Boolean.valueOf(String.valueOf(data));
            }
            case TIME -> {
                return String.valueOf(data);
            }
            case YEAR -> {
                final String date = String.valueOf(data);
                return Short.valueOf(date.substring(0, date.indexOf('-')));
            }
        }
        log.warn("column type {} is not known", column.getColumnType());
        throw new IllegalArgumentException("Column type not known");
    }

    default QueryResultDto resultListToQueryResultDto(List<ColumnDto> columns, ResultSet result) throws SQLException {
        log.trace("mapping result list to query result, columns.size={}", columns.size());
        final List<Map<String, Object>> resultList = new LinkedList<>();
        while (result.next()) {
            /* map the result set to the columns through the stored metadata in the metadata database */
            int[] idx = new int[]{1};
            final Map<String, Object> map = new HashMap<>();
            for (final ColumnDto column : columns) {
                final String columnOrAlias;
                if (column.getAlias() != null) {
                    log.debug("column {} has alias {}", column.getInternalName(), column.getAlias());
                    columnOrAlias = column.getAlias();
                } else {
                    columnOrAlias = column.getInternalName();
                }
                if (List.of(ColumnTypeDto.BLOB, ColumnTypeDto.TINYBLOB, ColumnTypeDto.MEDIUMBLOB, ColumnTypeDto.LONGBLOB).contains(column.getColumnType())) {
                    log.trace("column {} is of type {}", columnOrAlias, column.getColumnType().getType().toLowerCase());
                    final Blob blob = result.getBlob(idx[0]++);
                    final String value = blob == null ? null : Hex.encodeHexString(blob.getBytes(1, (int) blob.length())).toUpperCase();
                    map.put(columnOrAlias, value);
                    continue;
                }
                final Object object = dataColumnToObject(result.getObject(idx[0]++), column);
                map.put(columnOrAlias, object);
            }
            resultList.add(map);
        }
        final int[] idx = new int[]{0};
        final List<Map<String, Integer>> headers = columns.stream()
                .map(c -> (Map<String, Integer>) new LinkedHashMap<String, Integer>() {{
                    put(c.getAlias() != null ? c.getAlias() : c.getInternalName(), idx[0]++);
                }})
                .toList();
        log.trace("created ordered header list: {}", headers);
        return QueryResultDto.builder()
                .result(resultList)
                .headers(headers)
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
