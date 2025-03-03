BEGIN;

CREATE TABLE IF NOT EXISTS `mdb_users`
(
    id               VARCHAR(36)  NOT NULL DEFAULT UUID(),
    keycloak_id      VARCHAR(36)  NOT NULL,
    username         VARCHAR(255) NOT NULL,
    firstname        VARCHAR(255),
    lastname         VARCHAR(255),
    orcid            VARCHAR(255),
    affiliation      VARCHAR(255),
    is_internal      BOOLEAN      NOT NULL DEFAULT FALSE,
    mariadb_password VARCHAR(255) NOT NULL,
    theme            VARCHAR(255) NOT NULL DEFAULT ('light'),
    language         VARCHAR(3)   NOT NULL DEFAULT ('en'),
    PRIMARY KEY (`id`),
    UNIQUE (`keycloak_id`),
    UNIQUE (`username`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_images`
(
    id            VARCHAR(36)  NOT NULL DEFAULT UUID(),
    registry      VARCHAR(255) NOT NULL DEFAULT 'docker.io',
    name          VARCHAR(255) NOT NULL,
    version       VARCHAR(255) NOT NULL,
    DEFAULT_port  INT          NOT NULL,
    dialect       VARCHAR(255) NOT NULL,
    driver_class  VARCHAR(255) NOT NULL,
    jdbc_method   VARCHAR(255) NOT NULL,
    is_DEFAULT    BOOLEAN      NOT NULL DEFAULT FALSE,
    created       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE (`name`, `version`),
    UNIQUE (`is_DEFAULT`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_containers`
(
    id                  VARCHAR(36)  NOT NULL DEFAULT UUID(),
    internal_name       VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    host                VARCHAR(255) NOT NULL,
    port                INT          NOT NULL DEFAULT 3306,
    ui_host             VARCHAR(255) NOT NULL DEFAULT host,
    ui_port             INT          NOT NULL DEFAULT port,
    ui_additional_flags TEXT,
    image_id            VARCHAR(36)  NOT NULL,
    created             TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified       TIMESTAMP,
    privileged_username VARCHAR(255) NOT NULL,
    privileged_password VARCHAR(255) NOT NULL,
    quota               INT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`image_id`) REFERENCES mdb_images (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_licenses`
(
    identifier  VARCHAR(255) NOT NULL,
    uri         TEXT         NOT NULL,
    description TEXT         NOT NULL,
    PRIMARY KEY (`identifier`),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_databases`
(
    id               VARCHAR(36)  NOT NULL DEFAULT UUID(),
    cid              VARCHAR(36)  NOT NULL,
    name             VARCHAR(255) NOT NULL,
    internal_name    VARCHAR(255) NOT NULL,
    exchange_name    VARCHAR(255) NOT NULL,
    description      TEXT,
    engine           VARCHAR(20),
    is_public        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_schema_public BOOLEAN      NOT NULL DEFAULT TRUE,
    image            LONGBLOB,
    owned_by         VARCHAR(36)  NOT NULL,
    contact_person   VARCHAR(36)  NOT NULL,
    created          TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified    TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`cid`) REFERENCES mdb_containers (`id`),
    FOREIGN KEY (`owned_by`) REFERENCES mdb_users (`id`),
    FOREIGN KEY (`contact_person`) REFERENCES mdb_users (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_tables`
(
    id               VARCHAR(36)  NOT NULL DEFAULT UUID(),
    tDBID            VARCHAR(36)  NOT NULL,
    tName            VARCHAR(64)  NOT NULL,
    internal_name    VARCHAR(64)  NOT NULL,
    queue_name       VARCHAR(255) NOT NULL,
    routing_key      VARCHAR(255),
    tDescription     VARCHAR(2048),
    num_rows         BIGINT,
    data_length      BIGINT,
    max_data_length  BIGINT,
    avg_row_length   BIGINT,
    created          TIMESTAMP    NOT NULL DEFAULT NOW(),
    versioned        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_public        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_schema_public BOOLEAN      NOT NULL DEFAULT TRUE,
    owned_by         VARCHAR(36)  NOT NULL DEFAULT UUID(),
    last_modified    TIMESTAMP,
    PRIMARY KEY (`ID`),
    UNIQUE (`tDBID`, `internal_name`),
    FOREIGN KEY (`tDBID`) REFERENCES mdb_databases (`id`),
    FOREIGN KEY (`owned_by`) REFERENCES mdb_users (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns`
(
    id               VARCHAR(36)     NOT NULL DEFAULT UUID(),
    tID              VARCHAR(36)     NOT NULL,
    cName            VARCHAR(64),
    internal_name    VARCHAR(64)     NOT NULL,
    Datatype         ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','SERIAL','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    length           BIGINT UNSIGNED NULL,
    ordinal_position INT             NOT NULL,
    index_length     BIGINT UNSIGNED NULL,
    description      VARCHAR(2048),
    size             BIGINT UNSIGNED,
    d                BIGINT UNSIGNED,
    is_null_allowed  BOOLEAN         NOT NULL DEFAULT TRUE,
    val_min          NUMERIC         NULL,
    val_max          NUMERIC         NULL,
    mean             NUMERIC         NULL,
    median           NUMERIC         NULL,
    std_dev          Numeric         NULL,
    created          TIMESTAMP       NOT NULL DEFAULT NOW(),
    last_modified    TIMESTAMP,
    FOREIGN KEY (`tID`) REFERENCES mdb_tables (`ID`) ON DELETE CASCADE,
    PRIMARY KEY (`ID`),
    UNIQUE (`tID`, `internal_name`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_enums`
(
    id        VARCHAR(36)  NOT NULL DEFAULT UUID(),
    column_id VARCHAR(36)  NOT NULL,
    value     VARCHAR(255) NOT NULL,
    FOREIGN KEY (`column_id`) REFERENCES mdb_columns (`ID`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_sets`
(
    id        VARCHAR(36)  NOT NULL DEFAULT UUID(),
    column_id VARCHAR(36)  NOT NULL,
    value     VARCHAR(255) NOT NULL,
    FOREIGN KEY (`column_id`) REFERENCES mdb_columns (`ID`) ON DELETE CASCADE,
    PRIMARY KEY (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key`
(
    fkid      VARCHAR(36)  NOT NULL DEFAULT UUID(),
    tid       VARCHAR(36)  NOT NULL,
    rtid      VARCHAR(36)  NOT NULL,
    name      VARCHAR(255) NOT NULL,
    on_update VARCHAR(50)  NULL,
    on_delete VARCHAR(50)  NULL,
    position  INT          NULL,
    PRIMARY KEY (`fkid`),
    FOREIGN KEY (`tid`) REFERENCES mdb_tables (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`rtid`) REFERENCES mdb_tables (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_primary_key`
(
    pkid VARCHAR(36) NOT NULL DEFAULT UUID(),
    tID  VARCHAR(36) NOT NULL,
    cid  VARCHAR(36) NOT NULL,
    PRIMARY KEY (`pkid`),
    FOREIGN KEY (`tID`) REFERENCES mdb_tables (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`cid`) REFERENCES mdb_columns (`id`) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key_reference`
(
    id   VARCHAR(36) NOT NULL DEFAULT UUID(),
    fkid VARCHAR(36) NOT NULL,
    cid  VARCHAR(36) NOT NULL,
    rcid VARCHAR(36) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (fkid, cid, rcid),
    FOREIGN KEY (`fkid`) REFERENCES mdb_constraints_foreign_key (`fkid`) ON UPDATE CASCADE,
    FOREIGN KEY (`cid`) REFERENCES mdb_columns (`id`),
    FOREIGN KEY (`rcid`) REFERENCES mdb_columns (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique`
(
    uid      VARCHAR(36)  NOT NULL DEFAULT UUID(),
    name     VARCHAR(255) NOT NULL,
    tid      VARCHAR(36)  NOT NULL,
    position INT          NULL,
    PRIMARY KEY (`uid`),
    FOREIGN KEY (`tid`) REFERENCES mdb_tables (`id`) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique_columns`
(
    id  VARCHAR(36) NOT NULL DEFAULT UUID(),
    uid VARCHAR(36) NOT NULL,
    cid VARCHAR(36) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`uid`) REFERENCES mdb_constraints_unique (`uid`),
    FOREIGN KEY (`cid`) REFERENCES mdb_columns (`id`) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_checks`
(
    id     VARCHAR(36)  NOT NULL DEFAULT UUID(),
    tid    VARCHAR(36)  NOT NULL,
    checks VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`tid`) REFERENCES mdb_tables (`id`) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;


CREATE TABLE IF NOT EXISTS `mdb_concepts`
(
    id          VARCHAR(36)  NOT NULL DEFAULT UUID(),
    uri         TEXT         NOT NULL,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`id`),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_units`
(
    id          VARCHAR(36)  NOT NULL DEFAULT UUID(),
    uri         TEXT         NOT NULL,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`id`),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_concepts`
(
    id      VARCHAR(36) NOT NULL DEFAULT UUID(),
    cID     VARCHAR(36) NOT NULL,
    created TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, cid),
    FOREIGN KEY (`id`) REFERENCES mdb_concepts (`id`),
    FOREIGN KEY (`cID`) REFERENCES mdb_columns (`ID`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_units`
(
    id      VARCHAR(36) NOT NULL DEFAULT UUID(),
    cID     VARCHAR(36) NOT NULL,
    created TIMESTAMP   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, cID),
    FOREIGN KEY (id) REFERENCES mdb_units (id),
    FOREIGN KEY (`cID`) REFERENCES mdb_columns (`ID`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view`
(
    id               VARCHAR(36)  NOT NULL DEFAULT UUID(),
    vdbid            VARCHAR(36)  NOT NULL,
    vName            VARCHAR(64)  NOT NULL,
    internal_name    VARCHAR(64)  NOT NULL,
    Query            TEXT         NOT NULL,
    query_hash       VARCHAR(255) NOT NULL,
    Public           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_schema_public BOOLEAN      NOT NULL DEFAULT TRUE,
    InitialView      BOOLEAN      NOT NULL,
    created          TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified    TIMESTAMP,
    owned_by         VARCHAR(36)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE (`vdbid`, `internal_name`),
    FOREIGN KEY (`vdbid`) REFERENCES mdb_databases (`id`),
    FOREIGN KEY (`owned_by`) REFERENCES mdb_users (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_messages`
(
    id            VARCHAR(36)                       NOT NULL DEFAULT UUID(),
    type          ENUM ('ERROR', 'WARNING', 'INFO') NOT NULL DEFAULT 'INFO',
    message       TEXT                              NOT NULL,
    link          TEXT                              NULL,
    link_TEXT     VARCHAR(255)                      NULL,
    display_start TIMESTAMP                         NULL,
    display_end   TIMESTAMP                         NULL,
    PRIMARY KEY (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_ontologies`
(
    id              VARCHAR(36) NOT NULL DEFAULT UUID(),
    prefix          VARCHAR(8)  NOT NULL,
    uri             TEXT        NOT NULL,
    uri_pattern     TEXT,
    sparql_endpoint TEXT        NULL,
    rdf_path        TEXT        NULL,
    last_modified   TIMESTAMP,
    created         TIMESTAMP   NOT NULL DEFAULT NOW(),
    UNIQUE (`prefix`),
    UNIQUE (uri(200)),
    PRIMARY KEY (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view_columns`
(
    id               VARCHAR(36) NOT NULL DEFAULT UUID(),
    view_id          VARCHAR(36) NOT NULL,
    name             VARCHAR(64),
    internal_name    VARCHAR(64) NOT NULL,
    column_type      ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    ordinal_position INT         NOT NULL,
    size             BIGINT UNSIGNED,
    d                BIGINT UNSIGNED,
    is_null_allowed  BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`view_id`) REFERENCES mdb_view (`id`) ON DELETE CASCADE,
    UNIQUE (view_id, internal_name)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifiers`
(
    id                VARCHAR(36)                                  NOT NULL DEFAULT UUID(),
    dbid              VARCHAR(36)                                  NOT NULL,
    qid               VARCHAR(36),
    vid               VARCHAR(36),
    tid               VARCHAR(36),
    publisher         VARCHAR(255)                                 NOT NULL,
    language          VARCHAR(2),
    publication_year  INT                                          NOT NULL,
    publication_month INT,
    publication_day   INT,
    identifier_type   ENUM ('DATABASE', 'SUBSET', 'VIEW', 'TABLE') NOT NULL,
    status            ENUM ('DRAFT', 'PUBLISHED')                  NOT NULL DEFAULT ('PUBLISHED'),
    query             TEXT,
    query_normalized  TEXT,
    query_hash        VARCHAR(255),
    execution         TIMESTAMP,
    result_hash       VARCHAR(255),
    result_number     BIGINT,
    doi               VARCHAR(255),
    created           TIMESTAMP                                    NOT NULL DEFAULT NOW(),
    owned_by          VARCHAR(36)                                  NOT NULL,
    last_modified     TIMESTAMP,
    PRIMARY KEY (`id`), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (`dbid`) REFERENCES mdb_databases (`id`),
    FOREIGN KEY (`owned_by`) REFERENCES mdb_users (`id`),
    FOREIGN KEY (`tid`) REFERENCES mdb_tables (`id`),
    FOREIGN KEY (`vid`) REFERENCES mdb_view (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_licenses`
(
    pid        VARCHAR(36)  NOT NULL DEFAULT UUID(),
    license_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (pid, license_id),
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`),
    FOREIGN KEY (`license_id`) REFERENCES mdb_licenses (`identifier`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_titles`
(
    id         VARCHAR(36) NOT NULL DEFAULT UUID(),
    pid        VARCHAR(36) NOT NULL,
    title      TEXT        NOT NULL,
    title_type ENUM ('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER'),
    language   VARCHAR(2),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_funders`
(
    id                     VARCHAR(36)  NOT NULL DEFAULT UUID(),
    pid                    VARCHAR(36)  NOT NULL,
    funder_name            VARCHAR(255) NOT NULL,
    funder_identifier      TEXT,
    funder_identifier_type ENUM ('CROSSREF_FUNDER_ID', 'GRID', 'ISNI', 'ROR', 'OTHER'),
    scheme_uri             TEXT,
    award_number           VARCHAR(255),
    award_title            TEXT,
    language               VARCHAR(255),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_descriptions`
(
    id               VARCHAR(36) NOT NULL DEFAULT UUID(),
    pid              VARCHAR(36) NOT NULL,
    description      TEXT        NOT NULL,
    description_type ENUM ('ABSTRACT', 'METHODS', 'SERIES_INFORMATION', 'TABLE_OF_CONTENTS', 'TECHNICAL_INFO', 'OTHER'),
    language         VARCHAR(2),
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_related`
(
    id       VARCHAR(36)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          NOT NULL DEFAULT UUID(),
    pid      VARCHAR(36)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          NOT NULL DEFAULT UUID(),
    value    VARCHAR(255)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         NOT NULL,
    type     ENUM ('DOI','URL','URN','ARK','ARXIV','BIBCODE','EAN13','EISSN','HANDLE','IGSN','ISBN','ISTC','LISSN','LSID','PMID','PURL','UPC','W3ID')                                                                                                                                                                                                                                                                                                                                                                                                                             NOT NULL,
    relation ENUM ('IS_CITED_BY','CITES','IS_SUPPLEMENT_TO','IS_SUPPLEMENTED_BY','IS_CONTINUED_BY','CONTINUES','IS_DESCRIBED_BY','DESCRIBES','HAS_METADATA','IS_METADATA_FOR','HAS_VERSION','IS_VERSION_OF','IS_NEW_VERSION_OF','IS_PREVIOUS_VERSION_OF','IS_PART_OF','HAS_PART','IS_PUBLISHED_IN','IS_REFERENCED_BY','REFERENCES','IS_DOCUMENTED_BY','DOCUMENTS','IS_COMPILED_BY','COMPILES','IS_VARIANT_FORM_OF','IS_ORIGINAL_FORM_OF','IS_IDENTICAL_TO','IS_REVIEWED_BY','REVIEWS','IS_DERIVED_FROM','IS_SOURCE_OF','IS_REQUIRED_BY','REQUIRES','IS_OBSOLETED_BY','OBSOLETES') NOT NULL,
    PRIMARY KEY (`id`), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`),
    UNIQUE (pid, value)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_creators`
(
    id                                VARCHAR(36)  NOT NULL               DEFAULT UUID(),
    pid                               VARCHAR(36)  NOT NULL,
    given_names                       TEXT,
    family_name                       TEXT,
    creator_name                      VARCHAR(255) NOT NULL,
    name_type                         ENUM ('PERSONAL', 'ORGANIZATIONAL') DEFAULT 'PERSONAL',
    name_identifier                   TEXT,
    name_identifier_scheme            ENUM ('ROR', 'GRID', 'ISNI', 'ORCID'),
    name_identifier_scheme_uri        TEXT,
    affiliation                       VARCHAR(255),
    affiliation_identifier            TEXT,
    affiliation_identifier_scheme     ENUM ('ROR', 'GRID', 'ISNI'),
    affiliation_identifier_scheme_uri TEXT,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`pid`) REFERENCES mdb_identifiers (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_access`
(
    aUserID  VARCHAR(255) NOT NULL,
    aDBID    VARCHAR(36)  NOT NULL,
    attime   TIMESTAMP,
    download BOOLEAN,
    created  TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID),
    FOREIGN KEY (`aDBID`) REFERENCES mdb_databases (`id`),
    FOREIGN KEY (`aUserID`) REFERENCES mdb_users (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_have_access`
(
    user_id     VARCHAR(36)                             NOT NULL,
    database_id VARCHAR(36)                             NOT NULL,
    access_type ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL') NOT NULL,
    created     TIMESTAMP                               NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, database_id),
    FOREIGN KEY (`database_id`) REFERENCES mdb_databases (`id`),
    FOREIGN KEY (`user_id`) REFERENCES mdb_users (`id`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_types`
(
    id            VARCHAR(36)  NOT NULL DEFAULT UUID(),
    image_id      VARCHAR(36)  NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    value         VARCHAR(255) NOT NULL,
    size_min      INT UNSIGNED,
    size_max      INT UNSIGNED,
    size_DEFAULT  INT UNSIGNED,
    size_required BOOLEAN comment 'When setting NULL, the service assumes the data type has no size',
    size_step     INT UNSIGNED,
    d_min         INT UNSIGNED,
    d_max         INT UNSIGNED,
    d_DEFAULT     INT UNSIGNED,
    d_required    BOOLEAN comment 'When setting NULL, the service assumes the data type has no d',
    d_step        INT UNSIGNED,
    type_hint     TEXT,
    data_hint     TEXT,
    documentation TEXT         NOT NULL,
    is_generated  BOOLEAN      NOT NULL,
    is_quoted     BOOLEAN      NOT NULL,
    is_buildable  BOOLEAN      NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`image_id`) REFERENCES `mdb_images` (`id`),
    UNIQUE (`value`)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_operators`
(
    id            VARCHAR(36)  NOT NULL DEFAULT UUID(),
    image_id      VARCHAR(36)  NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    value         VARCHAR(255) NOT NULL,
    documentation TEXT         NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`image_id`) REFERENCES `mdb_images` (`id`),
    UNIQUE (image_id, value)
) WITH SYSTEM VERSIONING;

COMMIT;

BEGIN;

INSERT INTO `mdb_licenses` (identifier, uri, description)
VALUES ('CC0-1.0', 'https://creativecommons.org/publicdomain/zero/1.0/legalcode',
        'CC0 waives copyright interest in a work you''ve created and dedicates it to the world-wide public domain. Use CC0 to opt out of copyright entirely and ensure your work has the widest reach.'),
       ('CC-BY-4.0', 'https://creativecommons.org/licenses/by/4.0/legalcode',
        'The Creative Commons Attribution license allows re-distribution and re-use of a licensed work on the condition that the creator is appropriately credited.');

INSERT INTO `mdb_images` (id, name, registry, version, DEFAULT_port, dialect, driver_class, jdbc_method)
VALUES ('d79cb089-363c-488b-9717-649e44d8fcc5', 'mariadb', 'docker.io', '11.1.3', 3306,
        'org.hibernate.dialect.MariaDBDialect', 'org.mariadb.jdbc.Driver', 'mariadb');

INSERT INTO `mdb_image_types` (image_id, display_name, value, size_min, size_max, size_DEFAULT, size_required,
                               size_step, d_min, d_max, d_DEFAULT, d_required, d_step, type_hint, data_hint,
                               documentation, is_quoted, is_buildable, is_generated)
VALUES ('d79cb089-363c-488b-9717-649e44d8fcc5', 'BIGINT(size)', 'bigint', 0, null, null, FALSE, 1, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/bigint/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'BINARY(size)', 'binary', 0, 255, 255, TRUE, 1, null, null, null, null,
        null, 'size in Bytes', null, 'https://mariadb.com/kb/en/binary/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'BIT(size)', 'bit', 0, 64, null, FALSE, 1, null, null, null, null, null,
        null, null, 'https://mariadb.com/kb/en/bit/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'BLOB(size)', 'blob', 0, 65535, null, FALSE, 1, null, null, null, null,
        null, 'size in Bytes', null, 'https://mariadb.com/kb/en/blob/', FALSE, FALSE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'BOOL', 'bool', null, null, null, null, null, null, null, null, null,
        null, null, null, 'https://mariadb.com/kb/en/bool/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'CHAR(size)', 'char', 0, 255, 255, FALSE, 1, null, null, null, null,
        null, null, null, 'https://mariadb.com/kb/en/char/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'DATE', 'date', null, null, null, null, null, null, null, null, null,
        null, 'min. 1000-01-01, max. 9999-12-31', 'e.g. YYYY-MM-DD, YY-MM-DD, YYMMDD, YYYY/MM/DD',
        'https://mariadb.com/kb/en/date/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'DATETIME(fsp)', 'datetime', 0, 6, null, null, 1, null, null, null,
        null, null, 'fsp=microsecond precision, min. 1000-01-01 00:00:00.0, max. 9999-12-31 23:59:59.9',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/datetime/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'DECIMAL(size, d)', 'decimal', 0, 65, null, FALSE, 1, 0, 38, null,
        FALSE, null, null, null, 'https://mariadb.com/kb/en/decimal/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'DOUBLE(size, d)', 'double', null, null, null, FALSE, null, null, null,
        null, FALSE, null, null, null, 'https://mariadb.com/kb/en/double/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'ENUM(v1,v2,...)', 'enum', null, null, null, null, null, null, null,
        null, null, null, null, 'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/enum/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'FLOAT(size)', 'float', null, null, null, FALSE, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/float/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'INT(size)', 'int', null, null, null, FALSE, null, null, null, null,
        null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/int/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'LONGBLOB', 'longblob', null, null, null, null, null, null, null, null,
        null, null, 'max. 3.999 GiB', null, 'https://mariadb.com/kb/en/longblob/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'LONGTEXT', 'longtext', null, null, null, null, null, null, null, null,
        null, null, 'max. 3.999 GiB', null, 'https://mariadb.com/kb/en/longtext/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'MEDIUMBLOB', 'mediumblob', null, null, null, null, null, null, null,
        null, null, null, 'max. 15.999 MiB', null, 'https://mariadb.com/kb/en/mediumblob/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'MEDIUMINT', 'mediumint', null, null, null, null, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/mediumint/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'MEDIUMTEXT', 'mediumtext', null, null, null, null, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/mediumtext/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'SERIAL', 'serial', null, null, null, null, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/bigint/', TRUE, TRUE, TRUE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'SET(v1,v2,...)', 'set', null, null, null, null, null, null, null, null,
        null, null, null, 'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/set/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'SMALLINT(size)', 'smallint', 0, null, null, FALSE, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/smallint/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TEXT(size)', 'text', 0, null, null, FALSE, null, null, null, null,
        null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/text/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TIME(fsp)', 'time', 0, 6, 0, FALSE, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'e.g. HH:MM:SS, HH:MM, HHMMSS, H:M:S',
        'https://mariadb.com/kb/en/time/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TIMESTAMP(fsp)', 'timestamp', 0, 6, 0, FALSE, null, null, null, null,
        null, null, 'fsp=microsecond precision, min. 0, max. 6',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/timestamp/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TINYBLOB', 'tinyblob', null, null, null, null, null, null, null, null,
        null, null, null, 'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/timestamp/', FALSE,
        TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TINYINT(size)', 'tinyint', 0, null, null, FALSE, null, null, null,
        null, null, null, null, 'size in Bytes', 'https://mariadb.com/kb/en/tinyint/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'TINYTEXT', 'tinytext', null, null, null, null, null, null, null, null,
        null, null, null, 'max. 255 characters', 'https://mariadb.com/kb/en/tinytext/', TRUE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'YEAR', 'year', 2, 4, null, FALSE, 2, null, null, null, null, null,
        'min. 1901, max. 2155', 'e.g. YYYY, YY', 'https://mariadb.com/kb/en/year/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'VARBINARY(size)', 'varbinary', 0, null, null, TRUE, null, null, null,
        null, null, null, null, null, 'https://mariadb.com/kb/en/varbinary/', FALSE, TRUE, FALSE),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'VARCHAR(size)', 'varchar', 0, 65532, 255, TRUE, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/varchar/', FALSE, TRUE, FALSE);


INSERT INTO `mdb_image_operators` (image_id, display_name, value, documentation)
VALUES ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Equal operator', '=',
        'https://mariadb.com/kb/en/assignment-operators-assignment-operator/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'NULL-safe equal operator', '<=>',
        'https://mariadb.com/kb/en/null-safe-equal/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Less-than operator', '<', 'https://mariadb.com/kb/en/less-than/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Less than or equal operator', '<=',
        'https://mariadb.com/kb/en/less-than-or-equal/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Greater-than operator', '>',
        'https://mariadb.com/kb/en/greater-than/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Greater than or equal operator', '>=',
        'https://mariadb.com/kb/en/greater-than-or-equal/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'Not equal operator', '!=', 'https://mariadb.com/kb/en/not-equal/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'LIKE', 'LIKE', 'https://mariadb.com/kb/en/like/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'NOT LIKE', 'NOT LIKE', 'https://mariadb.com/kb/en/not-like/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'IN', 'IN', 'https://mariadb.com/kb/en/in/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'NOT IN', 'NOT IN', 'https://mariadb.com/kb/en/not-in/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'IS NOT NULL', 'IS NOT NULL', 'https://mariadb.com/kb/en/is-not-null/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'IS NULL', 'IS NULL', 'https://mariadb.com/kb/en/is-null/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'REGEXP', 'REGEXP', 'https://mariadb.com/kb/en/regexp/'),
       ('d79cb089-363c-488b-9717-649e44d8fcc5', 'NOT REGEXP', 'NOT REGEXP', 'https://mariadb.com/kb/en/not-regexp/');

INSERT
INTO `mdb_ontologies` (prefix, uri, uri_pattern, sparql_endpoint, rdf_path)
VALUES ('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/',
        'http://www.ontology-of-units-of-measure.org/resource/om-2/.*', null, 'rdf/om-2.0.rdf'),
       ('wd', 'http://www.wikidata.org/', 'http://www.wikidata.org/entity/.*', 'https://query.wikidata.org/sparql',
        null),
       ('mo', 'http://purl.org/ontology/mo/', 'http://purl.org/ontology/mo/.*', null, null),
       ('dc', 'http://purl.org/dc/elements/1.1/', null, null, null),
       ('xsd', 'http://www.w3.org/2001/XMLSchema#', null, null, null),
       ('tl', 'http://purl.org/NET/c4dm/timeline.owl#', null, null, null),
       ('foaf', 'http://xmlns.com/foaf/0.1/', null, null, null),
       ('schema', 'http://schema.org/', null, null, null),
       ('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#', null, null, null),
       ('rdfs', 'http://www.w3.org/2000/01/rdf-schema#', null, null, null),
       ('owl', 'http://www.w3.org/2002/07/owl#', null, null, null),
       ('prov', 'http://www.w3.org/ns/prov#', null, null, null),
       ('db', 'http://dbpedia.org', 'http://dbpedia.org/ontology/.*', 'http://dbpedia.org/sparql', null);
COMMIT;