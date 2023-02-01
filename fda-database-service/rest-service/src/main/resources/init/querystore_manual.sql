CREATE SEQUENCE `qs_queries_seq`;
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
DELIMITER $$
CREATE PROCEDURE hash_table(IN name VARCHAR(255), OUT hash VARCHAR(255))
BEGIN
    DECLARE _sql TEXT;
    SELECT CONCAT('SELECT SHA2(GROUP_CONCAT(CONCAT_WS(\'\',',
                  GROUP_CONCAT(CONCAT('`', column_name, '`') ORDER BY column_name),
                  ') SEPARATOR \',\'), 256) AS hash FROM `', name, '` INTO @hash;')
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = name
    INTO _sql;
    PREPARE stmt FROM _sql; EXECUTE stmt; DEALLOCATE PREPARE stmt; SET hash = @hash;
END $$
DELIMITER  $$
CREATE PROCEDURE store_query(IN query TEXT, OUT queryId BIGINT)
BEGIN
    DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256);
    DECLARE _username varchar(255) DEFAULT REGEXP_REPLACE(current_user(), '@.*', '');
    DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')');
    PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash);
    SELECT COUNT(*) FROM _tmp INTO @count;
    INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`,
                              `result_number`)
    SELECT _username, query, query, true, _queryhash, @hash, @count
    WHERE NOT EXISTS(SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
    SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
END $$
DELIMITER  $$
CREATE
    DEFINER = 'root' PROCEDURE _store_query(IN _username VARCHAR(255), IN query TEXT, OUT queryId BIGINT)
BEGIN
    DECLARE _queryhash varchar(255) DEFAULT SHA2(query, 256);
    DECLARE _query TEXT DEFAULT CONCAT('CREATE OR REPLACE TABLE _tmp AS (', query, ')');
    PREPARE stmt FROM _query; EXECUTE stmt; DEALLOCATE PREPARE stmt; CALL hash_table('_tmp', @hash);
    SELECT COUNT(*) FROM _tmp INTO @count;
    INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`,
                              `result_number`)
    SELECT _username, query, query, false, _queryhash, @hash, @count
    WHERE NOT EXISTS(SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
    SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash);
END $$