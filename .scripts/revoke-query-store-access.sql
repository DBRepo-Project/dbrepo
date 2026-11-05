-- Run as the configured Data Service database account, selecting one research database.
DELIMITER //
DROP PROCEDURE IF EXISTS _migrate_query_store_access//
CREATE PROCEDURE _migrate_query_store_access()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE account_user VARCHAR(128);
    DECLARE account_host VARCHAR(255);
    DECLARE target_routine VARCHAR(64);
    DECLARE grants_cursor CURSOR FOR
        SELECT grants.User, grants.Host, grants.Routine_name FROM (
            SELECT User, Host, NULL AS Routine_name FROM mysql.db
                WHERE Db = DATABASE() AND Execute_priv = 'Y'
            UNION ALL
            SELECT User, Host, Routine_name FROM mysql.procs_priv
                WHERE Db = DATABASE() AND Routine_type = 'PROCEDURE'
                  AND Routine_name IN ('store_query', '_store_query', 'hash_query', 'hash_table')
                  AND FIND_IN_SET('Execute', Proc_priv) > 0
        ) AS grants
        WHERE CONCAT(grants.User, '@', grants.Host) <> CURRENT_USER()
          AND CONCAT(grants.User, '@', grants.Host) NOT IN (
              SELECT DEFINER FROM information_schema.ROUTINES
              WHERE ROUTINE_SCHEMA = DATABASE()
                AND ROUTINE_NAME IN ('store_query', '_store_query', 'hash_query', 'hash_table'));
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qs_queries') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select a DBRepo research database containing qs_queries';
    END IF;
    OPEN grants_cursor;
    revoke_loop: LOOP
        FETCH grants_cursor INTO account_user, account_host, target_routine;
        IF finished THEN LEAVE revoke_loop; END IF;
        SET @query_store_revoke = CONCAT('REVOKE EXECUTE ON ',
            IF(target_routine IS NULL, '', 'PROCEDURE '),
            '`', REPLACE(DATABASE(), '`', '``'), '`.',
            IF(target_routine IS NULL, '*', CONCAT('`', REPLACE(target_routine, '`', '``'), '`')),
            ' FROM `', REPLACE(account_user, '`', '``'), '`@`', REPLACE(account_host, '`', '``'), '`');
        PREPARE revoke_statement FROM @query_store_revoke;
        EXECUTE revoke_statement;
        DEALLOCATE PREPARE revoke_statement;
    END LOOP;
    CLOSE grants_cursor;
END//
CALL _migrate_query_store_access()//
DROP PROCEDURE _migrate_query_store_access//
DELIMITER ;
