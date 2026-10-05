package at.ac.tuwien.ifs.dbrepo.mapper;

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
import org.jooq.*;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
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
public interface MariaDbMapper {

    Logger log = LoggerFactory.getLogger(MariaDbMapper.class);

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

    default String databaseFindAccessQuery() {
        final StringBuilder statement = new StringBuilder("SHOW GRANTS FOR ?@`%`;");
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
        final Pattern databasePattern = Pattern.compile("ON `?([a-zA-Z0-9*_]+)`?");
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

    default String databaseCreateUserQuery() {
        final String statement = "CREATE USER IF NOT EXISTS ?@`%` IDENTIFIED BY PASSWORD ?;";
        log.trace("mapped create user query: {}", statement);
        return statement;
    }

    default String databaseCreateUserRawQuery() {
        final String statement = "CREATE USER IF NOT EXISTS ?@`%` IDENTIFIED BY ?;";
        log.trace("mapped create user raw query: {}", statement);
        return statement;
    }

    default String databaseGrantPrivilegesQuery(String database, String username, String grants) {
        final StringBuilder statement = new StringBuilder("GRANT ")
                .append(grants)
                .append(" ON `")
                .append(database)
                .append("`.* TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped grant privileges statement: {}", statement);
        return statement.toString();
    }

    default String databaseRevokePrivilegesQuery(String database, String username) {
        final StringBuilder statement = new StringBuilder("REVOKE ALL PRIVILEGES ON `")
                .append(database)
                .append("`.* FROM `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped revoke privileges statement: {}", statement);
        return statement.toString();
    }

    default String databaseRevokeProcedureQuery(String database, String username, String procedure) {
        return "REVOKE EXECUTE ON PROCEDURE `" + database.replace("`", "``") + "`.`"
                + procedure.replace("`", "``") + "` FROM `" + username.replace("`", "``") + "`@`%`;";
    }

    default String databaseGrantProcedureQuery(String username, String procedure) {
        final StringBuilder statement = new StringBuilder("GRANT EXECUTE ON PROCEDURE `")
                .append(procedure)
                .append("` TO `")
                .append(username)
                .append("`@`%`;");
        log.trace("mapped grant privileges statement: {}", statement);
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

    default String queryStoreCreateTableRawQuery() {
        return """
                CREATE TABLE qs_queries (
                    id VARCHAR(36) NOT NULL PRIMARY KEY DEFAULT UUID(),
                    created DATETIME(6) NOT NULL DEFAULT NOW(6),
                    executed DATETIME(6) NOT NULL DEFAULT NOW(6),
                    created_by VARCHAR(255), query TEXT NOT NULL, query_normalized TEXT NOT NULL,
                    is_persisted BOOLEAN NOT NULL, query_hash VARCHAR(255) NOT NULL,
                    result_hash VARCHAR(255), result_number BIGINT,
                    creation_location VARCHAR(512), replication_revision BIGINT NOT NULL DEFAULT 0,
                    snapshot_hash CHAR(64),
                    execution_context LONGTEXT,
                    INDEX query_fixity (query_hash, result_hash)
                ) ENGINE=InnoDB WITH SYSTEM VERSIONING
                """;
    }

    default String queryStoreCreateSubsetOutboxRawQuery() {
        return """
                CREATE TABLE IF NOT EXISTS qs_subset_outbox (
                    query_id VARCHAR(36) NOT NULL,
                    target_site VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                    revision BIGINT NOT NULL,
                    attempts BIGINT NOT NULL DEFAULT 0,
                    next_attempt DATETIME(6) NOT NULL DEFAULT UTC_TIMESTAMP(6),
                    last_error TEXT,
                    PRIMARY KEY (query_id, target_site),
                    INDEX subset_due (next_attempt)
                ) ENGINE=InnoDB
                """;
    }

    default String queryStoreEnqueueSubsetRawQuery() {
        return """
                INSERT INTO qs_subset_outbox (query_id, target_site, revision)
                SELECT q.id, targets.site, q.replication_revision FROM qs_queries q
                JOIN JSON_TABLE(COALESCE(@dbrepo_subset_targets, '[]'), '$[*]'
                    COLUMNS (site VARCHAR(512) PATH '$')) targets
                WHERE q.id = ? AND q.creation_location IS NOT NULL AND q.replication_revision > 0
                ON DUPLICATE KEY UPDATE
                    next_attempt = IF(VALUES(revision) > revision, UTC_TIMESTAMP(6), next_attempt),
                    revision = GREATEST(revision, VALUES(revision))
                """;
    }

    default String queryStoreCreateResultsRawQuery() {
        return """
                CREATE TABLE IF NOT EXISTS qs_subset_results (
                    query_id VARCHAR(36) PRIMARY KEY,
                    schema_json LONGTEXT NOT NULL,
                    order_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                    snapshot_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                    ready BOOLEAN NOT NULL DEFAULT FALSE,
                    next_row BIGINT NOT NULL DEFAULT 0,
                    next_offset BIGINT NOT NULL DEFAULT 0
                ) ENGINE=InnoDB
                """;
    }

    default String queryStoreCreateResultRowsRawQuery() {
        return """
                CREATE TABLE IF NOT EXISTS qs_subset_result_rows (
                    query_id VARCHAR(36) NOT NULL,
                    row_no BIGINT NOT NULL,
                    row_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                    byte_length BIGINT NOT NULL,
                    payload LONGTEXT CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                    PRIMARY KEY (query_id, row_no),
                    INDEX result_fixity (query_id, row_hash, row_no)
                ) ENGINE=InnoDB
                """;
    }

    default String queryStoreCreateCaptureResultProcedureRawQuery() {
        return """
                CREATE PROCEDURE _capture_subset_result(IN work_table VARCHAR(64), IN subset_id VARCHAR(36),
                    IN expected_hash VARCHAR(255), IN expected_count BIGINT)
                SQL SECURITY INVOKER
                BEGIN
                    DECLARE fields LONGTEXT;
                    DECLARE schema_json LONGTEXT;
                    DECLARE schema_encoding LONGTEXT;
                    DECLARE row_digest CHAR(64);
                    DECLARE result_digest CHAR(64);
                    DECLARE ordered_digest CHAR(64) DEFAULT SHA2('dbrepo:subset-order:v1:',256);
                    DECLARE result_count BIGINT DEFAULT 0;
                    DECLARE done BOOLEAN DEFAULT FALSE;
                    DECLARE previous_concat_limit BIGINT DEFAULT @@session.group_concat_max_len;
                    DECLARE ordered_rows CURSOR FOR SELECT row_hash FROM qs_subset_result_rows
                        WHERE query_id = subset_id ORDER BY row_no;
                    DECLARE sorted_rows CURSOR FOR SELECT row_hash FROM qs_subset_result_rows
                        WHERE query_id = subset_id ORDER BY row_hash, row_no;
                    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        SET SESSION group_concat_max_len = previous_concat_limit;
                        DROP TEMPORARY TABLE IF EXISTS _dbrepo_subset_capture;
                        RESIGNAL;
                    END;
                    SET SESSION group_concat_max_len = 16777216;
                    SELECT GROUP_CONCAT(CONCAT('IF(`', REPLACE(column_name, '`', '``'),
                               '` IS NULL,CHAR(78),CONCAT(CHAR(86),HEX(CAST(`',
                               REPLACE(column_name, '`', '``'), '` AS BINARY))))')
                               ORDER BY ordinal_position SEPARATOR ','),
                           GROUP_CONCAT(CONCAT(HEX(column_name), ':', HEX(column_type))
                               ORDER BY ordinal_position SEPARATOR ';'),
                           JSON_ARRAYAGG(JSON_OBJECT('name', column_name, 'column_type', column_type,
                               'data_type', data_type, 'charset', character_set_name) ORDER BY ordinal_position)
                    INTO fields, schema_encoding, schema_json
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = work_table;
                    SET SESSION group_concat_max_len = previous_concat_limit;
                    IF fields IS NULL OR OCTET_LENGTH(schema_json) > 1048576 THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing or oversized subset result schema';
                    END IF;
                    DROP TEMPORARY TABLE IF EXISTS _dbrepo_subset_capture;
                    CREATE TEMPORARY TABLE _dbrepo_subset_capture (
                        position BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        payload LONGTEXT CHARACTER SET ascii COLLATE ascii_bin NOT NULL
                    ) ENGINE=InnoDB;
                    EXECUTE IMMEDIATE CONCAT('INSERT INTO _dbrepo_subset_capture(payload) SELECT JSON_ARRAY(',
                        fields, ') FROM `', REPLACE(work_table,'`','``'), '`');
                    INSERT INTO qs_subset_result_rows(query_id,row_no,row_hash,byte_length,payload)
                        SELECT subset_id,position-1,SHA2(payload,256),OCTET_LENGTH(payload),payload
                        FROM _dbrepo_subset_capture ORDER BY position;
                    DROP TEMPORARY TABLE _dbrepo_subset_capture;
                    OPEN ordered_rows;
                    order_loop: LOOP
                        FETCH ordered_rows INTO row_digest;
                        IF done THEN LEAVE order_loop; END IF;
                        SET ordered_digest = SHA2(CONCAT(ordered_digest, row_digest),256);
                        SET result_count = result_count + 1;
                    END LOOP;
                    CLOSE ordered_rows;
                    SET done = FALSE;
                    SET result_digest = SHA2(CONCAT('dbrepo:rows:v2:',schema_encoding),256);
                    OPEN sorted_rows;
                    hash_loop: LOOP
                        FETCH sorted_rows INTO row_digest;
                        IF done THEN LEAVE hash_loop; END IF;
                        SET result_digest = SHA2(CONCAT(result_digest, row_digest),256);
                    END LOOP;
                    CLOSE sorted_rows;
                    IF CONCAT('v2:',result_digest) <> expected_hash OR result_count <> expected_count THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Captured result differs from original subset fixity';
                    END IF;
                    INSERT INTO qs_subset_results(query_id,schema_json,order_hash,snapshot_hash,ready,next_row)
                    VALUES(subset_id,schema_json,ordered_digest,
                        SHA2(CONCAT('dbrepo:subset-artifact:v1:',schema_json,':',ordered_digest,':',expected_hash,':',expected_count),256),
                        TRUE,result_count);
                END
                """;
    }

    default String queryStoreCreateHashTableProcedureRawQuery() {
        return """
                CREATE PROCEDURE hash_table(IN table_name VARCHAR(255), OUT result_hash VARCHAR(255), OUT result_count BIGINT)
                SQL SECURITY INVOKER
                BEGIN
                    DECLARE field_expressions LONGTEXT;
                    DECLARE schema_encoding LONGTEXT;
                    DECLARE row_digest CHAR(64);
                    DECLARE digest CHAR(64);
                    DECLARE done BOOLEAN DEFAULT FALSE;
                    DECLARE previous_concat_limit BIGINT DEFAULT @@session.group_concat_max_len;
                    DECLARE hashes CURSOR FOR SELECT row_hash FROM _dbrepo_hash_rows ORDER BY row_hash;
                    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        DROP TEMPORARY TABLE IF EXISTS _dbrepo_hash_rows;
                        SET SESSION group_concat_max_len = previous_concat_limit;
                        RESIGNAL;
                    END;
                    SET SESSION group_concat_max_len = 16777216;
                    SELECT GROUP_CONCAT(CONCAT('IF(`', REPLACE(column_name, '`', '``'),
                               '` IS NULL,CHAR(78),CONCAT(CHAR(86),HEX(CAST(`',
                               REPLACE(column_name, '`', '``'), '` AS BINARY))))')
                               ORDER BY ordinal_position SEPARATOR ','),
                           GROUP_CONCAT(CONCAT(HEX(column_name), ':', HEX(column_type))
                               ORDER BY ordinal_position SEPARATOR ';')
                    INTO field_expressions, schema_encoding
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND information_schema.columns.table_name = table_name;
                    SET SESSION group_concat_max_len = previous_concat_limit;
                    IF field_expressions IS NULL THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot hash a missing table';
                    END IF;
                    DROP TEMPORARY TABLE IF EXISTS _dbrepo_hash_rows;
                    CREATE TEMPORARY TABLE _dbrepo_hash_rows (row_hash CHAR(64) CHARACTER SET ascii NOT NULL);
                    EXECUTE IMMEDIATE CONCAT('INSERT INTO _dbrepo_hash_rows SELECT SHA2(JSON_ARRAY(',
                        field_expressions, '),256) FROM `', REPLACE(table_name, '`', '``'), '`');
                    SET result_count = 0;
                    SET digest = SHA2(CONCAT('dbrepo:rows:v2:', schema_encoding),256);
                    OPEN hashes;
                    read_hashes: LOOP
                        FETCH hashes INTO row_digest;
                        IF done THEN LEAVE read_hashes; END IF;
                        SET digest = SHA2(CONCAT(digest, row_digest),256);
                        SET result_count = result_count + 1;
                    END LOOP;
                    CLOSE hashes;
                    DROP TEMPORARY TABLE _dbrepo_hash_rows;
                    SET result_hash = CONCAT('v2:', digest);
                END
                """;
    }

    default String queryStoreCreateStoreQueryProcedureRawQuery() {
        return """
                CREATE PROCEDURE store_query(IN query TEXT, IN normalized_query TEXT,
                    IN executed DATETIME(6), OUT queryId VARCHAR(36))
                SQL SECURITY INVOKER
                BEGIN
                    CALL _store_query(SUBSTRING_INDEX(USER(), '@', 1), query, normalized_query, executed, queryId);
                END
                """;
    }

    default String queryStoreCreateInternalStoreQueryProcedureRawQuery() {
        return """
                CREATE PROCEDURE _store_query(IN username VARCHAR(255), IN original_query TEXT,
                    IN normalized_query TEXT, IN selected_at DATETIME(6), OUT queryId VARCHAR(36))
                SQL SECURITY INVOKER
                BEGIN
                    DECLARE work_table VARCHAR(64) DEFAULT CONCAT('_dbrepo_query_', REPLACE(UUID(),'-',''));
                    DECLARE query_digest CHAR(64) DEFAULT SHA2(original_query,256);
                    DECLARE result_digest VARCHAR(255);
                    DECLARE result_count BIGINT;
                    DECLARE identity_lock CHAR(64);
                    DECLARE snapshot_digest CHAR(64) DEFAULT NULL;
                    DECLARE lock_acquired BOOLEAN DEFAULT FALSE;
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        ROLLBACK;
                        EXECUTE IMMEDIATE CONCAT('DROP TABLE IF EXISTS `', work_table, '`');
                        IF lock_acquired THEN DO RELEASE_LOCK(identity_lock); END IF;
                        RESIGNAL;
                    END;
                    IF selected_at IS NULL OR normalized_query IS NULL OR normalized_query = '' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Query and selection timestamp are required';
                    END IF;
                    EXECUTE IMMEDIATE CONCAT('CREATE TABLE `', work_table, '` AS (',
                        COALESCE(@dbrepo_subset_execution_sql, normalized_query), ')');
                    CALL hash_table(work_table, result_digest, result_count);
                    SET identity_lock = SHA2(CONCAT(DATABASE(), ':', COALESCE(@dbrepo_subset_origin, ''),
                        ':', query_digest, ':', result_digest),256);
                    SET lock_acquired = GET_LOCK(identity_lock, 10);
                    IF lock_acquired IS NULL OR NOT lock_acquired THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Query identity is busy; retry';
                    END IF;
                    START TRANSACTION;
                    SET queryId = (SELECT id FROM qs_queries
                        WHERE query_hash = query_digest AND result_hash = result_digest
                            AND creation_location <=> @dbrepo_subset_origin
                        ORDER BY created, id LIMIT 1);
                    IF queryId IS NULL THEN
                        SET queryId = UUID();
                        IF @dbrepo_subset_origin IS NOT NULL THEN
                            CALL _capture_subset_result(work_table, queryId, result_digest, result_count);
                            SELECT snapshot_hash INTO snapshot_digest FROM qs_subset_results WHERE query_id = queryId;
                        END IF;
                        INSERT INTO qs_queries(id, created_by, query, query_normalized, is_persisted,
                            query_hash, result_hash, result_number, executed, creation_location, replication_revision, snapshot_hash, execution_context)
                        VALUES(queryId, username, original_query, normalized_query, FALSE,
                            query_digest, result_digest, result_count, selected_at, @dbrepo_subset_origin,
                            IF(@dbrepo_subset_origin IS NULL, 0, 1),
                            snapshot_digest, @dbrepo_subset_context);
                    END IF;
                    IF @dbrepo_subset_origin IS NOT NULL THEN
                """ + queryStoreEnqueueSubsetRawQuery().replace("?", "queryId") + ";\n" + """
                    END IF;
                    COMMIT;
                    EXECUTE IMMEDIATE CONCAT('DROP TABLE `', work_table, '`');
                    DO RELEASE_LOCK(identity_lock);
                END
                """;
    }

    default String queryStoreCreateInternalHashQueryProcedureRawQuery() {
        return """
                CREATE PROCEDURE hash_query(IN normalized_query TEXT, OUT result_digest VARCHAR(255))
                SQL SECURITY INVOKER
                BEGIN
                    DECLARE work_table VARCHAR(64) DEFAULT CONCAT('_dbrepo_query_', REPLACE(UUID(),'-',''));
                    DECLARE result_count BIGINT;
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        EXECUTE IMMEDIATE CONCAT('DROP TABLE IF EXISTS `', work_table, '`');
                        RESIGNAL;
                    END;
                    EXECUTE IMMEDIATE CONCAT('CREATE TABLE `', work_table, '` AS (', normalized_query, ')');
                    CALL hash_table(work_table, result_digest, result_count);
                    EXECUTE IMMEDIATE CONCAT('DROP TABLE `', work_table, '`');
                END
                """;
    }

    default String queryStoreStoreQueryRawQuery() {
        final String statement = "{call _store_query(?, ?, ?, ?, ?)}";
        log.trace("mapped store query statement: {}", statement);
        return statement;
    }

    default String queryStoreHashQueryRawQuery() {
        final String statement = "{call hash_query(?, ?)}";
        log.trace("mapped hash query statement: {}", statement);
        return statement;
    }

    default String queryStoreUpdateQueryRawQuery() {
        final String statement = "UPDATE `qs_queries` SET `is_persisted` = ? WHERE `id` = ?";
        log.trace("mapped update query statement: {}", statement);
        return statement;
    }

    default String queryStoreFindQueryRawQuery() {
        final String statement = "SELECT `id`, `created_by`, `query`, `query_normalized`, `query_hash`, `result_hash`, `result_number`, `is_persisted`, `executed`, `creation_location`, `replication_revision`, `snapshot_hash`, `execution_context` FROM `qs_queries` q WHERE q.`id` = ?";
        log.trace("mapped find query statement: {}", statement);
        return statement;
    }

    default String databaseTablesSelectRawQuery() {
        final String statement = "SELECT DISTINCT t.`TABLE_NAME` FROM information_schema.TABLES t WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` = 'SYSTEM VERSIONED' AND t.`TABLE_NAME` != 'qs_queries' ORDER BY t.`TABLE_NAME` ASC";
        log.trace("mapped select tables statement: {}", statement);
        return statement;
    }

    default String analyseTableRawQuery() {
        final String statement = "ANALYZE TABLE `?`.`?` PERSISTENT FOR ALL;";
        log.trace("mapped analyse table statement: {}", statement);
        return statement;
    }

    default String databaseTableSelectRawQuery() {
        final String statement = "SELECT t.`TABLE_NAME`, t.`TABLE_TYPE`, t.`TABLE_ROWS`, t.`AVG_ROW_LENGTH`, t.`DATA_LENGTH`, t.`MAX_DATA_LENGTH`, COALESCE(t.`CREATE_TIME`, NOW()) as `CREATE_TIME`, t.`UPDATE_TIME`, v.`VIEW_DEFINITION`, t.`TABLE_COMMENT` FROM information_schema.TABLES t LEFT JOIN information_schema.VIEWS v ON t.`TABLE_NAME` = v.`TABLE_NAME` WHERE t.`TABLE_SCHEMA` = ? AND t.`TABLE_TYPE` IN ('SYSTEM VERSIONED', 'VIEW') AND t.`TABLE_NAME` != 'qs_queries' AND t.`TABLE_NAME` = ?";
        log.trace("mapped select table statement: {}", statement);
        return statement;
    }

    @Named("dropView")
    default String dropViewRawQuery(String databaseName, String viewName) {
        final StringBuilder statement = new StringBuilder("DROP VIEW IF EXISTS `")
                .append(databaseName)
                .append("`.`")
                .append(viewName)
                .append("`;");
        log.trace("mapped drop view statement: {}", statement);
        return statement.toString();
    }

    default String columnsCheckConstraintSelectRawQuery() {
        final String statement = "SELECT DISTINCT c.`CHECK_CLAUSE` FROM information_schema.COLUMNS k JOIN information_schema.CHECK_CONSTRAINTS c ON k.TABLE_NAME = c.TABLE_NAME WHERE k.TABLE_SCHEMA = ? AND k.TABLE_NAME = ?";
        log.trace("mapped select column constraint statement: {}", statement);
        return statement;
    }

    default String databaseTableColumnsSelectRawQuery() {
        final String statement = "SELECT `ORDINAL_POSITION`, `COLUMN_DEFAULT`, `IS_NULLABLE`, `DATA_TYPE`, `CHARACTER_MAXIMUM_LENGTH`, `NUMERIC_PRECISION`, `NUMERIC_SCALE`, `COLUMN_TYPE`, `COLUMN_KEY`, `COLUMN_NAME`, IF(`COLUMN_COMMENT`='',NULL,`COLUMN_COMMENT`) AS `COLUMN_COMMENT` FROM `information_schema`.`COLUMNS` WHERE `TABLE_SCHEMA` = ? AND `TABLE_NAME` = ? ORDER BY `ORDINAL_POSITION` ASC;";
        log.trace("mapped select columns statement: {}", statement);
        return statement;
    }

    default String databaseTableConstraintsSelectRawQuery() {
        final String statement = "SELECT k.`ORDINAL_POSITION`, c.`CONSTRAINT_TYPE`, k.`CONSTRAINT_NAME`, k.`COLUMN_NAME`, k.`REFERENCED_TABLE_NAME`, k.`REFERENCED_COLUMN_NAME`, r.`DELETE_RULE`, r.`UPDATE_RULE` FROM information_schema.TABLE_CONSTRAINTS c JOIN information_schema.KEY_COLUMN_USAGE k ON c.`TABLE_NAME` = k.`TABLE_NAME` AND c.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` AND c.`CONSTRAINT_SCHEMA` = k.`CONSTRAINT_SCHEMA` LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS r ON r.`CONSTRAINT_NAME` = k.`CONSTRAINT_NAME` AND r.`CONSTRAINT_SCHEMA` = c.`TABLE_SCHEMA` AND r.`TABLE_NAME` = c.`TABLE_NAME` WHERE LOWER(k.`COLUMN_NAME`) != 'row_end' AND c.`TABLE_SCHEMA` = ? AND c.`TABLE_NAME` = ? ORDER BY k.`ORDINAL_POSITION` ASC;";
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
        final StringBuilder statement = new StringBuilder("SELECT `id`, `created_by`, `query`, `query_normalized`, `query_hash`, `result_hash`, `result_number`, `is_persisted`, `executed`, `creation_location`, `replication_revision`, `snapshot_hash` FROM `qs_queries`");
        if (filterPersisted != null) {
            statement.append(" WHERE `is_persisted` = ?");
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
            case BIGINT -> "BIGINT(" + Objects.requireNonNullElse(data.getSize(), "255") + ")";
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

    default String tableColumnStatisticsSelectRawQuery(String databaseName, String table, List<ColumnDto> data) {
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
                        .append("`) as std_dev FROM `")
                        .append(databaseName)
                        .append("`.`")
                        .append(table)
                        .append("`"));
        data.stream()
                .filter(column -> MariaDbUtil.stringDataTypes.contains(column.getColumnType()))
                .forEach(column -> statement.append(idx[0]++ > 0 ? " UNION " : "")
                        .append("SELECT '")
                        .append(column.getInternalName())
                        .append("' as name, MIN(LENGTH(`")
                        .append(column.getInternalName())
                        .append("`)) as min, MAX(LENGTH(`")
                        .append(column.getInternalName())
                        .append("`)) as max, AVG(LENGTH(`")
                        .append(column.getInternalName())
                        .append("`)) as median, AVG(LENGTH(`")
                        .append(column.getInternalName())
                        .append("`)) as mean, STDDEV(LENGTH(`")
                        .append(column.getInternalName())
                        .append("`)) as std_dev FROM `")
                        .append(databaseName)
                        .append("`.`")
                        .append(table)
                        .append("`"));
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
     * @param databaseName The database name.
     * @param tableName    The table name.
     * @return The raw query statement.
     */
    default String tableNameToUpdateTableRawQuery(String databaseName, String tableName) {
        final StringBuilder stringBuilder = new StringBuilder("ALTER TABLE `")
                .append(databaseName)
                .append("`.`")
                .append(tableName)
                .append("` COMMENT = ?;");
        log.trace("mapped update table statement: {}", stringBuilder);
        return stringBuilder.toString();
    }

    default String tableCreateDtoToCreateTableRawQuery(String databaseName, CreateTableDto data) {
        final StringBuilder stringBuilder = new StringBuilder("CREATE TABLE `")
                .append(databaseName)
                .append("`.`")
                .append(nameToInternalName(data.getName()))
                .append("` (");
        log.trace("PRIMARY KEY column(s) exist: {}", data.getConstraints().getPrimaryKey());
        final int[] idx = {0};
        for (CreateTableColumnDto column : data.getColumns()) {
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
        /* create PRIMARY KEY index */
        if (data.getConstraints() != null) {
            log.trace("constraints are {}", data.getConstraints());
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
    default String dropTableRawQuery(String databaseName, String tableName) {
        return dropTableRawQuery(databaseName, tableName, true);
    }

    /**
     * Map the table delete query as raw query. Currently, preparing statements for database name and table name are not supported by the driver.
     *
     * @param databaseName The database name.
     * @param tableName    The table name.
     * @param force        If true, force the deletion and throw an error if the table does not exists, otherwise ignore errors.
     * @return The raw query statement.
     */
    default String dropTableRawQuery(String databaseName, String tableName, Boolean force) {
        final StringBuilder statement = new StringBuilder("DROP TABLE ");
        if (!force) {
            statement.append("IF EXISTS ");
        }
        statement.append("`")
                .append(databaseName)
                .append("`.`")
                .append(tableName)
                .append("`;");
        log.trace("mapped drop table query: {}", statement);
        return statement.toString();
    }

    default String copyTableSchemaToRawQuery(String from, String to) {
        final StringBuilder statement = new StringBuilder("CREATE OR REPLACE TABLE `")
                .append(to)
                .append("` LIKE `")
                .append(from)
                .append("`;");
        log.trace("mapped copy table schema statement: {}", statement);
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

    default String tupleToRawDeleteQuery(String databaseName, Table table, TupleDeleteDto data) throws TableMalformedException {
        log.trace("table csv to delete query, table.id={}, data.keys={}", table.getId(), data.getKeys());
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("DELETE FROM `")
                .append(databaseName)
                .append("`.`")
                .append(table.getInternalName())
                .append("` WHERE ");
        final int[] idx = new int[]{0};
        data.getKeys()
                .forEach((key, value) -> statement.append(idx[0]++ == 0 ? "" : " AND ")
                        .append("`")
                        .append(key)
                        .append("` <=> ?"));
        log.trace("mapped delete tuple query {}", statement);
        return statement.toString();
    }

    default String tupleToRawUpdateQuery(String databaseName, Table table, TupleUpdateDto data)
            throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("UPDATE `")
                .append(databaseName)
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

    default String tupleToRawCreateQuery(String databaseName, Table table, TupleDto data) throws TableMalformedException {
        if (table.getColumns().isEmpty()) {
            throw new TableMalformedException("Columns are not known");
        }
        /* parameterized query for prepared statement */
        final StringBuilder statement = new StringBuilder("INSERT INTO `")
                .append(databaseName)
                .append("`.`")
                .append(table.getInternalName())
                .append("` (");
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
                            .append("`")
                            .append(key)
                            .append("`");
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
                statement.setBigDecimal(idx, new BigDecimal(String.valueOf(value)));
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

    default String rawSelectQuery(String query, Instant timestamp, Long page, Long size) {
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT * FROM (")
                .append(query);
        statement.append(")");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(" as tbl");
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

    default String paginateSubset(String normalizedQuery, Long page, Long size) {
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder(normalizedQuery);
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

    default String defaultRawSelectQuery(String databaseName, String tableOrViewName, Instant timestamp, Long page,
                                         Long size, List<String> sortColumns, String sortDirection) {
        /* query check (this is enforced by the db also) */
        final StringBuilder statement = new StringBuilder("SELECT * FROM (SELECT * FROM `")
                .append(databaseName)
                .append("`.`")
                .append(tableOrViewName)
                .append("`");
        if (timestamp != null) {
            statement.append(" FOR SYSTEM_TIME AS OF TIMESTAMP '")
                    .append(mariaDbFormatter.format(timestamp))
                    .append("'");
        }
        statement.append(" as tbl");
        statement.append(") as tbl2");
        if (sortColumns != null && !sortColumns.isEmpty() && sortDirection != null) {
            /* Keep sorting on the outer query so unpaginated exports preserve row order. */
            final String orderByClause = sortColumns.stream()
                    .map(columnName -> new StringBuilder("`tbl2`.`")
                            .append(columnName)
                            .append("`")
                            .append(" ")
                            .append(sortDirection.toUpperCase(Locale.ROOT))
                            .toString())
                    .collect(Collectors.joining(", "));
            statement.append(" ORDER BY ")
                    .append(orderByClause);
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

    default Map<UUID, String> databaseToDatasourceKV(Database database) {
        final Map<UUID, String> dataSources = new HashMap<>();
        if (database.getTables() != null) {
            database.getTables()
                    .forEach(table -> {
                        dataSources.put(table.getId(), table.getInternalName());
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
        FilterTypeDto connector = null;
        boolean hasCondition = false;
        for (FilterDto filter : data.getFilters()) {
            if (filter.getType().equals(FilterTypeDto.AND) || filter.getType().equals(FilterTypeDto.OR)) {
                connector = filter.getType();
                continue;
            }
            final at.ac.tuwien.ifs.dbrepo.api.Column column = columnIdToColumn(database, filter.getColumnId());
            if (!hasCondition) {
                conditions = step.where(filterDtoToCondition(database, column, filter));
                hasCondition = true;
            } else if (connector != null) {
                if (connector.equals(FilterTypeDto.OR)) {
                    conditions = conditions.or(filterDtoToCondition(database, column, filter));
                } else if (connector.equals(FilterTypeDto.AND)) {
                    conditions = conditions.and(filterDtoToCondition(database, column, filter));
                }
            }
            connector = null;
        }
        return conditions;
    }

    default Condition filterDtoToCondition(Database database, at.ac.tuwien.ifs.dbrepo.api.Column column, FilterDto data)
            throws ImageNotFoundException {
        final String operator = operatorIdToOperatorDto(database, data.getOperatorId()).getValue();
        switch (operator) {
            case "=":
            case "<=>":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).eq(data.getValue());
            case "<":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).lt(data.getValue());
            case "<=":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).le(data.getValue());
            case ">":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).gt(data.getValue());
            case ">=":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).ge(data.getValue());
            case "!=":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).ne(data.getValue());
            case "LIKE":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).like(data.getValue());
            case "NOT LIKE":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).notLike(data.getValue());
            case "IN":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).in(data.getValue());
            case "NOT IN":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).notIn(data.getValue());
            case "IS NOT NULL":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).isNotNull();
            case "IS NULL":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).isNull();
            case "REGEXP":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).likeRegex(data.getValue());
            case "NOT REGEXP":
                return field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).notLikeRegex(data.getValue());
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
                sort.add(field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())));
                continue;
            }
            switch (order.getDirection()) {
                case ASC ->
                        sort.add(field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).asc());
                case DESC ->
                        sort.add(field(name(database.getInternalName(), column.getDatasourceName(), column.getInternalName())).desc());
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
                    Field<Object> field = field(name(database.getInternalName(), entry.getValue().getDatasourceName(), entry.getValue().getInternalName()));
                    final Optional<SubsetColumnDto> optional = data.getColumns().stream().filter(column -> entry.getKey().equals(column.getId())).findFirst();
                    if (optional.isPresent() && optional.get().getAlias() != null) {
                        log.trace("column `{}`.`{}`.`{}` is aliased: {}", database.getInternalName(), entry.getValue().getDatasourceName(), entry.getValue().getInternalName(), optional.get().getAlias());
                        field = field.as(name(optional.get().getAlias()));
                    }
                    return field;
                })
                .toList();
        final List<Map.Entry<UUID, String>> tables = databaseToDatasourceKV(database)
                .entrySet()
                .stream()
                .filter(entry -> data.getDatasourceIds().contains(entry.getKey()))
                .toList();
        log.debug("subset selects from table(s): {}", tables.stream().map(Map.Entry::getValue).toList());
        final String temporal = timestamp == null ? "" : " FOR SYSTEM_TIME AS OF TIMESTAMP '"
                + mariaDbFormatter.format(timestamp) + "'";
        SelectJoinStep<Record> query = context.select(filteredColumns)
                .from(tables.stream()
                        .map(entry -> table("{0}" + temporal, name(entry.getValue())))
                        .toList());
        final Map<UUID, String> datasources = databaseToDatasourceKV(database);
        if (data.getJoins() != null) {
            log.debug("subset joins: {}", data.getJoins().stream().map(j -> datasources.get(j.getDatasourceId())).toList());
            for (JoinDto join : data.getJoins()) {
                Condition condition = null;
                for (ConditionalDto conditional : join.getConditionals()) {
                    final Condition equality = field(name(database.getInternalName(), columns.get(conditional.getColumnId()).getDatasourceName(), columns.get(conditional.getColumnId()).getInternalName())).eq(
                            field(name(database.getInternalName(), columns.get(conditional.getForeignColumnId()).getDatasourceName(), columns.get(conditional.getForeignColumnId()).getInternalName())));
                    condition = condition == null ? equality : condition.and(equality);
                }
                query = query.join(table("{0}" + temporal, name(datasources.get(join.getDatasourceId()))),
                        joinTypeDtoToJoinType(join.getType())).on(condition == null ? trueCondition() : condition);
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
