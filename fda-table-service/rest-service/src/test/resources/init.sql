CREATE SCHEMA IF NOT EXISTS fda;
DROP TABLE IF EXISTS fda.mdb_concepts CASCADE;
CREATE TABLE IF NOT EXISTS fda.mdb_concepts
(
    uri        varchar(255) not null,
    name       VARCHAR(255),
    created    timestamp    NOT NULL DEFAULT NOW(),
    created_by bigint,
    PRIMARY KEY (uri)
);