package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDeleteDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Operator;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import org.apache.logging.log4j.util.Strings;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

@Mapper(componentModel = "spring", uses = {DataMapper.class, DataMapper.class})
public interface PostgresMapper {

    Logger log = LoggerFactory.getLogger(PostgresMapper.class);

    DateTimeFormatter sqlDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS]")
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
        final StringBuilder statement = new StringBuilder("ALTER USER ")
                .append(username)
                .append("@% IDENTIFIED BY '")
                .append(password)
                .append("';");
        log.trace("mapped set password statement: {}", statement);
        return statement.toString();
    }

    default String databaseFindAccessQuery() {
        final StringBuilder statement = new StringBuilder("SHOW GRANTS FOR ?;");
        log.trace("mapped database find access statement: {}", statement);
        return statement.toString();
    }

    default Map<String, Set<String>> resultSetToGrants(ResultSet resultSet) throws SQLException,
            DatabaseMalformedException {
        final Pattern grantPattern = Pattern.compile("GRANT (.*) ON");
        final Matcher grantMatcher = grantPattern.matcher(resultSet.getString(1));
        final Set<String> grants = new HashSet<>();
        if (grantMatcher.find()) {
            Arrays.asList(grantMatcher.group(1)
                            .split(","))
                    .forEach(g -> grants.add(g.trim()));
        } else {
            log.debug("no grants were found in the result set");
        }
        final Map<String, Set<String>> map = new HashMap<>();
        final Pattern databasePattern = Pattern.compile("ON ?([a-zA-Z0-9*_]+)?");
        final Matcher databaseMatcher = databasePattern.matcher(resultSet.getString(1));
        if (databaseMatcher.find()) {
            final String databaseName = databaseMatcher.group(1)
                    .trim();
            if (!databaseName.equals("PROCEDURE")) {
                map.put(databaseName, grants);
                log.trace("grant on {} has privilege(s): {}", databaseName, grants);
            }
            return map;
        }
        log.debug("no database name was found in the result set");
        throw new DatabaseMalformedException("No database name was found in the result set");
    }

    default String databaseCreateUserQuery(String username, String password) {
        final StringBuilder statement = new StringBuilder("DO $$ BEGIN IF EXISTS ( SELECT FROM pg_catalog.pg_roles WHERE rolname = '")
                .append(username)
                .append("') THEN RAISE NOTICE 'Role already exists. Skipping.'; ELSE CREATE ROLE ")
                .append(username)
                .append(" CONNECTION LIMIT 5 LOGIN PASSWORD '")
                .append(password)
                .append("'; END IF; END $$;");
        log.trace("mapped create user query: {}", statement);
        return statement.toString();
    }

    default String databaseGrantPrivilegesQuery(String database, String username, String grants) {
        final StringBuilder statement = new StringBuilder("GRANT CONNECT ON DATABASE ")
                .append(database)
                .append(" TO ")
                .append(username)
                .append(";");
        log.trace("mapped grant privileges statement: {}", statement);
        return statement.toString();
    }

    default String databaseRevokePrivilegesQuery(String database, String username) {
        final StringBuilder statement = new StringBuilder("REVOKE ALL ON ")
                .append(database)
                .append(" FROM ")
                .append(username)
                .append(";");
        log.trace("mapped revoke privileges statement: {}", statement);
        return statement.toString();
    }

    @Named("createDatabase")
    default String databaseCreateDatabaseQuery(String database) {
        final StringBuilder statement = new StringBuilder("CREATE DATABASE ")
                .append(database);
        log.trace("mapped create database statement: {}", statement);
        return statement.toString();
    }

    default String createExtensionRawQuery(String extension) {
        final String statement = "CREATE EXTENSION " + extension + " CASCADE;";
        log.trace("mapped create extension statement: {}", statement);
        return statement;
    }

    default String queryStoreStoreQueryRawQuery() {
        final String statement = "CALL dbrepo.store_query(?, ?, ?, ?)";
        log.trace("mapped store query statement: {}", statement);
        return statement;
    }

    default String queryStoreUpdateQueryRawQuery() {
        final String statement = "CALL dbrepo.persist(?, ?)";
        log.trace("mapped update query statement: {}", statement);
        return statement;
    }

    default String queryStoreFindQueryRawQuery() {
        final String statement = "SELECT id, created_by, query, query_normalized, query_hash, result_hash, result_number, is_persisted, executed FROM dbrepo.queries q WHERE q.id = ?";
        log.trace("mapped find query statement: {}", statement);
        return statement;
    }

    default String databaseTablesSelectRawQuery() {
        final String statement = "SELECT DISTINCT t.TABLE_NAME FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = 'query_store' AND t.TABLE_TYPE = 'SYSTEM VERSIONED' AND t.TABLE_NAME != 'queries' ORDER BY t.TABLE_NAME ASC";
        log.trace("mapped select tables statement: {}", statement);
        return statement;
    }

    default String queryStoreHashQueryResultRawQuery() {
        final String statement = "CALL dbrepo.hash_query_result(?, ?)";
        log.trace("mapped hash query result statement: {}", statement);
        return statement;
    }

    default String databaseTableSelectRawQuery() {
        final String statement = "SELECT t.table_name, t.table_type, -1 as table_rows, -1 as avg_row_length, -1 as data_length, -1 as max_data_length, NOW() as create_time, NOW() as update_time, v.view_definition, (SELECT obj_description(c.oid) FROM pg_class c WHERE c.relkind = 'r' AND c.relname = t.table_name) FROM information_schema.tables t LEFT JOIN information_schema.views v ON v.table_name = t.table_name WHERE t.table_schema = 'public' AND t.table_name NOT LIKE '%_history' AND t.table_name != 'queries' AND t.table_name = ?;";
        log.trace("mapped select table statement: {}", statement);
        return statement;
    }

    @Named("dropView")
    default String dropViewRawQuery(String viewName) {
        final StringBuilder statement = new StringBuilder("DROP VIEW ")
                .append(viewName)
                .append(";");
        log.trace("mapped drop view statement: {}", statement);
        return statement.toString();
    }

    default String refreshMaterializedViewRawQuery(String viewName) {
        final StringBuilder statement = new StringBuilder("REFRESH MATERIALIZED VIEW ")
                .append(viewName)
                .append(";");
        log.trace("mapped refresh materialized view statement: {}", statement);
        return statement.toString();
    }

    default String columnsCheckConstraintSelectRawQuery(String tableName) {
        final String statement = "SELECT pg_get_constraintdef(oid) as check_clause FROM pg_constraint c JOIN pg_attribute a ON a.attrelid = c.conrelid  AND a.attnum = ANY (c.conkey)  WHERE c.conrelid = '" + tableName + "'::regclass AND c.contype = 'c';";
        log.trace("mapped select column constraint statement: {}", statement);
        return statement;
    }

    default String databaseTableColumnsSelectRawQuery() {
        final String statement = "SELECT ordinal_position, column_default, is_nullable, data_type, character_maximum_length, numeric_precision, numeric_scale, column_name, (SELECT pgd.description FROM pg_catalog.pg_statio_all_tables as st INNER JOIN pg_catalog.pg_description pgd on (pgd.objoid = st.relid) INNER JOIN information_schema.columns c ON (pgd.objsubid = c.ordinal_position AND c.table_schema = st.schemaname AND c.table_name = st.relname)) FROM information_schema.columns WHERE TABLE_SCHEMA = 'public' AND TABLE_NAME = ? AND column_name NOT IN ('row_start', 'row_end') ORDER BY ORDINAL_POSITION;";
        log.trace("mapped select columns statement: {}", statement);
        return statement;
    }

    default String databaseTableConstraintsSelectRawQuery() {
        final String statement = "SELECT kcu.ordinal_position, tc.constraint_type, kcu.constraint_name, kcu.column_name, pgc.confrelid::regclass AS referenced_table_name, ccu.column_name as referenced_column_name, rfc.delete_rule, rfc.update_rule FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_catalog = kcu.constraint_catalog AND tc.constraint_schema = kcu.constraint_schema AND tc.constraint_name = kcu.constraint_name LEFT JOIN information_schema.referential_constraints rfc ON tc.constraint_name = rfc.constraint_name AND tc.constraint_schema = rfc.constraint_schema LEFT JOIN pg_constraint pgc ON pgc.conname = kcu.constraint_name JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name WHERE tc.constraint_schema = 'public' AND LOWER(kcu.column_name) != 'row_end' AND tc.table_name = ? ORDER BY kcu.ordinal_position;";
        log.trace("mapped select table constraints statement: {}", statement);
        return statement;
    }

    /**
     * Creates a view with given name from a provided query statement. Currently, preparing statements for database name and table name are not supported by the driver.
     *
     * @param viewName The view name.
     * @param query    The query statement.
     * @return The raw query statement to create the view.
     */
    default String viewCreateRawQuery(String viewName, String query, Boolean isMaterialized) {
        final StringBuilder statement = new StringBuilder("CREATE ");
        if (isMaterialized) {
            statement.append("MATERIALIZED ");
        }
        statement.append("VIEW ")
                .append(viewName)
                .append(" AS (")
                .append(query)
                .append(")");
        log.trace("mapped create view statement: {}", statement);
        return statement.toString();
    }

    default String databaseViewsSelectRawQuery() {
        final String statement = "SELECT DISTINCT t.TABLE_NAME FROM information_schema.TABLES t WHERE t.TABLE_SCHEMA = ? AND t.TABLE_TYPE = 'VIEW'";
        log.trace("mapped select views statement: {}", statement);
        return statement;
    }

    default String filterToGetQueriesRawQuery(Boolean filterPersisted) {
        final StringBuilder statement = new StringBuilder("SELECT id, created_by, query, query_normalized, query_hash, result_hash, result_number, is_persisted, executed FROM dbrepo.queries");
        if (filterPersisted != null) {
            statement.append(" WHERE is_persisted = ?");
        }
        statement.append(";");
        log.trace("mapped get queries: {}", statement);
        return statement.toString();
    }

    /**
     * Maps the desired data type to a MySQL string with the default MySQL 8 values for each
     *
     * @param data The column definition.
     * @return The MySQL string.
     */
    default String columnTypeDtoToDataType(CreateTableColumnDto data) {
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
            case BIGINT -> "BIGINT";
            case FLOAT -> "FLOAT(" + Objects.requireNonNullElse(data.getSize(), "24") + ")";
            case DOUBLE ->
                    "DOUBLE(" + Objects.requireNonNullElse(data.getSize(), "25") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            case DECIMAL ->
                    "DECIMAL(" + Objects.requireNonNullElse(data.getSize(), "10") + "," + Objects.requireNonNullElse(data.getD(), "0") + ")";
            default -> data.getType().getType().toUpperCase();
        };
    }

    default String columnCreateDtoToPrimaryKeyLengthSpecification(CreateTableColumnDto data) {
        if (EnumSet.of(ColumnTypeDto.BLOB, ColumnTypeDto.TEXT).contains(data.getType())) {
            return "(" + Objects.requireNonNullElse(data.getIndexLength(), 255) + ")";
        }
        return "";
    }

    default String tableColumnStatisticsSelectRawQuery(String table, List<ColumnDto> data) {
        final StringBuilder statement = new StringBuilder();
        final int[] idx = new int[]{0};
        data.stream()
                .filter(column -> MariaDbUtil.numericDataTypes.contains(column.getColumnType()))
                .forEach(column -> statement.append(idx[0]++ > 0 ? " UNION " : "")
                        .append("SELECT '")
                        .append(column.getInternalName())
                        .append("' as name, MIN(")
                        .append(column.getInternalName())
                        .append(") as min, MAX(")
                        .append(column.getInternalName())
                        .append(") as max, AVG(")
                        .append(column.getInternalName())
                        .append(") as median, AVG(")
                        .append(column.getInternalName())
                        .append(") as mean, STDDEV(")
                        .append(column.getInternalName())
                        .append(") as std_dev FROM ")
                        .append(table));
        data.stream()
                .filter(column -> MariaDbUtil.stringDataTypes.contains(column.getColumnType()))
                .forEach(column -> statement.append(idx[0]++ > 0 ? " UNION " : "")
                        .append("SELECT '")
                        .append(column.getInternalName())
                        .append("' as name, MIN(LENGTH(")
                        .append(column.getInternalName())
                        .append(")) as min, MAX(LENGTH(")
                        .append(column.getInternalName())
                        .append(")) as max, AVG(LENGTH(")
                        .append(column.getInternalName())
                        .append(")) as median, AVG(LENGTH(")
                        .append(column.getInternalName())
                        .append(")) as mean, STDDEV(LENGTH(")
                        .append(column.getInternalName())
                        .append(")) as std_dev FROM ")
                        .append(table));
        if (statement.isEmpty()) {
            return null;
        }
        statement.append(";");
        log.trace("mapped select column statistic statement: {}", statement);
        return statement.toString();
    }

    /**
     * Updates a table comment as raw query statement. Currently, preparing statements for database name and table name are not supported by the driver.
     *
     * @param tableName The table name.
     * @return The raw query statement.
     */
    default String tableNameToUpdateTableRawQuery(String tableName) {
        final StringBuilder stringBuilder = new StringBuilder("ALTER TABLE ")
                .append(tableName)
                .append(" COMMENT = ?;");
        log.trace("mapped update table statement: {}", stringBuilder);
        return stringBuilder.toString();
    }

    default String tableCreateDtoToCreateTableRawQuery(CreateTableDto data) {
        final String tableName = nameToInternalName(data.getName());
        final StringBuilder stringBuilder = new StringBuilder("DO $$ BEGIN CREATE TABLE ")
                .append(tableName)
                .append(" (");
        log.trace("primary key column(s): {}", data.getConstraints().getPrimaryKey());
        final int[] idx = {0};
        for (CreateTableColumnDto column : data.getColumns()) {
            stringBuilder.append(idx[0]++ > 0 ? ", " : "")
                    .append(nameToInternalName(column.getName()))
                    .append(" ")
                    /* data type */
                    .append(columnTypeDtoToDataType(column))
                    /* null expressions */
                    .append(column.getNullAllowed() != null && column.getNullAllowed() ? " NULL" : " NOT NULL");
        }
        /* create PRIMARY KEY index */
        if (data.getConstraints() != null) {
            log.trace("constraints are primary key index={}, unique index={}, foreign key index={}, check={}",
                    data.getConstraints().getPrimaryKey(), data.getConstraints().getUniques(),
                    data.getConstraints().getForeignKeys(), data.getConstraints().getChecks());
            if (data.getConstraints().getPrimaryKey() != null && !data.getConstraints().getPrimaryKey().isEmpty()) {
                /* create PRIMARY KEY index */
                stringBuilder.append(", PRIMARY KEY (")
                        .append(String.join(",", data.getConstraints()
                                .getPrimaryKey()
                                .stream()
                                .map(c -> {
                                    final Optional<CreateTableColumnDto> optional = data.getColumns()
                                            .stream()
                                            .filter(cc -> cc.getName().equals(c))
                                            .findFirst();
                                    log.trace("lookup {} in columns: {}", c, data.getColumns().stream().map(CreateTableColumnDto::getName).toList());
                                    return nameToInternalName(c) + columnCreateDtoToPrimaryKeyLengthSpecification(optional.get());
                                })
                                .toArray(String[]::new)))
                        .append(")");
            }
            if (data.getConstraints().getUniques() != null) {
                /* create unique indices */
                data.getConstraints().getUniques()
                        .forEach(u -> stringBuilder.append(", ")
                                .append("UNIQUE KEY (")
                                .append(u.stream().map(this::nameToInternalName).collect(Collectors.joining(",")))
                                .append(")"));
            }
            if (data.getConstraints().getForeignKeys() != null) {
                /* create foreign key indices */
                data.getConstraints().getForeignKeys()
                        .forEach(fk -> {
                            stringBuilder.append(", FOREIGN KEY (")
                                    .append(fk.getColumns().stream().map(this::nameToInternalName).collect(Collectors.joining(",")))
                                    .append(") REFERENCES ")
                                    .append(nameToInternalName(fk.getReferencedTable()))
                                    .append(" (")
                                    .append(fk.getReferencedColumns().stream().map(this::nameToInternalName).collect(Collectors.joining(",")))
                                    .append(")");
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
        }
        stringBuilder.append("); ");
        if (data.getDescription() != null && !data.getDescription().isBlank()) {
            /* create table comments */
            stringBuilder.append("COMMENT ON TABLE ")
                    .append(tableName)
                    .append(" IS '")
                    .append(data.getDescription())
                    .append("'; ");
        }
        for (CreateTableColumnDto column : data.getColumns()) {
            if (column.getDescription() != null && !column.getDescription().isEmpty()) {
                /* comments */
                stringBuilder.append("COMMENT ON COLUMN ")
                        .append(tableName)
                        .append(".")
                        .append(nameToInternalName(column.getName()))
                        .append(" IS '")
                        .append(column.getDescription())
                        .append("'; ");
            }
        }
        stringBuilder.append("PERFORM periods.add_system_time_period('")
                .append(tableName)
                .append("', 'row_start', 'row_end'); PERFORM periods.add_system_versioning('")
                .append(tableName)
                .append("'); END $$;");
        log.trace("mapped create table statement: {}", stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * Selects the row count from a table/view.
     *
     * @param tableOrView The table/view internal name.
     * @param timestamp   The moment in time the data should be returned in UTC timezone.
     * @return The raw SQL query.
     */
    default String selectCountRawQuery(String tableOrView, Instant timestamp) {
        final StringBuilder statement = new StringBuilder("SELECT COUNT(1) FROM ")
                .append(tableOrView);
        if (timestamp != null) {
            statement.append("__as_of('")
                    .append(sqlDateFormatter.format(timestamp))
                    .append("')");
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
     * @param table The table internal name.
     * @return The raw SQL query.
     */
    default String selectHistoryRawQuery(String table, Long size) {
        final StringBuilder statement = new StringBuilder("SELECT IF(deleted_at IS NULL, inserted_at, deleted_at) as timestamp, IF(deleted_at IS NULL, 'INSERT', 'DELETE') as event, total FROM (SELECT ROW_START AS inserted_at, IF(ROW_END > NOW(), NULL, ROW_END) AS deleted_at, COUNT(1) as total FROM ")
                .append(table)
                .append(" FOR SYSTEM_TIME ALL GROUP BY inserted_at, deleted_at ORDER BY deleted_at DESC) AS v ORDER BY v.inserted_at, v.deleted_at ASC LIMIT ")
                .append(size)
                .append(";");
        log.trace("mapped history query: {}", statement);
        return statement.toString();
    }

    /**
     * Map the table delete query as raw query. Currently, preparing statements for database name and table name are not supported by the driver.
     *
     * @param tableName The table name.
     * @return The raw query statement.
     */
    default String dropTableRawQuery(String tableName) {
        final StringBuilder statement = new StringBuilder("DROP TABLE ");
        statement.append(tableName)
                .append(" CASCADE;");
        log.trace("mapped drop table query: {}", statement);
        return statement.toString();
    }

    default String dropTableVersioningRawQuery(String tableName) {
        final StringBuilder statement = new StringBuilder("SELECT periods.drop_system_versioning('");
        statement.append(tableName)
                .append("');");
        log.trace("mapped drop table versioning query: {}", statement);
        return statement.toString();
    }

    default String tableImportFromS3ToRawQuery(S3Config s3Config, ImportDto data, String tableName, String columnNames) {
        final StringBuilder statement = new StringBuilder("SET aws_s3.endpoint_url TO '")
                .append(s3Config.getS3Endpoint())
                .append("'; SET aws_s3.access_key_id TO '")
                .append(s3Config.getS3AccessKey())
                .append("'; SET aws_s3.secret_access_key TO '")
                .append(s3Config.getS3SecretKey())
                .append("';")
                .append("SELECT aws_s3.table_import_from_s3('")
                .append(tableName)
                .append("','")
                .append(columnNames)
                .append("','(FORMAT CSV, DELIMITER ''")
                .append(data.getSeparator())
                .append("'', HEADER ")
                .append(data.getHeader())
                .append(")','")
                .append(s3Config.getS3Bucket())
                .append("','")
                .append(data.getLocation())
                .append("','")
                .append(s3Config.getS3Region())
                .append("');");
        log.trace("mapped table import from s3 statement: {}", statement);
        return statement.toString();
    }

    default String tupleToRawDeleteQuery(Table table, TupleDeleteDto data) throws TableMalformedException {
        log.trace("table csv to delete query, table.id={}, data.keys={}", table.getId(), data.getKeys());
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("DELETE FROM ")
                .append(table.getInternalName())
                .append(" WHERE ");
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> statement.append(idx[0]++ == 0 ? "" : " AND ")
                        .append(key)
                        .append(" ")
                        .append(data.getKeys().get(key) == null ? "IS" : "=")
                        .append(" ?"));
        log.trace("mapped delete tuple query {}", statement);
        return statement.toString();
    }

    default String tupleToRawUpdateQuery(Table table, TupleUpdateDto data)
            throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("UPDATE ")
                .append(table.getInternalName())
                .append(" SET ");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    statement.append(idx[0]++ == 0 ? "" : ", ")
                            .append(key)
                            .append(" = ?");
                });
        statement.append(" WHERE ");
        final int[] jdx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> {
                    statement.append(jdx[0] == 0 ? "" : " AND ")
                            .append(key);
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

    default String tupleToRawCreateQuery(Table table, TupleDto data) throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("INSERT INTO ")
                .append(table.getInternalName())
                .append(" (");
        final int[] idx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    final Optional<Column> optional = table.getColumns().stream()
                            .filter(c -> c.getInternalName().equals(key))
                            .findFirst();
                    if (optional.isEmpty()) {
                        log.error("Failed to find table column {}", key);
                        throw new IllegalArgumentException("Failed to find table column");
                    }
                    statement.append(idx[0]++ == 0 ? "" : ", ")
                            .append(key);
                });
        statement.append(") VALUES (");
        final int[] jdx = new int[]{0};
        data.getData()
                .forEach((key, value) -> {
                    final Optional<Column> optional = table.getColumns().stream()
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

    default void prepareStatementWithColumnTypeObject(StorageService storageService, PreparedStatement statement,
                                                      ColumnType columnType, int idx, String columnName, Object value)
            throws SQLException, StorageUnavailableException, StorageNotFoundException {
        switch (columnType) {
            case BLOB, TINYBLOB, MEDIUMBLOB, LONGBLOB:
                if (value == null) {
                    statement.setNull(idx, Types.BLOB);
                    break;
                }
                final byte[] data = storageService.getBytes(String.valueOf(value));
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

    default String defaultRawTableSelectQuery(Set<String> columns, String tableName, Instant timestamp, Long page, Long size) {
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT ")
                .append(Strings.join(columns, ','))
                .append(" FROM (SELECT * FROM ")
                .append(tableName);
        if (timestamp != null) {
            statement.append("__as_of('")
                    .append(sqlDateFormatter.format(timestamp))
                    .append("')");
        }
        /* pagination */
        if (size != null && page != null) {
            log.trace("pagination size/limit of {}", size);
            statement.append(" LIMIT ")
                    .append(size);
            log.trace("pagination page/offset of {}", page);
            statement.append(" OFFSET ")
                    .append(page * size);
        }
        statement.append(") as tbl");
        log.trace("mapped select query: {}", statement);
        return statement.toString();
    }

    default String defaultRawSubsetSelectQuery(Set<String> columns, String query, Long page, Long size) {
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT ")
                .append(Strings.join(columns, ','))
                .append(" FROM (")
                .append(query)
                .append(") as tbl");
        /* pagination */
        if (size != null && page != null) {
            log.trace("pagination size/limit of {}", size);
            statement.append(" LIMIT ")
                    .append(size);
            log.trace("pagination page/offset of {}", page);
            statement.append(" OFFSET ")
                    .append(page * size);
        }
        log.trace("mapped select query: {}", statement);
        return statement.toString();
    }

    default String countRawSelectQuery(String query) {
        query = query.toLowerCase(Locale.ROOT)
                .trim();
        if (query.matches(";$")) {
            /* remove last semicolon */
            query = query.substring(0, query.length() - 1);
        }
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT COUNT(1) FROM (")
                .append(query)
                .append(") as tbl");
        log.trace("mapped count query: {}", statement);
        return statement.toString();
    }

    default Map<UUID, at.ac.tuwien.ifs.dbrepo.api.Column> databaseToColumnsKV(Database database) {
        final Map<UUID, at.ac.tuwien.ifs.dbrepo.api.Column> columns = new HashMap<>();
        if (database.getTables() != null) {
            database.getTables()
                    .forEach(table -> {
                        table.getColumns()
                                .forEach(column -> {
                                    columns.put(column.getId(), at.ac.tuwien.ifs.dbrepo.api.Column.builder()
                                            .internalName(column.getInternalName())
                                            .datasourceName(table.getInternalName())
                                            .build());
                                });
                    });
        }
        if (database.getViews() != null) {
            database.getViews()
                    .forEach(view -> {
                        view.getColumns()
                                .forEach(column -> {
                                    columns.put(column.getId(), at.ac.tuwien.ifs.dbrepo.api.Column.builder()
                                            .internalName(column.getInternalName())
                                            .datasourceName(view.getInternalName())
                                            .build());
                                });
                    });
        }
        return columns;
    }

    default Map<UUID, String> databaseToDatasourceKV(Database database, Instant timestamp) {
        final Map<UUID, String> dataSources = new HashMap<>();
        if (database.getTables() != null) {
            database.getTables()
                    .forEach(table -> {
                        final String optionalSuffix = timestamp == null ? "" : "__as_of('" + sqlDateFormatter.format(timestamp) + "')";
                        dataSources.put(table.getId(), table.getInternalName() + optionalSuffix);
                    });
        }
        if (database.getViews() != null) {
            database.getViews()
                    .forEach(view -> {
                        dataSources.put(view.getId(), view.getInternalName());
                    });
        }
        return dataSources;
    }

    default at.ac.tuwien.ifs.dbrepo.api.Column columnIdToColumn(Database database, UUID columnId)
            throws ColumnNotFoundException {
        final Map<UUID, at.ac.tuwien.ifs.dbrepo.api.Column> columns = databaseToColumnsKV(database);
        if (!columns.containsKey(columnId)) {
            log.error("Failed to find column with id: {}", columnId);
            throw new ColumnNotFoundException("Failed to find column");
        }
        return columns.get(columnId);
    }

    default SelectConditionStep<Record> subsetDtoToSelectConditions(SelectJoinStep<Record> step, Database database,
                                                                    SubsetDto data) throws ColumnNotFoundException,
            ImageNotFoundException {
        if (data.getFilters() == null || data.getFilters().isEmpty()) {
            return step.where();
        }
        SelectConditionStep<Record> conditions = step.where();
        FilterTypeDto next = null;
        final int[] idx = new int[]{0};
        for (FilterDto filter : data.getFilters()) {
            final at.ac.tuwien.ifs.dbrepo.api.Column column = columnIdToColumn(database, filter.getColumnId());
            if (idx[0]++ == 0) {
                conditions = step.where(filterDtoToCondition(database, column, filter));
            } else if (next != null) {
                if (next.equals(FilterTypeDto.OR)) {
                    conditions = conditions.or(filterDtoToCondition(database, column, filter));
                } else if (next.equals(FilterTypeDto.AND)) {
                    conditions = conditions.and(filterDtoToCondition(database, column, filter));
                }
            }
            next = filter.getType();
        }
        return conditions;
    }

    default Condition filterDtoToCondition(Database database, at.ac.tuwien.ifs.dbrepo.api.Column column, FilterDto data)
            throws ImageNotFoundException {
        final String operator = operatorIdToOperatorDto(database, data.getOperatorId()).getValue();
        switch (operator) {
            case "=":
            case "<=>":
                return field(name(column.getDatasourceName(), column.getInternalName())).eq(data.getValue());
            case "<":
                return field(name(column.getDatasourceName(), column.getInternalName())).lt(data.getValue());
            case "<=":
                return field(name(column.getDatasourceName(), column.getInternalName())).le(data.getValue());
            case ">":
                return field(name(column.getDatasourceName(), column.getInternalName())).gt(data.getValue());
            case ">=":
                return field(name(column.getDatasourceName(), column.getInternalName())).ge(data.getValue());
            case "!=":
                return field(name(column.getDatasourceName(), column.getInternalName())).ne(data.getValue());
            case "LIKE":
                return field(name(column.getDatasourceName(), column.getInternalName())).like(data.getValue());
            case "NOT LIKE":
                return field(name(column.getDatasourceName(), column.getInternalName())).notLike(data.getValue());
            case "IN":
                return field(name(column.getDatasourceName(), column.getInternalName())).in(data.getValue());
            case "NOT IN":
                return field(name(column.getDatasourceName(), column.getInternalName())).notIn(data.getValue());
            case "IS NOT NULL":
                return field(name(column.getDatasourceName(), column.getInternalName())).isNotNull();
            case "IS NULL":
                return field(name(column.getDatasourceName(), column.getInternalName())).isNull();
            case "REGEXP":
                return field(name(column.getDatasourceName(), column.getInternalName())).likeRegex(data.getValue());
            case "NOT REGEXP":
                return field(name(column.getDatasourceName(), column.getInternalName())).notLikeRegex(data.getValue());
        }
        log.error("Failed to map operator: {}", operator);
        throw new IllegalArgumentException("Failed to map operator: " + operator);
    }

    default SelectSeekStepN<Record> subsetToSelectOrder(SelectConditionStep<Record> step, Database database,
                                                        SubsetDto data) throws ColumnNotFoundException {
        final List<OrderField<Object>> sort = new LinkedList<>();
        for (OrderDto order : data.getOrders()) {
            final at.ac.tuwien.ifs.dbrepo.api.Column column = columnIdToColumn(database, order.getColumnId());
            if (order.getDirection() == null) {
                sort.add(field(name(column.getDatasourceName(), column.getInternalName())));
                continue;
            }
            switch (order.getDirection()) {
                case ASC ->
                        sort.add(field(name(column.getDatasourceName(), column.getInternalName())).asc());
                case DESC ->
                        sort.add(field(name(column.getDatasourceName(), column.getInternalName())).desc());
            }
        }
        return step.orderBy(sort);
    }

    default String subsetDtoToNormalizedQuery(DSLContext context, Database database, SubsetDto data)
            throws ColumnNotFoundException, ImageNotFoundException {
        return subsetDtoToNormalizedTimestampedQuery(context, database, data, null);
    }

    /**
     * Maps a subset to a normalized query, optionally to a timestamp.
     *
     * @param context   The database flavor context.
     * @param database  The database schema.
     * @param data      The subset.
     * @param timestamp The time at which the subset is executed, optional.
     * @return The normalized query.
     * @throws ColumnNotFoundException Some referenced column was not found in the database schema.
     * @throws ImageNotFoundException  Some operation was not found in the database schema.
     */
    default String subsetDtoToNormalizedTimestampedQuery(DSLContext context, Database database, SubsetDto data,
                                                         Instant timestamp) throws ColumnNotFoundException,
            ImageNotFoundException {
        final Map<UUID, at.ac.tuwien.ifs.dbrepo.api.Column> columns = databaseToColumnsKV(database);
        final List<Field<Object>> filteredColumns = columns.entrySet()
                .stream()
                .filter(entry -> data.getColumns().stream().anyMatch(column -> column.getId().equals(entry.getKey())))
                .map(entry -> {
                    Field<Object> field = field(name(entry.getValue().getInternalName()));
                    final Optional<SubsetColumnDto> optional = data.getColumns().stream().filter(column -> entry.getKey().equals(column.getId())).findFirst();
                    if (optional.isPresent() && optional.get().getAlias() != null) {
                        log.trace("column {}.{}.{} is aliased: {}", database.getInternalName(), entry.getValue().getDatasourceName(), entry.getValue().getInternalName(), optional.get().getAlias());
                        field = field.as(name(optional.get().getAlias()));
                    }
                    return field;
                })
                .toList();
        final List<Map.Entry<UUID, String>> tables = databaseToDatasourceKV(database, timestamp)
                .entrySet()
                .stream()
                .filter(entry -> data.getDatasourceIds().contains(entry.getKey()))
                .toList();
        log.debug("subset selects from table(s): {}", tables.stream().map(Map.Entry::getValue).toList());
        final int[] idx = new int[]{1};
        SelectJoinStep<Record> query = context.select(filteredColumns)
                .from(tables.stream()
                        .map(entry -> table(entry.getValue()))
                        .toList());
        final Map<UUID, String> datasources = databaseToDatasourceKV(database, timestamp);
        if (data.getJoins() != null) {
            log.debug("subset joins: {}", data.getJoins().stream().map(j -> datasources.get(j.getDatasourceId())).toList());
            for (JoinDto join : data.getJoins()) {
                for (ConditionalDto conditional : join.getConditionals()) {
                    query = query.join(table(datasources.get(join.getDatasourceId())), joinTypeDtoToJoinType(join.getType()))
                            .on(field(name(columns.get(conditional.getColumnId()).getDatasourceName(), columns.get(conditional.getColumnId()).getInternalName())).eq(
                                    field(name(columns.get(conditional.getForeignColumnId()).getDatasourceName(), columns.get(conditional.getForeignColumnId()).getInternalName()))));
                }
            }
        }
        final SelectConditionStep<Record> where = subsetDtoToSelectConditions(query, database, data);
        final String sql;
        if (data.getOrders() == null) {
            sql = where.getSQL(ParamType.INLINED);
        } else {
            sql = subsetToSelectOrder(where, database, data)
                    .getSQL(ParamType.INLINED);
        }
        log.trace("mapped prepared query: {}", sql);
        return sql;
    }

    default JoinType joinTypeDtoToJoinType(JoinTypeDto data) {
        if (data == null) {
            return JoinType.JOIN;
        }
        return switch (data) {
            case INNER -> JoinType.JOIN;
            case LEFT -> JoinType.LEFT_OUTER_JOIN;
            case RIGHT -> JoinType.RIGHT_OUTER_JOIN;
            case CROSS -> JoinType.CROSS_JOIN;
        };
    }

    default Operator operatorIdToOperatorDto(Database database, UUID operatorId) throws ImageNotFoundException {
        final Optional<Operator> optional = database.getContainer()
                .getImage()
                .getOperators()
                .stream()
                .filter(op -> op.getId().equals(operatorId))
                .findFirst();
        if (optional.isEmpty()) {
            log.error("Failed to find operator: {}", operatorId);
            throw new ImageNotFoundException("Failed to find operator: " + operatorId);
        }
        return optional.get();
    }

}
