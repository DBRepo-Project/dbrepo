CREATE PROCEDURE store_query(IN query TEXT, IN normalized_query TEXT, IN executed DATETIME, OUT queryId VARCHAR(36)) BEGIN
DECLARE _queryhash VARCHAR(255) DEFAULT SHA2(query, 256); DECLARE _username VARCHAR(255) DEFAULT REGEXP_REPLACE(current_user(), '@.*', ''); DECLARE _work VARCHAR(64) DEFAULT CONCAT('_dbrepo_hash_', REPLACE(UUID(), '-', ''));
DECLARE _created BOOLEAN DEFAULT FALSE;
DECLARE _query TEXT;
DECLARE EXIT HANDLER FOR SQLEXCEPTION
BEGIN
  IF _created THEN
    SET @cleanup_sql = CONCAT('DROP TABLE IF EXISTS `', _work, '`');
    PREPARE cleanup_stmt FROM @cleanup_sql;
    EXECUTE cleanup_stmt;
    DEALLOCATE PREPARE cleanup_stmt;
  END IF;
  RESIGNAL;
END;
SET _query = CONCAT('CREATE TABLE `', _work, '` AS (', normalized_query, ')');
PREPARE stmt FROM _query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET _created = TRUE;
CALL hash_table(_work, @hash, @count);
SET _query = CONCAT('DROP TABLE `', _work, '`');
PREPARE stmt FROM _query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET _created = FALSE;  IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, normalized_query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF; END;
