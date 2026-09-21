ALTER TABLE mdb_databases
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_databases
    ADD COLUMN origin_owner_site VARCHAR(255),
    ADD COLUMN origin_owner_issuer VARCHAR(255),
    ADD COLUMN origin_owner_subject VARCHAR(255),
    ADD COLUMN origin_owner_username VARCHAR(255),
    ADD COLUMN replication_access_status VARCHAR(32),
    ADD COLUMN replication_local_username VARCHAR(255);
ALTER TABLE mdb_databases
    ADD SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_replication_identity_mappings
(
    id             VARCHAR(36)  NOT NULL DEFAULT UUID(),
    origin_site    VARCHAR(255) NOT NULL,
    origin_issuer  VARCHAR(255) NOT NULL,
    origin_subject VARCHAR(255) NOT NULL,
    local_username VARCHAR(255) NOT NULL,
    created        TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_replication_identity_mapping UNIQUE (origin_site, origin_issuer, origin_subject)
);
