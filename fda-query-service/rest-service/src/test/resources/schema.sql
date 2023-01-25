CREATE SCHEMA IF NOT EXISTS `fda`;
SET SCHEMA `fda`;
DROP TABLE IF EXISTS fda.mdb_concepts;
CREATE TABLE IF NOT EXISTS fda.mdb_concepts
(
    uri        VARCHAR(500) not null,
    name       VARCHAR(255),
    created    timestamp    NOT NULL DEFAULT NOW(),
    created_by bigint,
    PRIMARY KEY (uri)
);
DROP TABLE IF EXISTS fda.mdb_units;
CREATE TABLE IF NOT EXISTS fda.mdb_units
(
    uri        VARCHAR(500) not null,
    name       VARCHAR(255),
    created    timestamp    NOT NULL DEFAULT NOW(),
    created_by bigint,
    PRIMARY KEY (uri)
);
-- Modified for H2
-- Assume id=1 is invalid
-- Assume id=2 is still valid token
-- CREATE VIEW IF NOT EXISTS fda.mdb_invalid_tokens AS
-- (SELECT `id`, `token_hash`, `creator`, `created`, `expires`, `last_used` FROM fda.`mdb_tokens` WHERE `id` = 1);