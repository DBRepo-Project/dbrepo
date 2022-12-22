-- CREATE SEQUENCES
DROP SEQUENCE IF EXISTS `qs_queries_seq`;
CREATE SEQUENCE `qs_queries_seq`;
-- CREATE TABLES
DROP TABLE IF EXISTS `qs_queries`;
CREATE TABLE `qs_queries`
(
    `id`               bigint       not null primary key default nextval(`qs_queries_seq`),
    `created`          datetime     not null             default now(),
    `created_by`       varchar(255) not null,
    `query`            text         not null,
    `query_normalized` text         not null,
    `is_persisted`     boolean      not null,
    `query_hash`       varchar(255) not null,
    `result_hash`      varchar(255),
    `result_number`    bigint
);
-- HASH PROCEDURE
DROP PROCEDURE IF EXISTS hash_table;
DROP PROCEDURE IF EXISTS store_query;
DELIMITER $$
CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT hash VARCHAR(255))
BEGIN
    DECLARE _sql TEXT;
    -- COMPILE QUERY
    SELECT CONCAT(
                   'SELECT SHA2(GROUP_CONCAT(CONCAT_WS(\'\',',
                   GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY column_name),
                   ') SEPARATOR \',\'), 256) AS hash FROM `', name, '` INTO @hash;')
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = name
    INTO _sql;
    -- EXECUTE QUERY
    PREPARE stmt FROM _sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    -- SET OUTPUT
    SET hash = @hash;
END $$
-- STORE QUERY PROCEDURE FOR USER
CREATE PROCEDURE store_query(IN query TEXT, OUT queryId BIGINT)
BEGIN
    DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256);
    DECLARE _username varchar(255) DEFAULT REPLACE(current_user(), '@%', '');
    DECLARE _query TEXT DEFAULT CONCAT('CREATE TABLE IF NOT EXISTS _tmp AS (', query, ')');
    -- DROP PREVIOUS TEMPORARY TABLE
    DROP TABLE IF EXISTS _tmp;
    -- CREATE TEMPORARY TABLE AND FILL IT WITH QUERY EXECUTION RESULT
    PREPARE stmt FROM _query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    -- GET TEMPORARY TABLE HASH
    CALL hash_table('_tmp', @hash);
    SELECT COUNT(*) FROM _tmp INTO @count;
    -- STORE QUERY OR RETRIEVE PREVIOUS QUERY
    INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`,
                              `result_number`)
    SELECT _username, query, query, false, _queryhash, @hash, @count
    WHERE NOT EXISTS(SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
    -- RETURN THE `queryId`
    SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash);
END $$
-- STORE QUERY PROCEDURE FOR SYSTEM
CREATE
    DEFINER = 'root' PROCEDURE _store_query(IN _username VARCHAR(255), IN query TEXT, OUT queryId BIGINT)
BEGIN
    DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256);
    DECLARE _query TEXT DEFAULT CONCAT('CREATE TABLE IF NOT EXISTS _tmp AS (', query, ')');
    -- DROP PREVIOUS TEMPORARY TABLE
    DROP TABLE IF EXISTS _tmp;
    -- CREATE TEMPORARY TABLE AND FILL IT WITH QUERY EXECUTION RESULT
    PREPARE stmt FROM _query;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
    -- GET TEMPORARY TABLE HASH
    CALL hash_table('_tmp', @hash);
    SELECT COUNT(*) FROM _tmp INTO @count;
    -- STORE QUERY OR RETRIEVE PREVIOUS QUERY
    INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`,
                              `result_number`)
    SELECT _username, query, query, false, _queryhash, @hash, @count
    WHERE NOT EXISTS(SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
    -- RETURN THE `queryId`
    SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash);
END $$