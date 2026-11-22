CREATE PROCEDURE hash_table(IN table_name VARCHAR(255), OUT result_hash VARCHAR(255), OUT result_count BIGINT) SQL SECURITY INVOKER BEGIN
    DECLARE field_expressions LONGTEXT;
    DECLARE schema_encoding LONGTEXT;
    DECLARE row_digest CHAR(64);
    DECLARE digest CHAR(64);
    DECLARE done BOOLEAN DEFAULT FALSE;
    DECLARE created_hash_rows BOOLEAN DEFAULT FALSE;
    DECLARE size_limit_hit BOOLEAN DEFAULT FALSE;
    DECLARE previous_concat_limit BIGINT DEFAULT @@session.group_concat_max_len;
    DECLARE hashes CURSOR FOR SELECT row_hash FROM _dbrepo_hash_rows ORDER BY row_hash;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    -- GROUP_CONCAT truncation and max_allowed_packet overflow must never yield a digest.
    DECLARE CONTINUE HANDLER FOR 1260, 1301 SET size_limit_hit = TRUE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        IF created_hash_rows THEN DROP TEMPORARY TABLE _dbrepo_hash_rows; END IF;
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
    IF size_limit_hit THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Query result schema exceeds the MariaDB hash limits';
    END IF;
    IF field_expressions IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot hash a missing table';
    END IF;
    CREATE TEMPORARY TABLE _dbrepo_hash_rows (row_hash CHAR(64) CHARACTER SET ascii NOT NULL);
    SET created_hash_rows = TRUE;
    EXECUTE IMMEDIATE CONCAT('INSERT INTO _dbrepo_hash_rows SELECT SHA2(JSON_ARRAY(',
        field_expressions, '),256) FROM `', REPLACE(table_name, '`', '``'), '`');
    SET result_count = 0;
    SET digest = SHA2(CONCAT('dbrepo:rows:v2:', schema_encoding),256);
    IF size_limit_hit OR digest IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Query result exceeds the MariaDB hash limits';
    END IF;
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
END;
