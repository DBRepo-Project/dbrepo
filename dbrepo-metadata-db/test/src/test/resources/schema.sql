CREATE SCHEMA IF NOT EXISTS `fda`;
SET SCHEMA `fda`;
DROP TABLE IF EXISTS fda.mdb_concepts;
CREATE TABLE fda.mdb_concepts
(
    uri        VARCHAR(500) not null,
    name       VARCHAR(255),
    created    timestamp    NOT NULL DEFAULT NOW(),
    created_by bigint,
    PRIMARY KEY (uri)
);
DROP TABLE IF EXISTS fda.mdb_units;
CREATE TABLE fda.mdb_units
(
    uri        VARCHAR(500) not null,
    name       VARCHAR(255),
    created    timestamp    NOT NULL DEFAULT NOW(),
    created_by bigint,
    PRIMARY KEY (uri)
);