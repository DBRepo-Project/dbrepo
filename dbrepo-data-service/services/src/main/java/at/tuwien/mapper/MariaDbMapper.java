package at.tuwien.mapper;

import at.tuwien.api.database.table.TupleDeleteDto;
import at.tuwien.api.database.table.TupleDto;
import at.tuwien.api.database.table.TupleUpdateDto;
import at.tuwien.api.database.table.columns.ColumnCreateDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableMalformedException;
import at.tuwien.utils.MariaDbUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    default String databaseRevokePrivilegesQuery(String username) {
        final StringBuilder statement = new StringBuilder("REVOKE ALL PRIVILEGES ON *.* FROM `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped revoke privileges statement: {}", statement);
        return statement.toString();
    }

    default String databaseGrantProcedureQuery(String username, String procedure) {
        final StringBuilder statement = new StringBuilder("GRANT EXECUTE ON PROCEDURE `")
                .append(procedure)
                .append("` TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped revoke privileges statement: {}", statement);
        return statement.toString();
    }

    default String databaseFlushPrivilegesQuery() {
        final String statement = "FLUSH PRIVILEGES;";
        log.trace("mapped flush privileges statement: {}", statement);
        return statement;
    }

    @Named("createDatabase")
    default String databaseCreateDatabaseQuery(String database) {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE `")
                .append(database)
                .append("`");
        log.trace("mapped create database statement: {}", statement);
        return statement.toString();
    }

    default String queryStoreCreateSequenceRawQuery() {
        final String statement = "CREATE SEQUENCE `qs_queries_seq` NOCACHE;";
        log.trace("mapped create query store sequence statement: {}", statement);
        return statement;
    }

    default String queryStoreCreateTableRawQuery() {
        final String statement = "CREATE TABLE `qs_queries` ( `id` bigint not null primary key default nextval(`qs_queries_seq`), `created` datetime not null default now(), `executed` datetime not null default now(), `created_by` varchar(36), `query` text not null, `query_normalized` text not null, `is_persisted` boolean not null, `query_hash` varchar(255) not null, `result_hash` varchar(255), `result_number` bigint) WITH SYSTEM VERSIONING;";
        log.trace("mapped create query store table statement: {}", statement);
        return statement;
    }

    default String queryStoreCreateHashTableProcedureRawQuery() {
        final String statement = "CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT hash VARCHAR(255), OUT count BIGINT) BEGIN DECLARE _sql TEXT; SELECT CONCAT('SELECT SHA2(GROUP_CONCAT(CONCAT_WS(\\'\\',', GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY column_name), ') SEPARATOR \\',\\'), 256) AS hash, COUNT(*) AS count FROM `', name, '` INTO @hash, @count;') FROM `information_schema`.`columns` WHERE `table_schema` = DATABASE() AND `table_name` = name INTO _sql; PREPARE stmt FROM _sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; SET hash = @hash; SET count = @count; END;";
        log.trace("mapped create query store hash_table procedure statement: {}", statement);
        return statement;
    }

    default String queryStoreCreateStoreQueryProcedureRawQuery() {
        final String statement = "CREATE PROCEDURE store_query(IN query TEXT, IN executed DATETIME, OUT queryId BIGINT) BEGIN DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256); DECLARE _username varchar(255) DEFAULT REGEXP_REPLACE(current_user(), '@.*', ''); DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')'); PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash, @count); DROP TABLE IF EXISTS `_tmp`; IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF; END;";
        log.trace("mapped create query store store_query procedure statement: {}", statement);
        return statement;
    }

    default String queryStoreCreateInternalStoreQueryProcedureRawQuery() {
        final String statement = "CREATE DEFINER = 'root' PROCEDURE _store_query(IN _username VARCHAR(255), IN query TEXT, IN executed DATETIME, OUT queryId BIGINT) BEGIN DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256); DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')'); PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash, @count); DROP TABLE IF EXISTS `_tmp`; IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF; END;";
        log.trace("mapped create query store _store_query procedure statement: {}", statement);
        return statement;
    }

    default String queryStoreStoreQueryRawQuery() {
        final String statement = "{call _store_query(?, ?, ?, ?)}";
        log.trace("mapped store query statement: {}", statement);
        return statement;
    }

    default String queryStoreUpdateQueryRawQuery() {
        final String statement = "UPDATE `qs_queries` SET `is_persisted` = ? WHERE `id` = ?";
        log.trace("mapped update query statement: {}", statement);
        return statement;
    }

    default String queryStoreDeleteStaleQueriesRawQuery() {
        final String statement = "DELETE FROM `qs_queries` WHERE `is_persisted` = false AND ABS(DATEDIFF(`created`, NOW())) >= 1";
        log.trace("mapped delete stale queries statement: {}", statement);
        return statement;
    }

    default String queryStoreFindQueryRawQuery() {
        final String statement = "SELECT `id`, `created`, `created_by`, `query`, `query_hash`, `result_hash`, `result_number`, `is_persisted`, `executed` FROM `qs_queries` q WHERE q.`id` = ?";
        log.trace("mapped find query statement: {}", statement);
        return statement;
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

    @Named("dropView")
    default String dropViewRawQuery(String viewName) {
        final String statement = "DROP VIEW IF EXISTS `" + viewName + "`;";
        log.trace("mapped drop view statement: {}", statement);
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
        final String statement = "SELECT k.`ORDINAL_POSITION`, c.`CONSTRAINT_TYPE`, k.`CONSTRAINT_NAME`, k.`COLUMN_NAME`, k.`REFERENCED_TABLE_NAME`, k.`REFERENCED_COLUMN_NAME`, r.`DELETE_RULE`, r.`UPDATE_RULE` FROM information_schema.TABLE_CONSTRAINTS c JOIN information_schema.KEY_COLUMN_USAGE k ON c.`TABLE_NAME` = k.`TABLE_NAME` AND c.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` AND c.`CONSTRAINT_SCHEMA` = k.`CONSTRAINT_SCHEMA` LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS r ON r.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` AND r.`CONSTRAINT_SCHEMA` = c.`TABLE_SCHEMA` AND r.`TABLE_NAME` = c.`TABLE_NAME` WHERE LOWER(k.`COLUMN_NAME`) != 'row_end' AND c.`TABLE_SCHEMA` = ? AND c.`TABLE_NAME` = ? ORDER BY k.`ORDINAL_POSITION` ASC;";
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
                        .append("`) as max, AVG(`")
                        .append(column.getInternalName())
                        .append("`) as median, AVG(`")
                        .append(column.getInternalName())
                        .append("`) as mean, STDDEV(`")
                        .append(column.getInternalName())
                        .append("`) as std_dev FROM ")
                        .append(table));
        if (statement.isEmpty()) {
            return null;
        }
        statement.append(";");
        log.trace("mapped select column statistic statement: {}", statement);
        return statement.toString();
    }

    default String tableNameToUpdateTableRawQuery(String internalName) {
        final StringBuilder stringBuilder = new StringBuilder("ALTER TABLE `")
                .append(internalName)
                .append("` COMMENT = ?;");
        log.trace("mapped update table statement: {}", stringBuilder);
        return stringBuilder.toString();
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
                    .append(column.getNullAllowed() != null && column.getNullAllowed() ? " NULL" : " NOT NULL");
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

    default String selectExistsTableOrViewRawQuery() {
        final String statement = "SELECT IF((SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?), 1, 0) AS `exists`";
        log.trace("mapped select exists table or view statement: {}", statement);
        return statement;
    }

    default Long resultSetToNumber(ResultSet data) throws QueryMalformedException, SQLException {
        if (!data.next()) {
            throw new QueryMalformedException("Failed to map number");
        }
        return data.getLong(1);
    }

    default Boolean resultSetToBoolean(ResultSet data) throws QueryMalformedException, SQLException {
        if (!data.next()) {
            throw new QueryMalformedException("Failed to map boolean");
        }
        return data.getBoolean(1);
    }

    default List<Map<String, Object>> resultSetToList(ResultSet data, List<String> columns) throws SQLException {
        final List<Map<String, Object>> list = new LinkedList<>();
        while (data.next()) {
            final Map<String, Object> tuple = new HashMap<>();
            for (String column : columns) {
                tuple.put(column, data.getString(column));
            }
            list.add(tuple);
        }
        return list;
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
        return dropTableRawQuery(tableName, true);
    }

    default String dropTableRawQuery(String tableName, Boolean force) {
        final StringBuilder statement = new StringBuilder("DROP TABLE ");
        if (!force) {
            statement.append("IF EXISTS ");
        }
        statement.append("`")
                .append(tableName)
                .append("`;");
        log.trace("mapped drop table query: {}", statement);
        return statement.toString();
    }

    default String temporaryTableToRawMergeQuery(String tmp, String table, List<String> columns) {
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(table)
                .append("` SELECT * FROM `")
                .append(tmp)
                .append("` ON DUPLICATE KEY UPDATE ");
        final int[] idx = new int[]{0};
        columns.forEach(column -> statement.append(idx[0]++ > 0 ? ", " : "")
                .append("`")
                .append(table)
                .append("`.`")
                .append(column)
                .append("` = `")
                .append(tmp)
                .append("`.`")
                .append(column)
                .append("`"));
        statement.append(";");
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
                    statement.append(jdx[0] == 0 ? "" : " AND ")
                            .append("`")
                            .append(key)
                            .append("`");
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
                    statement.append(jdx[0]++ == 0 ? "" : ", ")
                            .append("?");
                });
        statement.append(");");
        log.trace("mapped create tuple query: {}", statement);
        return statement.toString();
    }

    default void prepareStatementWithColumnTypeObject(PreparedStatement statement, ColumnTypeDto columnType, int idx,
                                                      String columnName, Object value) throws SQLException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                if (value == null) {
                    statement.setNull(idx, Types.BLOB);
                    break;
                }
                final byte[] data = (byte[]) value;
                statement.setBlob(idx, new ByteArrayInputStream(data));
                break;
            case TEXT, CHAR, VARCHAR, TINYTEXT, MEDIUMTEXT, LONGTEXT, ENUM, SET:
                if (value == null) {
                    statement.setNull(idx, Types.VARCHAR);
                    break;
                }
                statement.setString(idx, String.valueOf(value));
                break;
            case DATE:
                if (value == null) {
                    statement.setNull(idx, Types.DATE);
                    break;
                }
                statement.setString(idx, String.valueOf(value));
                break;
            case BIGINT, SERIAL:
                if (value == null) {
                    statement.setNull(idx, Types.BIGINT);
                    break;
                }
                statement.setLong(idx, Long.parseLong(String.valueOf(value)));
                break;
            case INT, MEDIUMINT:
                if (value == null) {
                    statement.setNull(idx, Types.INTEGER);
                    break;
                }
                statement.setLong(idx, Integer.parseInt(String.valueOf(value)));
                break;
            case TINYINT:
                if (value == null) {
                    statement.setNull(idx, Types.TINYINT);
                    break;
                }
                statement.setLong(idx, Integer.parseInt(String.valueOf(value)));
                break;
            case SMALLINT:
                if (value == null) {
                    statement.setNull(idx, Types.SMALLINT);
                    break;
                }
                statement.setInt(idx, Integer.parseInt(String.valueOf(value)));
                break;
            case DECIMAL:
                if (value == null) {
                    statement.setNull(idx, Types.DECIMAL);
                    break;
                }
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case FLOAT:
                if (value == null) {
                    statement.setNull(idx, Types.FLOAT);
                    break;
                }
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case DOUBLE:
                if (value == null) {
                    statement.setNull(idx, Types.DOUBLE);
                    break;
                }
                statement.setDouble(idx, Double.parseDouble(String.valueOf(value)));
                break;
            case BINARY, VARBINARY, BIT:
                if (value == null) {
                    statement.setNull(idx, Types.DECIMAL);
                    break;
                }
                statement.setBinaryStream(idx, (InputStream) value);
                break;
            case BOOL:
                if (value == null) {
                    statement.setNull(idx, Types.BOOLEAN);
                    break;
                }
                statement.setBoolean(idx, Boolean.parseBoolean(String.valueOf(value)));
                break;
            case TIMESTAMP, DATETIME:
                if (value == null) {
                    statement.setNull(idx, Types.TIMESTAMP);
                    break;
                }
                statement.setString(idx, String.valueOf(value));
                break;
            case TIME, YEAR:
                if (value == null) {
                    statement.setNull(idx, Types.TIME);
                    break;
                }
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
