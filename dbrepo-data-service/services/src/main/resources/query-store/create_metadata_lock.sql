CREATE TABLE `qs_metadata_lock` (`id` TINYINT NOT NULL PRIMARY KEY CHECK (`id` = 1)) ENGINE=InnoDB COMMENT='dbrepo:qs-metadata-lock:v1' AS SELECT 1 AS id;
