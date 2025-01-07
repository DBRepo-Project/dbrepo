ALTER TABLE `mdb_databases`
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_databases`
    ADD COLUMN `is_schema_public` BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE `mdb_databases`
    DROP FOREIGN KEY `mdb_databases_ibfk_2`;
ALTER TABLE `mdb_databases`
    DROP COLUMN `created_by`;
UPDATE `mdb_databases`
SET `is_schema_public` = `is_public`;
ALTER TABLE `mdb_databases`
    ADD SYSTEM VERSIONING;

ALTER TABLE `mdb_tables`
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_tables`
    ADD COLUMN `is_schema_public` BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE `mdb_tables`
    DROP FOREIGN KEY `mdb_tables_ibfk_2`;
ALTER TABLE `mdb_tables`
    DROP COLUMN `created_by`;
UPDATE `mdb_tables`
SET `is_schema_public` = `is_public`;
ALTER TABLE `mdb_tables`
    ADD SYSTEM VERSIONING;

ALTER TABLE `mdb_view`
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_view`
    ADD COLUMN `is_schema_public` BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE `mdb_view`
    DROP FOREIGN KEY `mdb_view_ibfk_2`;
ALTER TABLE `mdb_view`
    RENAME COLUMN `created_by` TO `owned_by`;
ALTER TABLE `mdb_view`
    MODIFY `Public` BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE `mdb_view`
SET `is_schema_public` = `Public`;
ALTER TABLE `mdb_view`
    ADD SYSTEM VERSIONING;

ALTER TABLE `mdb_identifiers`
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_identifiers`
    RENAME COLUMN `created_by` TO `owned_by`;
ALTER TABLE `mdb_identifiers`
    ADD SYSTEM VERSIONING;