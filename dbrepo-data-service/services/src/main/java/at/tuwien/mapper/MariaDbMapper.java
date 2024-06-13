package at.tuwien.mapper;

import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.columns.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.exception.*;
import at.tuwien.utils.MariaDbUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.sql.Date;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {MetadataMapper.class, DataMapper.class})
public interface MariaDbMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MariaDbMapper.class);

    DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
            .withZone(ZoneId.of("UTC"));

    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("_")
                .replaceAll("-", "_");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    default String databaseSetPasswordQuery(String username, String password) {
        final StringBuilder statement = new StringBuilder("ALTER USER `")
                .append(username)
                .append("`@`%` IDENTIFIED BY '")
                .append(password)
                .append("';");
        log.trace("mapped set password statement: {}", statement);
        return statement.toString();
    }

    default String databaseCreateUserQuery(String username, String password) {
        final StringBuilder statement = new StringBuilder("CREATE USER IF NOT EXISTS `")
                .append(username)
                .append("`@`%` IDENTIFIED BY PASSWORD '")
                .append(password)
                .append("';");
        log.trace("mapped create user statement: {}", statement);
        return statement.toString();
    }

    default String databaseGrantPrivilegesQuery(String username, String grants) {
        final StringBuilder statement = new StringBuilder("GRANT ")
                .append(grants)
                .append(" ON *.* TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped grant privileges statement: {}", statement);
        return statement.toString();
    }

    @Named("createDatabase")
    default String databaseCreateDatabaseQuery(String database) {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database)
                .append("`");
        log.trace("mapped create database statement: {}", statement);
        return statement.toString();
    }

    default String databaseTablesSelectRawQuery() {
        final String statement = "SELECT DISTINCT t.`TABLE_NAME` FROM information_schema.TABLES t WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` = 'SYSTEM VERSIONED' AND t.`TABLE_NAME` != 'qs_queries' ORDER BY t.`TABLE_NAME` ASC";
        log.trace("mapped select tables statement: {}", statement);
        return statement;
    }

    default String databaseTableSelectRawQuery() {
        final String statement = "SELECT t.`TABLE_NAME`, t.`TABLE_TYPE`, t.`TABLE_ROWS`, t.`AVG_ROW_LENGTH`, t.`DATA_LENGTH`, t.`MAX_DATA_LENGTH`, COALESCE(t.`CREATE_TIME`, NOW()) as `CREATE_TIME`, t.`UPDATE_TIME`, v.`VIEW_DEFINITION`, t.`TABLE_COMMENT` FROM information_schema.TABLES t LEFT JOIN information_schema.VIEWS v ON t.`TABLE_NAME` = v.`TABLE_NAME` WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` = 'SYSTEM VERSIONED' AND t.`TABLE_NAME` != 'qs_queries' AND t.`TABLE_NAME` = ?";
        log.trace("mapped select table statement: {}", statement);
        return statement;
    }

    default String databaseViewSelectRawQuery() {
        final String statement = "SELECT t.`TABLE_NAME`, t.`TABLE_TYPE`, t.`TABLE_ROWS`, t.`AVG_ROW_LENGTH`, t.`DATA_LENGTH`, t.`MAX_DATA_LENGTH`, COALESCE(t.`CREATE_TIME`, NOW()) as `CREATE_TIME`, t.`UPDATE_TIME`, v.`VIEW_DEFINITION` FROM information_schema.TABLES t LEFT JOIN information_schema.VIEWS v ON t.`TABLE_NAME` = v.`TABLE_NAME` WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` = 'VIEW' AND t.`TABLE_NAME` != 'qs_queries' AND t.`TABLE_NAME` = ?";
        log.trace("mapped select view statement: {}", statement);
        return statement;
    }

    default String columnsCheckConstraintSelectRawQuery() {
        final String statement = "SELECT DISTINCT c.`CHECK_CLAUSE` FROM information_schema.COLUMNS k JOIN information_schema.CHECK_CONSTRAINTS c ON k.TABLE_NAME = c.TABLE_NAME WHERE k.TABLE_SCHEMA = ? AND k.TABLE_NAME = ?";
        log.trace("mapped select column constraint statement: {}", statement);
        return statement;
    }

    default String databaseTableColumnsSelectRawQuery() {
        final String statement = "SELECT `ORDINAL_POSITION`, `COLUMN_DEFAULT`, `IS_NULLABLE`, `DATA_TYPE`, `CHARACTER_MAXIMUM_LENGTH`, `NUMERIC_PRECISION`, `NUMERIC_SCALE`, `COLUMN_TYPE`, `COLUMN_KEY`, `COLUMN_NAME`, IF(`COLUMN_COMMENT`='',NULL,`COLUMN_COMMENT`) AS `COLUMN_COMMENT` FROM `information_schema`.`COLUMNS` WHERE `TABLE_SCHEMA` = ? AND `TABLE_NAME` = ?;";
        log.trace("mapped select columns statement: {}", statement);
        return statement;
    }

    default String databaseTableConstraintsSelectRawQuery() {
        final String statement = "SELECT k.`ORDINAL_POSITION`, c.`CONSTRAINT_TYPE`, k.`CONSTRAINT_NAME`, k.`COLUMN_NAME`, k.`REFERENCED_TABLE_NAME`, k.`REFERENCED_COLUMN_NAME`, r.`DELETE_RULE`, r.`UPDATE_RULE`FROM information_schema.TABLE_CONSTRAINTS c JOIN information_schema.KEY_COLUMN_USAGE k ON c.`TABLE_NAME` = k.`TABLE_NAME` AND c.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS r ON r.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` AND r.`CONSTRAINT_SCHEMA` = c.`TABLE_NAME`WHERE LOWER(k.`COLUMN_NAME`) != 'row_end' AND c.`TABLE_SCHEMA` = ? AND c.`TABLE_NAME` = ? ORDER BY k.`ORDINAL_POSITION` ASC;";
        log.trace("mapped select table constraints statement: {}", statement);
        return statement;
    }

    default String viewCreateRawQuery(String viewName, String query) {
        final String statement = "CREATE VIEW IF NOT EXISTS `" + viewName + "` AS (" + query + ")";
        log.trace("mapped create view statement: {}", statement);
        return statement;
    }

    default String databaseViewsSelectRawQuery() {
        final String statement = "SELECT DISTINCT t.`TABLE_NAME` FROM information_schema.TABLES t WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` = 'VIEW'";
        log.trace("mapped select views statement: {}", statement);
        return statement;
    }

    default String tableCreateDtoToCreateSequenceRawQuery(at.tuwien.api.database.table.internal.TableCreateDto data) {
        final String statement = "CREATE SEQUENCE IF NOT EXISTS `" + tableCreateDtoToSequenceName(data) + "` NOCACHE";
        log.trace("mapped create sequence statement: {}", statement);
        return statement;
    }

    default String filterToGetQueriesRawQuery(Boolean filterPersisted) {
        final StringBuilder statement = new StringBuilder("SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted`, `executed` FROM `qs_queries`");
        if (filterPersisted != null) {
            statement.append(" WHERE `is_persisted` = ?");
        }
        statement.append(";");
        log.trace("mapped get queries: {}", statement);
        return statement.toString();
    }

    default String tableCreateDtoToSequenceName(at.tuwien.api.database.table.internal.TableCreateDto data) {
        final String name = "seq_" + nameToInternalName(data.getName()) + "_id";
        log.trace("mapped table name {} to sequence name {}", data.getName(), name);
        return name;
    }

    /**
     * Maps the desired data type to a MySQL string with the default MySQL 8 values for each
     *
     * @param data The column definition.
     * @return The MySQL string.
     */
    default String columnTypeDtoToDataType(ColumnCreateDto data) {
        return switch (data.getType()) {
            case CHAR -> "CHAR(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case VARCHAR -> "VARCHAR(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case BINARY -> "BINARY(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case VARBINARY -> "VARBINARY(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case ENUM -> "ENUM(" + String.join(",", data.getEnums().stream().map(e -> ("'" + e + "'")).toList()) + ")";
            case SET -> "SET(" + String.join(",", data.getSets().stream().map(e -> ("'" + e + "'")).toList()) + ")";
            case BIT -> "BIT(" + Objects.requireNonNullElse(data.getSize(), "1") + ")";
            case TINYINT -> "TINYINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case SMALLINT -> "SMALLINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case MEDIUMINT -> "MEDIUMINT(" + Objects.requireNonNullElse(data.getSize(), "10") + ")";
            case INT -> "INT(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case BIGINT -> "BIGINT(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
            case FLOAT -> "FLOAT(" + Objects.requireNonNullElse(data.getSize(), "24") + ")";
            case DOUBLE ->
                    "DOUBLE(" + Objects.requireNonNullElse(data.getSize(), "25") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            case DECIMAL ->
                    "DECIMAL(" + Objects.requireNonNullElse(data.getSize(), "10") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            default -> data.getType().getType().toUpperCase();
        };
    }

    default String columnCreateDtoToPrimaryKeyLengthSpecification(ColumnCreateDto data) {
        if (EnumSet.of(ColumnTypeDto.BLOB, ColumnTypeDto.TEXT).contains(data.getType())) {
            return "(" + Objects.requireNonNullElse(data.getIndexLength(), 255) + ")";
        }
        return "";
    }

    default String tableColumnStatisticsSelectRawQuery(List<ColumnDto> data, String table) {
        final StringBuilder statement = new StringBuilder();
        final int[] idx = new int[]{0};
        data.stream()
                .filter(column -> MariaDbUtil.numericDataTypes.contains(column.getColumnType()))
                .forEach(column -> statement.append(idx[0]++ > 0 ? " UNION " : "")
                        .append("SELECT '")
                        .append(column.getInternalName())
                        .append("' as name, MIN(`")
                        .append(column.getInternalName())
                        .append("`) as min, MAX(`")
                        .append(column.getInternalName())
                        .append("`) as max, MEDIAN(`")
                        .append(column.getInternalName())
                        .append("`) OVER () as median, AVG(`")
                        .append(column.getInternalName())
                        .append("`) as mean, STDDEV(`")
                        .append(column.getInternalName())
                        .append("`) as std_dev FROM ")
                        .append(table));
        statement.append(";");
        log.trace("mapped select column statistic statement: {}", statement);
        return statement.toString();
    }

    default String tableCreateDtoToCreateTableRawQuery(at.tuwien.api.database.table.internal.TableCreateDto data) {
        final StringBuilder stringBuilder = new StringBuilder("CREATE TABLE `")
                .append(nameToInternalName(data.getName()))
                .append("` (");
        log.trace("primary key column(s) exist: {}", data.getConstraints().getPrimaryKey());
        final int[] idx = {0};
        for (ColumnCreateDto column : data.getColumns()) {
            stringBuilder.append(idx[0]++ > 0 ? ", " : "")
                    .append("`")
                    .append(nameToInternalName(column.getName()))
                    .append("` ")
                    /* data type */
                    .append(columnTypeDtoToDataType(column))
                    /* null expressions */
                    .append(column.getNullAllowed() != null && column.getNullAllowed() ? " NULL" : " NOT NULL")
                    /* default expressions */
                    .append(data.getNeedSequence() && column.getName().equals("id") ? " DEFAULT NEXTVAL(`" + tableCreateDtoToSequenceName(data) + "`)" : "");
            if (column.getDescription() != null && !column.getDescription().isEmpty()) {
                /* comments */
                stringBuilder.append(" COMMENT \"")
                        .append(column.getDescription())
                        .append("\"");
            }

        }
        /* create primary key index */
        if (data.getConstraints() != null) {
            log.trace("constraints are {}", data.getConstraints());
            if (data.getConstraints().getPrimaryKey() != null && !data.getConstraints().getPrimaryKey().isEmpty()) {
                /* create primary key index */
                stringBuilder.append(", PRIMARY KEY (")
                        .append(String.join(",", data.getConstraints()
                                .getPrimaryKey()
                                .stream()
                                .map(c -> {
                                    final Optional<ColumnCreateDto> optional = data.getColumns()
                                            .stream()
                                            .filter(cc -> cc.getName().equals(c))
                                            .findFirst();
                                    log.trace("lookup {} in columns: {}", c, data.getColumns().stream().map(ColumnCreateDto::getName).toList());
                                    return "`" + nameToInternalName(c) + "`" + columnCreateDtoToPrimaryKeyLengthSpecification(optional.get());
                                })
                                .toArray(String[]::new)))
                        .append(")");
            }
            if (data.getConstraints().getUniques() != null) {
                /* create unique indices */
                data.getConstraints().getUniques()
                        .forEach(u -> stringBuilder.append(", ")
                                .append("UNIQUE KEY (`")
                                .append(u.stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                .append("`)"));
            }
            if (data.getConstraints().getForeignKeys() != null) {
                /* create foreign key indices */
                data.getConstraints().getForeignKeys()
                        .forEach(fk -> {
                            stringBuilder.append(", FOREIGN KEY (`")
                                    .append(fk.getColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`) REFERENCES `")
                                    .append(nameToInternalName(fk.getReferencedTable()))
                                    .append("` (`")
                                    .append(fk.getReferencedColumns().stream().map(this::nameToInternalName).collect(Collectors.joining("`,`")))
                                    .append("`)");
                            if (fk.getOnDelete() != null) {
                                stringBuilder.append(" ON DELETE ").append(fk.getOnDelete());
                            }
                            if (fk.getOnUpdate() != null) {
                                stringBuilder.append(" ON UPDATE ").append(fk.getOnUpdate());
                            }
                        });
            }
            if (data.getConstraints().getChecks() != null) {
                /* create check constraints */
                data.getConstraints().getChecks()
                        .forEach(ck -> stringBuilder.append(", ")
                                .append("CHECK (")
                                .append(ck)
                                .append(")"));
            }
            if (data.getDescription() != null && !data.getDescription().isBlank()) {
                /* create table comments */
                stringBuilder.append(" COMMENT \"")
                        .append(data.getDescription())
                        .append("\"");
            }
        }
        stringBuilder.append(") WITH SYSTEM VERSIONING");
        if (data.getDescription() != null && !data.getDescription().isBlank()) {
            /* create table comments */
            stringBuilder.append(" COMMENT \"")
                    .append(data.getDescription())
                    .append("\"");
        }
        stringBuilder.append(";");
        log.trace("mapped create table statement: {}", stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * Selects the row count from a table/view.
     *
     * @param databaseName The database internal name.
     * @param tableOrView  The table/view internal name.
     * @param timestamp    The moment in time the data should be returned in UTC timezone.
     * @return The raw SQL query.
     */
    default String selectCountRawQuery(String databaseName, String tableOrView, Instant timestamp) {
        final StringBuilder statement = new StringBuilder("SELECT COUNT(1) FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableOrView)
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(";");
        return statement.toString();
    }

    default Long resultSetToNumber(ResultSet data) throws QueryMalformedException, SQLException {
        if (!data.next()) {
            throw new QueryMalformedException("Failed to map number");
        }
        return data.getLong(1);
    }

    /**
     * Selects the dataset page from a table/view.
     *
     * @param databaseName The database internal name.
     * @param tableOrView  The table/view internal name.
     * @param columns      The columns that should be contained in the result set.
     * @param timestamp    The moment in time the data should be returned in UTC timezone.
     * @return The raw SQL query.
     */
    default String selectDatasetRawQuery(String databaseName, String tableOrView, List<ColumnDto> columns,
                                         Instant timestamp, Long size, Long page) {
        final int[] idx = new int[]{0};
        final StringBuilder statement = new StringBuilder("SELECT ");
        columns.forEach(column -> statement.append(idx[0]++ > 0 ? "," : "")
                .append("`")
                .append(column.getInternalName())
                .append("`"));
        statement.append(" FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableOrView)
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        log.trace("pagination size/limit of {}", size);
        statement.append(" LIMIT ")
                .append(size);
        log.trace("pagination page/offset of {}", page);
        statement.append(" OFFSET ")
                .append(page * size)
                .append(";");
        log.trace("mapped select data query: {}", statement);
        return statement.toString();
    }

    /**
     * Selects the dataset page from a table/view.
     *
     * @param databaseName The database internal name.
     * @param table        The table internal name.
     * @return The raw SQL query.
     */
    default String selectHistoryRawQuery(String databaseName, String table, Long size) {
        final StringBuilder statement = new StringBuilder("SELECT IF(`deleted_at` IS NULL, `inserted_at`, `deleted_at`) as `timestamp`, IF(`deleted_at` IS NULL, 'INSERT', 'DELETE') as `event`, total FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(1) as total FROM `")
                .append(databaseName)
                .append("`.`")
                .append(table)
                .append("` FOR SYSTEM_TIME ALL GROUP BY inserted_at, deleted_at ORDER BY deleted_at DESC) AS v ORDER BY v.inserted_at, v.deleted_at ASC LIMIT ")
                .append(size)
                .append(";");
        log.trace("mapped history query: {}", statement);
        return statement.toString();
    }

    @Named("dropTableQuery")
    default String dropTableRawQuery(String tableName) {
        return "DROP TABLE `" + tableName + "`;";
    }

    default String tableOrViewToRawExportQuery(String databaseName, String tableOrView, List<ColumnDto> columns,
                                               Instant timestamp, String filePath) {
        final StringBuilder statement = new StringBuilder("SELECT ");
        int[] idx = new int[]{0};
        columns.forEach(column -> {
            statement.append(idx[0] != 0 ? "," : "")
                    .append("'")
                    .append(column.getInternalName())
                    .append("'");
            idx[0]++;
        });
        statement.append(" UNION ALL SELECT ");
        int[] jdx = new int[]{0};
        columns.forEach(column -> {
            statement.append(jdx[0] != 0 ? "," : "")
                    .append("`")
                    .append(column.getInternalName())
                    .append("`");
            jdx[0]++;
        });
        statement.append(" FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableOrView)
                .append("`");
        if (timestamp != null) {
            log.trace("export has timestamp present");
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(" INTO OUTFILE '")
                .append(filePath)
                .append("' CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"';");
        statement.append(";");
        log.debug("mapped table/view export query: {}", statement);
        return statement.toString();
    }

    default String subsetToRawExportQuery(String query, Instant timestamp, String filePath) {
        final StringBuilder statement = new StringBuilder(query.replaceAll(";", ""))
                .append(" FOR SYSTEM_TIME AS OF TIMESTAMP'")
                .append(mariaDbFormatter.format(timestamp))
                .append("'")
                .append(" INTO OUTFILE '")
                .append(filePath)
                .append("' CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"';");
        log.debug("mapped export query: {}", statement);
        return statement.toString();
    }

    default String datasetToRawInsertQuery(String databaseName, PrivilegedTableDto table, ImportCsvDto data) {
        final StringBuilder statement = new StringBuilder("LOAD DATA INFILE '")
                .append(data.getLocation())
                .append("' REPLACE INTO TABLE `")
                .append(databaseName)
                .append("`.`")
                .append(table.getInternalName())
                .append("` CHARACTER SET utf8 FIELDS TERMINATED BY '")
                .append(data.getSeparator())
                .append("'");
        if (data.getQuote() != null) {
            statement.append(" OPTIONALLY ENCLOSED BY '")
                    .append(data.getQuote())
                    .append("'");
        }
        statement.append(" LINES TERMINATED BY '")
                .append(data.getLineTermination())
                .append("'")
                .append(data.getSkipLines() != null ? (" IGNORE " + data.getSkipLines() + " LINES") : "")
                .append(" (");
        final StringBuilder set = new StringBuilder();
        int[] idx = new int[]{0};
        table.getColumns()
                .forEach(column -> {
                    if (column.getAutoGenerated()) {
                        log.trace("import column is auto generated, skip");
                        return;
                    }
                    statement.append(idx[0] != 0 ? "," : "");
                    /* format as variable */
                    statement.append("@")
                            .append(column.getInternalName());
                    if (column.getDateFormat() != null) {
                        /* reformat dates */
                        columnToDateSet(data, column, set);
                    } else if (column.getColumnType().equals(ColumnTypeDto.BOOL)) {
                        /* reformat booleans */
                        columnToBoolSet(data, column, set);
                    } else {
                        /* reformat others */
                        columnToTextSet(data, column, set);
                    }
                    idx[0]++;
                });
        statement.append(")")
                .append(set.length() != 0 ? (" SET " + set) : "")
                .append(";");
        log.trace("mapped insert statement: {}", statement);
        return statement.toString();
    }


    default String tupleToRawDeleteQuery(PrivilegedTableDto table, TupleDeleteDto data) throws TableMalformedException {
        log.trace("table csv to delete query, table.id={}, data.keys={}", table.getId(), data.getKeys());
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("DELETE FROM `")
                .append(table.getInternalName())
                .append("` WHERE ");
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> statement.append(idx[0]++ == 0 ? "" : " AND ")
                        .append("`")
                        .append(key)
                        .append("` ")
                        .append(data.getKeys().get(key) == null ? "IS" : "=")
                        .append(" ?"));
        log.trace("mapped delete tuple query {}", statement);
        return statement.toString();
    }

    default String tupleToRawUpdateQuery(PrivilegedTableDto table, TupleUpdateDto data)
            throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("UPDATE `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("` SET ");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    statement.append(idx[0]++ == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` = ?");
                });
        statement.append(" WHERE ");
        final int[] jdx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> {
                    statement.append(jdx[0] == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("` ");
                    if (value == null) {
                        statement.append(" IS NULL");
                    } else {
                        statement.append(" = ?");
                    }
                    jdx[0]++;
                });
        statement.append(";");
        log.trace("mapped update query: {}", statement);
        return statement.toString();
    }

    default String tupleToRawCreateQuery(PrivilegedTableDto table, TupleDto data) throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(table.getDatabase().getInternalName())
                .append("`.`")
                .append(table.getInternalName())
                .append("` (");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    final Optional<ColumnDto> optional = table.getColumns().stream()
                            .filter(c -> c.getInternalName().equals(key))
                            .findFirst();
                    if (optional.isEmpty()) {
                        log.error("Failed to find table column {}", key);
                        throw new IllegalArgumentException("Failed to find table column");
                    }
                    if (optional.get().getAutoGenerated()) {
                        return;
                    }
                    statement.append(idx[0]++ == 0 ? "" : ", ")
                            .append("`")
                            .append(key)
                            .append("`");
                });
        statement.append(") VALUES (");
        final int[] jdx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    final Optional<ColumnDto> optional = table.getColumns().stream()
                            .filter(c -> c.getInternalName().equals(key))
                            .findFirst();
                    if (optional.isEmpty()) {
                        log.error("Failed to find table column {}", key);
                        throw new IllegalArgumentException("Failed to find table column");
                    }
                    if (optional.get().getAutoGenerated()) {
                        return;
                    }
                    statement.append(jdx[0]++ == 0 ? "" : ", ")
                            .append("?");
                });
        statement.append(");");
        log.trace("mapped create tuple query: {}", statement);
        return statement.toString();
    }

    default void columnToDateSet(ImportCsvDto data, ColumnDto column, StringBuilder set) {
        log.trace("import column has date format, need to format it: {}", column.getDateFormat().getUnixFormat());
        set.append(!set.isEmpty() ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = STR_TO_DATE(");
        if (data.getNullElement() != null) {
            set.append("IF(STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'), @")
                    .append(column.getInternalName())
                    .append(", NULL), '")
                    .append(column.getDateFormat()
                            .getDatabaseFormat()
                            .replace('\'', '\\'))
                    .append("')");
            return;
        }
        set.append("@")
                .append(column.getInternalName())
                .append(", '")
                .append(column.getDateFormat()
                        .getDatabaseFormat()
                        .replace('\'', '\\'))
                .append("')");
    }

    default void columnToBoolSet(ImportCsvDto data, ColumnDto column, StringBuilder set) {
        set.append(!set.isEmpty() ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'),NULL,");
            columnToBoolSet2(data, column, set);
            set.append(")");
            return;
        }
        columnToBoolSet2(data, column, set);
    }

    default void columnToBoolSet2(ImportCsvDto data, ColumnDto column, StringBuilder set) {
        if (data.getTrueElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getTrueElement())
                    .append("'),TRUE,");
            if (data.getFalseElement() != null) {
                log.trace("import has false element present (both true and false)");
                /* can map both true/false */
                set.append("IF(!STRCMP(@")
                        .append(column.getInternalName())
                        .append(",'")
                        .append(data.getFalseElement())
                        .append("'),FALSE,@")
                        .append(column.getInternalName())
                        .append("))");
            } else {
                /* can only map true */
                set.append("@")
                        .append(column.getInternalName())
                        .append(")");
            }
            return;
        }
        if (data.getFalseElement() != null) {
            set.append("IF(!STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getFalseElement())
                    .append("'),FALSE,");
            if (data.getTrueElement() != null) {
                log.trace("import has true element present (both true and false)");
                /* can map both true/false */
                set.append("IF(!STRCMP(@")
                        .append(column.getInternalName())
                        .append(",'")
                        .append(data.getTrueElement())
                        .append("'),TRUE,@")
                        .append(column.getInternalName())
                        .append("))");
            } else {
                /* can only map true */
                set.append("@")
                        .append(column.getInternalName())
                        .append(")");
            }
            return;
        }
        set.append("@")
                .append(column.getInternalName());
    }

    default void columnToTextSet(ImportCsvDto data, ColumnDto column, StringBuilder set) {
        set.append(!set.isEmpty() ? ", " : "")
                .append("`")
                .append(column.getInternalName())
                .append("` = ");
        if (data.getNullElement() != null) {
            set.append("IF(STRCMP(@")
                    .append(column.getInternalName())
                    .append(",'")
                    .append(data.getNullElement())
                    .append("'), @")
                    .append(column.getInternalName())
                    .append(", NULL)");
            return;
        }
        set.append("@")
                .append(column.getInternalName());
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement statement, ColumnTypeDto columnType, int idx,
                                                      String columnName, Object value) throws SQLException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.BLOB);
                    break;
                }
                try {
                    final ByteArrayOutputStream boas = new ByteArrayOutputStream();
                    try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
                        ois.writeObject(value);
                        statement.setBlob(idx, new ByteArrayInputStream(boas.toByteArray()));
                        log.trace("prepare statement idx {} = {} blob", idx, columnName);
                    }

                } catch (IOException e) {
                    log.error("Failed to set blob/tinyblob/mediumblob/longblob: {}", e.getMessage());
                    throw new SQLException("Failed to set blob: " + e.getMessage(), e);
                }
                break;
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.VARCHAR);
                    break;
                }
                log.trace("prepare statement idx {} = {} text/char/varchar/tinytext/mediumtext/longtext/enum/set: {}", idx, columnName, value);
                statement.setString(idx, String.valueOf(value));
                break;
            case DATE:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.DATE);
                    break;
                }
                log.trace("prepare statement idx {} date: {}", idx, value);
                statement.setDate(idx, Date.valueOf(String.valueOf(value)));
                break;
            case BIGINT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.BIGINT);
                    break;
                }
                log.trace("prepare statement idx {} bigint: {}", idx, value);
                statement.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.INTEGER);
                    break;
                }
                log.trace("prepare statement idx {} = {} int/mediumint: {}", idx, columnName, value);
                statement.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case TINYINT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.TINYINT);
                    break;
                }
                log.trace("prepare statement idx {} = {} tinyint: {}", idx, columnName, value);
                statement.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case SMALLINT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.SMALLINT);
                    break;
                }
                log.trace("prepare statement idx {} = {} smallint: {}", idx, columnName, value);
                statement.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case DECIMAL:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.DECIMAL);
                    break;
                }
                log.trace("prepare statement idx {} = {} decimal: {}", idx, columnName, value);
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case FLOAT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.FLOAT);
                    break;
                }
                log.trace("prepare statement idx {} = {} float: {}", idx, columnName, value);
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case DOUBLE:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.DOUBLE);
                    break;
                }
                log.trace("prepare statement idx {} = {} double: {}", idx, columnName, value);
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.DECIMAL);
                    break;
                }
                log.trace("prepare statement idx {} = {} binary/varbinary/bit", idx, columnName);
                statement.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.BOOLEAN);
                    break;
                }
                log.trace("prepare statement idx {} = {} bool: {}", idx, columnName, value);
                statement.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIMESTAMP, DATETIME:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                log.trace("prepare statement idx {} timestamp/datetime: {}", idx, value);
                statement.setTimestamp(idx, Timestamp.valueOf(String.valueOf(value)));
                break;
            case TIME:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.TIME);
                    break;
                }
                log.trace("prepare statement idx {} = {} time: {}", idx, columnName, value);
                statement.setTime(idx, Time.valueOf(String.valueOf(value)));
                break;
            case YEAR:
                if (value == null) {
                    log.trace("idx {} = {} is null, prepare with null value", idx, columnName);
                    statement.setNull(idx, Types.TIME);
                    break;
                }
                log.trace("prepare statement idx {} = {} year: {}", idx, columnName, value);
                statement.setString(idx, String.valueOf(value));
                break;
            default:
                log.error("Failed to map column type {} at idx {} = {} for value {}", columnType, idx, columnName, value);
                throw new IllegalArgumentException("Failed to map column type " + columnType);
        }
    }

    default String selectRawSelectQuery(String query, Instant timestamp, Long page, Long size) {
        query = query.toLowerCase(Locale.ROOT)
                .trim();
        if (query.matches(";$")) {
            /* remove last semicolon */
            query = query.substring(0, query.length() - 1);
        }
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT * FROM (")
                .append(query)
                .append(") FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(mariaDbFormatter.format(timestamp))
                .append("' as tbl");
        /* pagination */
        log.trace("pagination size/limit of {}", size);
        statement.append(" LIMIT ")
                .append(size);
        log.trace("pagination page/offset of {}", page);
        statement.append(" OFFSET ")
                .append(page * size);
        statement.append(";");
        log.trace("mapped select query: {}", statement);
        return statement.toString();
    }

    default String countRawSelectQuery(String query, Instant timestamp) {
        query = query.toLowerCase(Locale.ROOT)
                .trim();
        if (query.matches(";$")) {
            /* remove last semicolon */
            query = query.substring(0, query.length() - 1);
        }
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT COUNT(1) FROM (")
                .append(query)
                .append(") FOR SYSTEM_TIME AS OF TIMESTAMP '")
                .append(mariaDbFormatter.format(timestamp))
                .append("' as tbl;");
        log.trace("mapped count query: {}", statement);
        return statement.toString();
    }

}
