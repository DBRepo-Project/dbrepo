BEGIN;

CREATE TABLE IF NOT EXISTS tuple_replication_timestamps
(
    site_url        TEXT         NOT NULL,
    replication_id  VARCHAR(255) NOT NULL,
    database_id     VARCHAR(36)  NOT NULL,
    table_id        VARCHAR(36)  NOT NULL,
    row_start       TIMESTAMP(6) NOT NULL,
    row_end         TIMESTAMP(6),
    PRIMARY KEY (`site_url`(255), `replication_id`, `row_start`)
);

COMMIT;
