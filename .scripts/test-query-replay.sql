SET time_zone = '+00:00';
CREATE TABLE replay_readings (id INT PRIMARY KEY, reading INT) WITH SYSTEM VERSIONING;
SET timestamp = 1700000000;
INSERT INTO replay_readings VALUES (1, 11);
SET timestamp = 1700000010;
UPDATE replay_readings SET reading = 22 WHERE id = 1;
SET timestamp = DEFAULT;

SET @original = 'SELECT reading FROM replay_readings WHERE id = 1';
SET @as_of = '2023-11-14 22:13:25';
SET @historical = CONCAT('SELECT reading FROM replay_readings FOR SYSTEM_TIME AS OF TIMESTAMP ', QUOTE(@as_of), ' WHERE id = 1');
CALL store_query(@original, @historical, @as_of, @subset_id);
SELECT query, query_normalized INTO @saved_query, @saved_replay FROM qs_queries WHERE id = @subset_id;
EXECUTE IMMEDIATE CONCAT('SELECT reading INTO @replayed FROM (', @saved_replay, ') AS saved');
DELIMITER $$
BEGIN NOT ATOMIC
    IF NOT (@saved_query <=> @original AND @saved_replay <=> @historical AND @replayed <=> 11)
       OR (SELECT reading FROM replay_readings WHERE id = 1) <> 22 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stored historical SQL did not replay the cited row';
    END IF;
END$$
DELIMITER ;
