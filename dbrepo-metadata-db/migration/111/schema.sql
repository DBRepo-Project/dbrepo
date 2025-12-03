-- mdb_databases
ALTER TABLE mdb_databases
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_databases
    MODIFY owned_by VARCHAR(36) NOT NULL;
ALTER TABLE mdb_databases
    MODIFY contact_person VARCHAR(36) NOT NULL;
ALTER TABLE mdb_databases
    DROP FOREIGN KEY mdb_databases_ibfk_2;
ALTER TABLE mdb_databases
    DROP FOREIGN KEY mdb_databases_ibfk_3;
ALTER TABLE mdb_databases
    ADD SYSTEM VERSIONING;
# mdb_tables
ALTER TABLE mdb_tables
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_tables
    MODIFY owned_by VARCHAR(36) NOT NULL;
ALTER TABLE mdb_tables
    DROP FOREIGN KEY mdb_tables_ibfk_2;
ALTER TABLE mdb_tables
    ADD SYSTEM VERSIONING;
-- mdb_view
ALTER TABLE mdb_view
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_view
    MODIFY owned_by VARCHAR(36) NOT NULL;
ALTER TABLE mdb_view
    DROP FOREIGN KEY mdb_view_ibfk_2;
ALTER TABLE mdb_view
    ADD SYSTEM VERSIONING;
-- mdb_identifiers
ALTER TABLE mdb_identifiers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_identifiers
    MODIFY owned_by VARCHAR(36) NOT NULL;
ALTER TABLE mdb_identifiers
    DROP FOREIGN KEY mdb_identifiers_ibfk_2;
ALTER TABLE mdb_identifiers
    ADD SYSTEM VERSIONING;
-- mdb_access
ALTER TABLE mdb_access
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_access
    CHANGE aUserID username VARCHAR(255) NOT NULL;
ALTER TABLE mdb_access
    DROP FOREIGN KEY mdb_access_ibfk_2;
ALTER TABLE mdb_access
    DROP PRIMARY KEY;
ALTER TABLE mdb_access
    ADD PRIMARY KEY (username, aDBID);
ALTER TABLE mdb_access
    ADD SYSTEM VERSIONING;
-- mdb_have_access
ALTER TABLE mdb_have_access
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_have_access
    DROP FOREIGN KEY mdb_have_access_ibfk_2;
ALTER TABLE mdb_have_access
    CHANGE user_id username VARCHAR(255) NOT NULL;
ALTER TABLE mdb_have_access
    DROP PRIMARY KEY;
ALTER TABLE mdb_have_access
    ADD PRIMARY KEY (username, database_id);
ALTER TABLE mdb_have_access
    ADD SYSTEM VERSIONING;
-- mdb_users
DROP TABLE mdb_users;