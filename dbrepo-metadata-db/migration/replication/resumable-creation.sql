-- Apply before deploying resumable replicated database/table creation.
-- Intents deliberately outlive failed metadata transactions and must not be pruned on retry.
CREATE TABLE IF NOT EXISTS mdb_replication_creations (
    id VARCHAR(36) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    parent_id VARCHAR(36) NOT NULL,
    physical_name VARCHAR(64) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_replication_creation_name (kind, parent_id, physical_name)
) ENGINE=InnoDB;
