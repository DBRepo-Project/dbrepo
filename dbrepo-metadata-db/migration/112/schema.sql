ALTER TABLE mdb_databases
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_databases
    ADD COLUMN creation_location VARCHAR(255);
ALTER TABLE mdb_databases
    ADD SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_databases_replica_urls
(
    database_id         CHAR(36) NOT NULL,
    replica_url         TEXT     NOT NULL,
    replica_database_id CHAR(36) DEFAULT NULL,
    PRIMARY KEY (database_id, replica_url(255)),
    CONSTRAINT fk_mdb_databases_replica_urls_database
        FOREIGN KEY (database_id)
        REFERENCES mdb_databases (id)
        ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

ALTER TABLE mdb_tables
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_tables
    ADD COLUMN creation_location VARCHAR(255);
ALTER TABLE mdb_tables
    ADD SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_tables_replica_urls
(
    table_id         VARCHAR(36) NOT NULL,
    replica_table_id VARCHAR(36) DEFAULT NULL,
    replica_url      TEXT        NOT NULL,
    PRIMARY KEY (table_id, replica_url(255)),
    CONSTRAINT fk_mdb_tables_replica_urls_table
        FOREIGN KEY (table_id)
        REFERENCES mdb_tables (id)
        ON DELETE CASCADE
);

ALTER TABLE mdb_view
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_view
    ADD COLUMN creation_location VARCHAR(255);
ALTER TABLE mdb_view
    ADD SYSTEM VERSIONING;
