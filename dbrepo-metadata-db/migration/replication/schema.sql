SET SESSION system_versioning_alter_history = KEEP;

ALTER TABLE mdb_databases
    ADD COLUMN IF NOT EXISTS creation_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS origin_owner_site VARCHAR(255),
    ADD COLUMN IF NOT EXISTS origin_owner_issuer VARCHAR(255),
    ADD COLUMN IF NOT EXISTS origin_owner_subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS origin_owner_username VARCHAR(255),
    ADD COLUMN IF NOT EXISTS replication_access_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS replication_local_username VARCHAR(255);
ALTER TABLE mdb_tables ADD COLUMN IF NOT EXISTS creation_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP(6) NULL DEFAULT NULL;
ALTER TABLE mdb_view ADD COLUMN IF NOT EXISTS creation_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP(6) NULL DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `mdb_replication_identity_mappings`
(
    id             VARCHAR(36)  NOT NULL DEFAULT UUID(),
    origin_site    VARCHAR(255) NOT NULL,
    origin_issuer  VARCHAR(255) NOT NULL,
    origin_subject VARCHAR(255) NOT NULL,
    local_username VARCHAR(255) NOT NULL,
    created        TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`id`),
    CONSTRAINT `uq_replication_identity_mapping` UNIQUE (`origin_site`, `origin_issuer`, `origin_subject`)
);

CREATE TABLE IF NOT EXISTS `mdb_databases_replica_urls`
(
    `database_id`         CHAR(36) NOT NULL,
    `replica_url`         TEXT     NOT NULL,
    `replica_database_id` CHAR(36) DEFAULT NULL,
    PRIMARY KEY (`database_id`, `replica_url`(255)),
    CONSTRAINT `fk_mdb_databases_replica_urls_database`
        FOREIGN KEY (`database_id`)
        REFERENCES `mdb_databases` (`id`)
        ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_tables_replica_urls`
(
    `table_id`         VARCHAR(36) NOT NULL,
    `replica_table_id` VARCHAR(36) DEFAULT NULL,
    `replica_url`      TEXT        NOT NULL,
    PRIMARY KEY (`table_id`, `replica_url`(255)),
    CONSTRAINT `fk_mdb_tables_replica_urls_table`
        FOREIGN KEY (`table_id`)
        REFERENCES `mdb_tables` (`id`)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `mdb_replication_notification_outbox`
(
    id                VARCHAR(36) NOT NULL DEFAULT UUID(),
    notification_type VARCHAR(64) NOT NULL,
    http_method       VARCHAR(16) NOT NULL,
    path              VARCHAR(255) NOT NULL,
    aggregate_id      VARCHAR(36),
    payload           LONGTEXT    NOT NULL,
    status            VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts          INT         NOT NULL DEFAULT 0,
    last_error        TEXT,
    created           TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_modified     TIMESTAMP,
    next_attempt_at   TIMESTAMP   NULL DEFAULT NOW(),
    PRIMARY KEY (`id`),
    INDEX idx_mdb_replication_notification_outbox_due (`status`, `next_attempt_at`),
    INDEX idx_mdb_replication_notification_outbox_aggregate (`aggregate_id`)
);

ALTER TABLE mdb_replication_notification_outbox
    MODIFY next_attempt_at TIMESTAMP NULL DEFAULT NOW();
