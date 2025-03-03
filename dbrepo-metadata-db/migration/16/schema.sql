DROP TABLE IF EXISTS `mdb_data`;
DROP TABLE IF EXISTS `mdb_columns_nom`;
DROP TABLE IF EXISTS `mdb_columns_cat`;
DROP TABLE IF EXISTS `mdb_update`;
DROP TABLE IF EXISTS `mdb_databases_subjects`;
DROP TABLE IF EXISTS `mdb_images_date`;
-- mdb_ontologies
ALTER TABLE `mdb_ontologies`
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_ontologies
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_ontologies`
    DROP PRIMARY KEY;
-- mdb_concepts
ALTER TABLE mdb_concepts
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_concepts
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_concepts`
    DROP PRIMARY KEY;
-- mdb_units
ALTER TABLE `mdb_units`
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_units
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_units`
    DROP PRIMARY KEY;
-- mdb_messages
ALTER TABLE `mdb_banner_messages`
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_banner_messages` RENAME `mdb_messages`;
ALTER TABLE mdb_messages
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_messages`
    DROP PRIMARY KEY;
-- mdb_image_operators
ALTER TABLE mdb_image_operators
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_image_operators
    DROP FOREIGN KEY mdb_image_operators_ibfk_1;
ALTER TABLE mdb_image_operators
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_image_operators`
    DROP PRIMARY KEY;
ALTER TABLE mdb_image_operators
    CHANGE COLUMN image_id image_id VARCHAR(36) NOT NULL;
-- mdb_image_types
ALTER TABLE mdb_image_types
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_image_types
    DROP FOREIGN KEY mdb_image_types_ibfk_1;
ALTER TABLE mdb_image_types
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_image_types`
    DROP PRIMARY KEY;
ALTER TABLE mdb_image_types
    CHANGE COLUMN image_id image_id VARCHAR(36) NOT NULL;
-- mdb_access
ALTER TABLE mdb_access
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_access
    DROP FOREIGN KEY mdb_access_ibfk_1;
ALTER TABLE mdb_access
    CHANGE COLUMN aDBID aDBID VARCHAR(36) NOT NULL;
ALTER TABLE `mdb_access`
    DROP PRIMARY KEY;
-- mdb_have_access
ALTER TABLE mdb_have_access
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_have_access
    DROP FOREIGN KEY mdb_have_access_ibfk_1;
ALTER TABLE mdb_have_access
    DROP FOREIGN KEY mdb_have_access_ibfk_2;
ALTER TABLE mdb_have_access
    CHANGE COLUMN user_id user_id VARCHAR(36) NOT NULL;
ALTER TABLE mdb_have_access
    CHANGE COLUMN database_id database_id VARCHAR(36) NOT NULL;
ALTER TABLE `mdb_have_access`
    DROP PRIMARY KEY;-- mdb_identifier_creators
ALTER TABLE mdb_identifier_creators
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_creators
    DROP FOREIGN KEY mdb_identifier_creators_ibfk_1;
ALTER TABLE mdb_identifier_creators
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifier_creators`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifier_creators
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
-- mdb_identifier_descriptions
ALTER TABLE mdb_identifier_descriptions
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_descriptions
    DROP FOREIGN KEY mdb_identifier_descriptions_ibfk_1;
ALTER TABLE mdb_identifier_descriptions
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifier_descriptions`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifier_descriptions
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
-- mdb_identifier_funders
ALTER TABLE mdb_identifier_funders
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_funders
    DROP FOREIGN KEY mdb_identifier_funders_ibfk_1;
ALTER TABLE mdb_identifier_funders
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifier_funders`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifier_funders
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
-- mdb_identifier_licenses
ALTER TABLE mdb_identifier_licenses
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_licenses
    DROP FOREIGN KEY mdb_identifier_licenses_ibfk_1;
ALTER TABLE mdb_identifier_licenses
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
ALTER TABLE `mdb_identifier_licenses`
    DROP PRIMARY KEY;
-- mdb_identifier_titles
ALTER TABLE mdb_identifier_titles
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifier_titles
    DROP FOREIGN KEY mdb_identifier_titles_ibfk_1;
ALTER TABLE mdb_identifier_titles
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifier_titles`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifier_titles
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
-- mdb_identifier_licenses
ALTER TABLE mdb_related_identifiers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_related_identifiers
    DROP FOREIGN KEY mdb_related_identifiers_ibfk_1;
ALTER TABLE mdb_related_identifiers RENAME mdb_identifier_related;
ALTER TABLE mdb_identifier_related
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifier_related`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifier_related
    CHANGE COLUMN pid pid VARCHAR(36) NOT NULL;
-- mdb_identifiers
ALTER TABLE mdb_identifiers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifiers
    DROP FOREIGN KEY mdb_identifiers_ibfk_1;
ALTER TABLE mdb_identifiers
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_identifiers`
    DROP PRIMARY KEY;
ALTER TABLE mdb_identifiers
    CHANGE COLUMN dbid dbid VARCHAR(36) NOT NULL;
ALTER TABLE mdb_identifiers
    CHANGE COLUMN qid qid VARCHAR(36);
ALTER TABLE mdb_identifiers
    CHANGE COLUMN tid tid VARCHAR(36);
ALTER TABLE mdb_identifiers
    CHANGE COLUMN vid vid VARCHAR(36);
-- mdb_columns_concepts
ALTER TABLE mdb_columns_concepts
    DROP SYSTEM VERSIONING;
ALTER TABLE `mdb_columns_concepts`
    DROP PRIMARY KEY;
ALTER TABLE mdb_columns_concepts
    DROP FOREIGN KEY mdb_columns_concepts_ibfk_1;
ALTER TABLE mdb_columns_concepts
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE mdb_columns_concepts
    CHANGE COLUMN cID cID VARCHAR(36) NOT NULL;
-- mdb_columns_enums
ALTER TABLE mdb_columns_enums
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_columns_enums
    DROP FOREIGN KEY mdb_columns_enums_ibfk_1;
ALTER TABLE mdb_columns_enums
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_columns_enums`
    DROP PRIMARY KEY;
ALTER TABLE mdb_columns_enums
    CHANGE COLUMN column_id column_id VARCHAR(36) NOT NULL;
-- mdb_columns_sets
ALTER TABLE mdb_columns_sets
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_columns_sets
    DROP FOREIGN KEY mdb_columns_sets_ibfk_1;
ALTER TABLE mdb_columns_sets
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_columns_sets`
    DROP PRIMARY KEY;
ALTER TABLE mdb_columns_sets
    CHANGE COLUMN column_id column_id VARCHAR(36) NOT NULL;
-- mdb_columns_units
ALTER TABLE mdb_columns_units
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_columns_units
    DROP FOREIGN KEY mdb_columns_units_ibfk_1;
ALTER TABLE mdb_columns_units
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_columns_units`
    DROP PRIMARY KEY;
ALTER TABLE mdb_columns_units
    CHANGE COLUMN cID cID VARCHAR(36) NOT NULL;
-- mdb_constraints_checks
ALTER TABLE mdb_constraints_checks
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_constraints_checks
    DROP FOREIGN KEY mdb_constraints_checks_ibfk_1;
ALTER TABLE mdb_constraints_checks
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_checks`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_checks
    CHANGE COLUMN tid tid VARCHAR(36) NOT NULL;
-- mdb_constraints_foreign_key_reference
ALTER TABLE mdb_constraints_foreign_key_reference
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_constraints_foreign_key_reference
    DROP FOREIGN KEY mdb_constraints_foreign_key_reference_ibfk_1;
ALTER TABLE mdb_constraints_foreign_key_reference
    DROP FOREIGN KEY mdb_constraints_foreign_key_reference_ibfk_2;
ALTER TABLE mdb_constraints_foreign_key_reference
    DROP FOREIGN KEY mdb_constraints_foreign_key_reference_ibfk_3;
ALTER TABLE mdb_constraints_foreign_key_reference
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_foreign_key_reference`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_foreign_key_reference
    CHANGE COLUMN fkid fkid VARCHAR(36) NOT NULL;
ALTER TABLE mdb_constraints_foreign_key_reference
    CHANGE COLUMN cid cid VARCHAR(36) NOT NULL;
ALTER TABLE mdb_constraints_foreign_key_reference
    CHANGE COLUMN rcid rcid VARCHAR(36) NOT NULL;
-- mdb_constraints_foreign_key
ALTER TABLE mdb_constraints_foreign_key
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_constraints_foreign_key
    DROP FOREIGN KEY mdb_constraints_foreign_key_ibfk_1;
ALTER TABLE mdb_constraints_foreign_key
    DROP FOREIGN KEY mdb_constraints_foreign_key_ibfk_2;
ALTER TABLE mdb_constraints_foreign_key
    CHANGE COLUMN fkid fkid VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_foreign_key`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_foreign_key
    CHANGE COLUMN tid tid VARCHAR(36) NOT NULL;
ALTER TABLE mdb_constraints_foreign_key
    CHANGE COLUMN rtid rtid VARCHAR(36) NOT NULL;
-- mdb_constraints_primary_key
ALTER TABLE mdb_constraints_primary_key
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_constraints_primary_key
    DROP FOREIGN KEY mdb_constraints_primary_key_ibfk_1;
ALTER TABLE mdb_constraints_primary_key
    DROP FOREIGN KEY mdb_constraints_primary_key_ibfk_2;
ALTER TABLE mdb_constraints_primary_key
    CHANGE COLUMN pkid pkid VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_primary_key`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_primary_key
    CHANGE COLUMN tID tID VARCHAR(36) NOT NULL;
ALTER TABLE mdb_constraints_primary_key
    CHANGE COLUMN cid cid VARCHAR(36) NOT NULL;
-- mdb_constraints_unique_columns
ALTER TABLE mdb_constraints_unique_columns
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_constraints_unique_columns
    DROP FOREIGN KEY mdb_constraints_unique_columns_ibfk_1;
ALTER TABLE mdb_constraints_unique_columns
    DROP FOREIGN KEY mdb_constraints_unique_columns_ibfk_2;
ALTER TABLE mdb_constraints_unique_columns
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_unique_columns`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_unique_columns
    CHANGE COLUMN uid uid VARCHAR(36) NOT NULL;
ALTER TABLE mdb_constraints_unique_columns
    CHANGE COLUMN cid cid VARCHAR(36) NOT NULL;
-- mdb_constraints_unique
ALTER TABLE mdb_constraints_unique
    DROP FOREIGN KEY mdb_constraints_unique_ibfk_1;
ALTER TABLE mdb_constraints_unique
    CHANGE COLUMN uid uid VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_constraints_unique`
    DROP PRIMARY KEY;
ALTER TABLE mdb_constraints_unique
    CHANGE COLUMN tid tid VARCHAR(36) NOT NULL;
-- mdb_columns
ALTER TABLE mdb_columns
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_columns
    DROP FOREIGN KEY mdb_columns_ibfk_1;
ALTER TABLE mdb_columns
    CHANGE COLUMN ID ID VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_columns`
    DROP PRIMARY KEY;
ALTER TABLE mdb_columns
    CHANGE COLUMN tID tID VARCHAR(36) NOT NULL;
-- mdb_tables
ALTER TABLE mdb_tables
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_tables
    DROP COLUMN `separator`;
ALTER TABLE mdb_tables
    DROP COLUMN `quote`;
ALTER TABLE mdb_tables
    DROP COLUMN `element_false`;
ALTER TABLE mdb_tables
    DROP COLUMN `element_null`;
ALTER TABLE mdb_tables
    DROP COLUMN `element_true`;
ALTER TABLE mdb_tables
    DROP COLUMN `skip_lines`;
ALTER TABLE mdb_tables
    DROP FOREIGN KEY mdb_tables_ibfk_1;
ALTER TABLE mdb_tables
    CHANGE COLUMN ID ID VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_tables`
    DROP PRIMARY KEY;
ALTER TABLE mdb_tables
    CHANGE COLUMN tDBID tDBID VARCHAR(36) NOT NULL;
-- mdb_view_columns
ALTER TABLE mdb_view_columns
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_view_columns
    DROP FOREIGN KEY mdb_view_columns_ibfk_1;
ALTER TABLE mdb_view_columns
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_view_columns`
    DROP PRIMARY KEY;
ALTER TABLE mdb_view_columns
    CHANGE COLUMN view_id view_id VARCHAR(36) NOT NULL;
-- mdb_view
ALTER TABLE mdb_view
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_view
    DROP FOREIGN KEY mdb_view_ibfk_1;
ALTER TABLE mdb_view
    CHANGE COLUMN ID ID VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_view`
    DROP PRIMARY KEY;
ALTER TABLE mdb_view
    CHANGE COLUMN vdbid vdbid VARCHAR(36) NOT NULL;
-- mdb_databases
ALTER TABLE mdb_databases
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_databases
    DROP FOREIGN KEY mdb_databases_ibfk_1;
ALTER TABLE mdb_databases
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_databases`
    DROP PRIMARY KEY;
ALTER TABLE mdb_databases
    CHANGE COLUMN cid cid VARCHAR(36) NOT NULL;
-- mdb_containers
ALTER TABLE mdb_containers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_containers
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_containers`
    DROP PRIMARY KEY;
ALTER TABLE mdb_containers
    DROP FOREIGN KEY mdb_containers_ibfk_1;
ALTER TABLE mdb_containers
    CHANGE COLUMN image_id image_id VARCHAR(36) NOT NULL;
-- mdb_images
ALTER TABLE mdb_images
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_images
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
ALTER TABLE `mdb_images`
    DROP PRIMARY KEY;
-- mdb_users
ALTER TABLE mdb_users
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_users
    CHANGE COLUMN id id VARCHAR(36) NOT NULL DEFAULT UUID();
-- mdb_images
ALTER TABLE mdb_images
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_images
    ADD SYSTEM VERSIONING;
-- mdb_containers
ALTER TABLE mdb_containers
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_containers
    ADD FOREIGN KEY (image_id) REFERENCES mdb_images (id);
ALTER TABLE mdb_containers
    ADD SYSTEM VERSIONING;
-- mdb_concepts
ALTER TABLE mdb_concepts
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_concepts
    ADD SYSTEM VERSIONING;
-- mdb_databases
ALTER TABLE mdb_databases
    ADD FOREIGN KEY (cid) REFERENCES mdb_containers (id);
ALTER TABLE mdb_databases
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_databases
    ADD SYSTEM VERSIONING;
-- mdb_tables
ALTER TABLE mdb_tables
    ADD FOREIGN KEY (tDBID) REFERENCES mdb_databases (id);
ALTER TABLE mdb_tables
    ADD PRIMARY KEY (ID);
ALTER TABLE mdb_tables
    ADD SYSTEM VERSIONING;
-- mdb_constraints_checks
ALTER TABLE mdb_constraints_checks
    ADD FOREIGN KEY (tid) REFERENCES mdb_tables (id);
ALTER TABLE mdb_constraints_checks
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_constraints_checks
    ADD SYSTEM VERSIONING;
-- mdb_constraints_foreign_key
ALTER TABLE mdb_constraints_foreign_key
    ADD FOREIGN KEY (tid) REFERENCES mdb_tables (id);
ALTER TABLE mdb_constraints_foreign_key
    ADD PRIMARY KEY (fkid);
-- mdb_columns
ALTER TABLE mdb_columns
    ADD FOREIGN KEY (tID) REFERENCES mdb_tables (id);
ALTER TABLE mdb_columns
    ADD PRIMARY KEY (ID);
ALTER TABLE mdb_columns
    ADD SYSTEM VERSIONING;
-- mdb_constraints_foreign_key_reference
ALTER TABLE mdb_constraints_foreign_key_reference
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_constraints_foreign_key_reference
    ADD FOREIGN KEY (fkid) REFERENCES mdb_constraints_foreign_key (fkid);
ALTER TABLE mdb_constraints_foreign_key_reference
    ADD FOREIGN KEY (cid) REFERENCES mdb_columns (ID);
ALTER TABLE mdb_constraints_foreign_key_reference
    ADD FOREIGN KEY (rcid) REFERENCES mdb_columns (ID);
ALTER TABLE mdb_constraints_foreign_key_reference
    ADD SYSTEM VERSIONING;
-- mdb_constraints_foreign_key
ALTER TABLE mdb_constraints_foreign_key
    ADD FOREIGN KEY (rtid) REFERENCES mdb_tables (`id`) ON DELETE CASCADE;
ALTER TABLE mdb_constraints_foreign_key
    ADD SYSTEM VERSIONING;
-- mdb_constraints_primary_key
ALTER TABLE mdb_constraints_primary_key
    ADD PRIMARY KEY (pkid);
ALTER TABLE mdb_constraints_primary_key
    ADD FOREIGN KEY (tID) REFERENCES mdb_tables (ID);
ALTER TABLE mdb_constraints_primary_key
    ADD FOREIGN KEY (cid) REFERENCES mdb_columns (ID);
ALTER TABLE mdb_constraints_primary_key
    ADD SYSTEM VERSIONING;
-- mdb_constraints_unique
ALTER TABLE mdb_constraints_unique
    ADD PRIMARY KEY (uid);
ALTER TABLE mdb_constraints_unique
    ADD FOREIGN KEY (tid) REFERENCES mdb_tables (ID);
ALTER TABLE mdb_constraints_unique
    ADD SYSTEM VERSIONING;
-- mdb_constraints_unique_columns
ALTER TABLE mdb_constraints_unique_columns
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_constraints_unique_columns
    ADD FOREIGN KEY (uid) REFERENCES mdb_constraints_unique (uid);
ALTER TABLE mdb_constraints_unique_columns
    ADD FOREIGN KEY (cid) REFERENCES mdb_columns (ID);
ALTER TABLE mdb_constraints_unique_columns
    ADD SYSTEM VERSIONING;
-- mdb_columns_enums
ALTER TABLE mdb_columns_enums
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_columns_enums
    ADD FOREIGN KEY (column_id) REFERENCES mdb_columns (id);
ALTER TABLE mdb_columns_enums
    ADD SYSTEM VERSIONING;
-- mdb_columns_sets
ALTER TABLE mdb_columns_sets
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_columns_sets
    ADD FOREIGN KEY (column_id) REFERENCES mdb_columns (id);
ALTER TABLE mdb_columns_sets
    ADD SYSTEM VERSIONING;
-- mdb_units
ALTER TABLE mdb_units
    ADD PRIMARY KEY (id);
-- mdb_columns_units
ALTER TABLE mdb_units
    ADD SYSTEM VERSIONING;
ALTER TABLE mdb_columns_units
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_columns_units
    ADD FOREIGN KEY (id) REFERENCES mdb_units (id);
ALTER TABLE mdb_columns_units
    ADD FOREIGN KEY (cID) REFERENCES mdb_columns (id);
ALTER TABLE mdb_columns_units
    ADD SYSTEM VERSIONING;
-- mdb_columns_concepts
ALTER TABLE mdb_columns_concepts
    ADD PRIMARY KEY (id, cid);
ALTER TABLE mdb_columns_concepts
    ADD FOREIGN KEY (id) REFERENCES mdb_concepts (id);
ALTER TABLE mdb_columns_concepts
    ADD FOREIGN KEY (cID) REFERENCES mdb_columns (id);
ALTER TABLE mdb_columns_concepts
    ADD SYSTEM VERSIONING;
-- mdb_view
ALTER TABLE mdb_view
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_view
    ADD FOREIGN KEY (vdbid) REFERENCES mdb_databases (id);
ALTER TABLE mdb_view
    ADD SYSTEM VERSIONING;
-- mdb_view_columns
ALTER TABLE mdb_view_columns
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_view_columns
    ADD FOREIGN KEY (view_id) REFERENCES mdb_view (id);
ALTER TABLE mdb_view_columns
    ADD SYSTEM VERSIONING;
-- mdb_access
ALTER TABLE mdb_access
    ADD PRIMARY KEY (aUserID, aDBID);
ALTER TABLE mdb_access
    ADD FOREIGN KEY (aDBID) REFERENCES mdb_databases (id);
ALTER TABLE mdb_access
    ADD FOREIGN KEY (aUserID) REFERENCES mdb_users (id);
ALTER TABLE mdb_access
    ADD SYSTEM VERSIONING;
-- mdb_identifiers
ALTER TABLE mdb_identifiers
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifiers
    ADD FOREIGN KEY (dbid) REFERENCES mdb_databases (id);
ALTER TABLE mdb_identifiers
    ADD FOREIGN KEY (tid) REFERENCES mdb_tables (id);
ALTER TABLE mdb_identifiers
    ADD FOREIGN KEY (vid) REFERENCES mdb_view (id);
ALTER TABLE mdb_identifiers
    ADD SYSTEM VERSIONING;
-- mdb_identifier_licenses
ALTER TABLE mdb_identifier_licenses
    ADD PRIMARY KEY (pid, license_id);
ALTER TABLE mdb_identifier_licenses
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_licenses
    ADD SYSTEM VERSIONING;
-- mdb_identifier_titles
ALTER TABLE mdb_identifier_titles
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifier_titles
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_titles
    ADD SYSTEM VERSIONING;
-- mdb_identifier_funders
ALTER TABLE mdb_identifier_funders
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifier_funders
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_funders
    ADD SYSTEM VERSIONING;
-- mdb_identifier_descriptions
ALTER TABLE mdb_identifier_descriptions
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifier_descriptions
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_descriptions
    ADD SYSTEM VERSIONING;
-- mdb_identifier_creators
ALTER TABLE mdb_identifier_creators
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifier_creators
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_creators
    ADD SYSTEM VERSIONING;
-- mdb_identifier_related
ALTER TABLE mdb_identifier_related
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_identifier_related
    ADD FOREIGN KEY (pid) REFERENCES mdb_identifiers (id);
ALTER TABLE mdb_identifier_related
    ADD SYSTEM VERSIONING;
-- mdb_users
ALTER TABLE mdb_users
    ADD SYSTEM VERSIONING;
-- mdb_have_access
ALTER TABLE mdb_have_access
    ADD PRIMARY KEY (user_id, database_id);
ALTER TABLE mdb_have_access
    ADD FOREIGN KEY (database_id) REFERENCES mdb_databases (id);
ALTER TABLE mdb_have_access
    ADD FOREIGN KEY (user_id) REFERENCES mdb_users (id);
ALTER TABLE mdb_have_access
    ADD SYSTEM VERSIONING;
-- mdb_image_types
ALTER TABLE mdb_image_types
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_image_types
    ADD FOREIGN KEY (image_id) REFERENCES mdb_images (id);
ALTER TABLE mdb_image_types
    ADD SYSTEM VERSIONING;
-- mdb_image_operators
ALTER TABLE mdb_image_operators
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_image_operators
    ADD FOREIGN KEY (image_id) REFERENCES mdb_images (id);
ALTER TABLE mdb_image_operators
    ADD SYSTEM VERSIONING;
-- mdb_ontologies
ALTER TABLE mdb_ontologies
    ADD PRIMARY KEY (id);
ALTER TABLE mdb_ontologies
    ADD SYSTEM VERSIONING;
-- mdb_messages
ALTER TABLE `mdb_messages`
    ADD SYSTEM VERSIONING;