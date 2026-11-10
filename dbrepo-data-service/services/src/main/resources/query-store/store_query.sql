CREATE PROCEDURE store_query(IN query TEXT, IN normalized_query TEXT, IN executed DATETIME, OUT queryId VARCHAR(36)) BEGIN
DECLARE _queryhash VARCHAR(255) DEFAULT SHA2(query, 256); DECLARE _username VARCHAR(255) DEFAULT REGEXP_REPLACE(current_user(), '@.*', ''); DECLARE _work VARCHAR(64) DEFAULT CONCAT('_dbrepo_hash_', REPLACE(UUID(), '-', ''));
DECLARE _created BOOLEAN DEFAULT FALSE;
DECLARE _query TEXT;
DECLARE _metadata_transaction BOOLEAN DEFAULT FALSE;
DECLARE _metadata_guard TINYINT;
DECLARE _previous_lock_timeout INT DEFAULT @@SESSION.innodb_lock_wait_timeout;
DECLARE EXIT HANDLER FOR SQLEXCEPTION
BEGIN
  IF _created THEN
    SET @cleanup_sql = CONCAT('DROP TABLE IF EXISTS `', _work, '`');
    PREPARE cleanup_stmt FROM @cleanup_sql;
    EXECUTE cleanup_stmt;
    DEALLOCATE PREPARE cleanup_stmt;
  END IF;
  IF _metadata_transaction THEN
    ROLLBACK;
    SET SESSION innodb_lock_wait_timeout = _previous_lock_timeout;
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
SET _created = FALSE;
SET _metadata_transaction = TRUE;
SET SESSION innodb_lock_wait_timeout = 10;
START TRANSACTION;
-- ponytail: one metadata writer per database; use indexed uniqueness if this short section becomes a bottleneck.
SELECT `id` INTO _metadata_guard FROM `qs_metadata_lock` WHERE `id` = 1 FOR UPDATE;
IF _metadata_guard IS NULL THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Query Store metadata lock row is missing';
END IF;
IF @hash IS NULL THEN INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, normalized_query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` IS NULL); ELSE INSERT INTO `qs_queries` (`created_by`, `query`, `query_normalized`, `is_persisted`, `query_hash`, `result_hash`, `result_number`, `executed`) SELECT _username, query, query, false, _queryhash, @hash, @count, executed WHERE NOT EXISTS (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); SET queryId = (SELECT `id` FROM `qs_queries` WHERE `query_hash` = _queryhash AND `result_hash` = @hash); END IF;
COMMIT;
SET SESSION innodb_lock_wait_timeout = _previous_lock_timeout;
SET _metadata_transaction = FALSE;
END;
