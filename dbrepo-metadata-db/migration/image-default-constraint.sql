-- Use a dedicated connection to the metadata database after backing it up and pausing image writes.
-- The generated key is NULL for ordinary images, so only default images compete for a unique value.
SET @_image_previous_history = @@SESSION.system_versioning_alter_history;
SET SESSION system_versioning_alter_history = KEEP;
ALTER TABLE mdb_images
    ADD COLUMN IF NOT EXISTS default_image TINYINT GENERATED ALWAYS AS (CASE WHEN is_default THEN 1 ELSE NULL END),
    ADD UNIQUE KEY IF NOT EXISTS only_default_image (default_image);

-- Hibernate and older schema imports may have chosen different names for the old index.
SET @_image_drop_index = (
    SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(INDEX_NAME, '`', '``'), '`') SEPARATOR ', ')
    FROM (
        SELECT INDEX_NAME
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mdb_images'
          AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'
        GROUP BY INDEX_NAME
        HAVING COUNT(*) = 1 AND MIN(COLUMN_NAME) = 'is_default'
    ) old_indexes
);
SET @_image_ddl = IF(@_image_drop_index IS NULL, 'DO 0', CONCAT('ALTER TABLE mdb_images ', @_image_drop_index));
PREPARE image_constraint FROM @_image_ddl;
EXECUTE image_constraint;
DEALLOCATE PREPARE image_constraint;
SET SESSION system_versioning_alter_history = @_image_previous_history;
