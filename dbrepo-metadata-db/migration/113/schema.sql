CREATE TABLE IF NOT EXISTS mdb_replication_notification_outbox
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
    next_attempt_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    INDEX idx_mdb_replication_notification_outbox_due (status, next_attempt_at),
    INDEX idx_mdb_replication_notification_outbox_aggregate (aggregate_id)
);
