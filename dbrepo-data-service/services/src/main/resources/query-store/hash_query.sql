CREATE DEFINER = 'root' PROCEDURE hash_query(IN normalized_query TEXT, OUT queryHash VARCHAR(64)) BEGIN
DECLARE _work VARCHAR(64) DEFAULT CONCAT('_dbrepo_hash_', REPLACE(UUID(), '-', ''));
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
SET _created = FALSE; 
SET queryHash = (SELECT @hash); END;
