BEGIN;

CREATE TABLE IF NOT EXISTS mdb_images
(
    id            VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    registry      VARCHAR(255) NOT NULL DEFAULT 'docker.io',
    name          VARCHAR(255) NOT NULL,
    version       VARCHAR(255) NOT NULL,
    default_port  INT          NOT NULL,
    dialect       VARCHAR(255) NOT NULL,
    driver_class  VARCHAR(255) NOT NULL,
    jdbc_method   VARCHAR(255) NOT NULL,
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    created       TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (name, version),
    UNIQUE (is_default)
);

CREATE TABLE IF NOT EXISTS mdb_containers
(
    id                  VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    internal_name       VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    host                VARCHAR(255) NOT NULL,
    port                INT          NOT NULL DEFAULT 3306,
    ui_host             VARCHAR(255) NOT NULL,
    ui_port             INT          NOT NULL,
    ui_additional_flags TEXT,
    image_id            VARCHAR(36)  NOT NULL,
    created             TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified       TIMESTAMP,
    privileged_username VARCHAR(255) NOT NULL,
    privileged_password VARCHAR(255) NOT NULL,
    quota               INT,
    readonly_username   VARCHAR(255) NOT NULL,
    readonly_password   VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id)
);

CREATE TABLE IF NOT EXISTS mdb_licenses
(
    identifier  VARCHAR(255) NOT NULL,
    uri         TEXT         NOT NULL,
    description TEXT         NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (uri)
);

CREATE TABLE IF NOT EXISTS mdb_databases
(
    id                    VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    cid                   VARCHAR(36)  NOT NULL,
    grafana_dashboard_uid character varying(255),
    name                  VARCHAR(255) NOT NULL,
    internal_name         VARCHAR(255) NOT NULL,
    exchange_name         VARCHAR(255) NOT NULL,
    description           TEXT,
    engine                VARCHAR(20),
    is_public             BOOLEAN      NOT NULL DEFAULT TRUE,
    is_schema_public      BOOLEAN      NOT NULL DEFAULT TRUE,
    is_dashboard_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    image                 bytea,
    owned_by              VARCHAR(255) NOT NULL,
    contact_person        VARCHAR(255) NOT NULL,
    created               TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_modified         TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (cid) REFERENCES mdb_containers (id)
);

CREATE TABLE IF NOT EXISTS mdb_tables
(
    id               VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
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
    owned_by         VARCHAR(255) NOT NULL,
    last_modified    TIMESTAMP,
    PRIMARY KEY (ID),
    UNIQUE (tDBID, internal_name),
    FOREIGN KEY (tDBID) REFERENCES mdb_databases (id)
);

CREATE TYPE data_type AS ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR','SERIAL');

CREATE TABLE IF NOT EXISTS mdb_columns
(
    id               VARCHAR(36)    NOT NULL DEFAULT gen_random_uuid(),
    tID              VARCHAR(36)    NOT NULL,
    cName            VARCHAR(64),
    internal_name    VARCHAR(64)    NOT NULL,
    Datatype         data_type      NOT NULL,
    length           BIGINT         NULL,
    ordinal_position INT            NOT NULL,
    index_length     BIGINT         NULL,
    description      VARCHAR(2048),
    concept_uri      TEXT,
    unit_uri         TEXT,
    size             BIGINT,
    d                BIGINT,
    is_null_allowed  BOOLEAN        NOT NULL DEFAULT TRUE,
    val_min          DECIMAL(65, 4) NULL,
    val_max          DECIMAL(65, 4) NULL,
    mean             DECIMAL(65, 4) NULL,
    median           DECIMAL(65, 4) NULL,
    std_dev          DECIMAL(65, 4) NULL,
    created          TIMESTAMP      NOT NULL DEFAULT NOW(),
    last_modified    TIMESTAMP,
    FOREIGN KEY (tID) REFERENCES mdb_tables (ID) ON DELETE CASCADE,
    PRIMARY KEY (ID),
    UNIQUE (tID, internal_name)
);

CREATE TABLE IF NOT EXISTS mdb_columns_enums
(
    id        VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    column_id VARCHAR(36)  NOT NULL,
    value     VARCHAR(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS mdb_columns_sets
(
    id        VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    column_id VARCHAR(36)  NOT NULL,
    value     VARCHAR(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS mdb_constraints_foreign_key
(
    fkid      VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    tid       VARCHAR(36)  NOT NULL,
    rtid      VARCHAR(36)  NOT NULL,
    name      VARCHAR(255) NOT NULL,
    on_update VARCHAR(50)  NULL,
    on_delete VARCHAR(50)  NULL,
    position  INT          NULL,
    PRIMARY KEY (fkid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE,
    FOREIGN KEY (rtid) REFERENCES mdb_tables (id)
);

CREATE TABLE IF NOT EXISTS mdb_constraints_primary_key
(
    pkid VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    tID  VARCHAR(36) NOT NULL,
    cid  VARCHAR(36) NOT NULL,
    PRIMARY KEY (pkid),
    FOREIGN KEY (tID) REFERENCES mdb_tables (id) ON DELETE CASCADE,
    FOREIGN KEY (cid) REFERENCES mdb_columns (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mdb_constraints_foreign_key_reference
(
    id   VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    fkid VARCHAR(36) NOT NULL,
    cid  VARCHAR(36) NOT NULL,
    rcid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (fkid, cid, rcid),
    FOREIGN KEY (fkid) REFERENCES mdb_constraints_foreign_key (fkid) ON UPDATE CASCADE,
    FOREIGN KEY (cid) REFERENCES mdb_columns (id),
    FOREIGN KEY (rcid) REFERENCES mdb_columns (id)
);

CREATE TABLE IF NOT EXISTS mdb_constraints_unique
(
    uid      VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    name     VARCHAR(255) NOT NULL,
    tid      VARCHAR(36)  NOT NULL,
    position INT          NULL,
    PRIMARY KEY (uid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mdb_constraints_unique_columns
(
    id  VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    uid VARCHAR(36) NOT NULL,
    cid VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_constraints_unique (uid),
    FOREIGN KEY (cid) REFERENCES mdb_columns (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mdb_constraints_checks
(
    id     VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    tid    VARCHAR(36)  NOT NULL,
    checks VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mdb_view
(
    id               VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
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
    owned_by         VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (vdbid, internal_name),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id)
);

CREATE TYPE message_type AS ENUM ('ERROR', 'WARNING', 'INFO');

CREATE TABLE IF NOT EXISTS mdb_messages
(
    id            VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    type          message_type NOT NULL DEFAULT 'INFO',
    message       TEXT         NOT NULL,
    link          TEXT         NULL,
    link_text     VARCHAR(255) NULL,
    display_start TIMESTAMP    NULL,
    display_end   TIMESTAMP    NULL,
    PRIMARY KEY (id)
);

CREATE TYPE column_type AS ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR','SERIAL');

CREATE TABLE IF NOT EXISTS mdb_view_columns
(
    id               VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    view_id          VARCHAR(36) NOT NULL,
    name             VARCHAR(64),
    internal_name    VARCHAR(64) NOT NULL,
    column_type      column_type NOT NULL,
    ordinal_position INT         NOT NULL,
    size             BIGINT,
    d                BIGINT,
    is_null_allowed  BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    FOREIGN KEY (view_id) REFERENCES mdb_view (id) ON DELETE CASCADE,
    UNIQUE (view_id, internal_name)
);

CREATE TYPE identifier_type AS ENUM ('DATABASE', 'SUBSET', 'VIEW', 'TABLE');
CREATE TYPE identifier_status AS ENUM ('DRAFT', 'PUBLISHED');

CREATE TABLE IF NOT EXISTS mdb_identifiers
(
    id                VARCHAR(36)       NOT NULL DEFAULT gen_random_uuid(),
    dbid              VARCHAR(36)       NOT NULL,
    qid               VARCHAR(36),
    vid               VARCHAR(36),
    tid               VARCHAR(36),
    publisher         VARCHAR(255)      NOT NULL,
    language          VARCHAR(2),
    publication_year  INT               NOT NULL,
    publication_month INT,
    publication_day   INT,
    identifier_type   identifier_type   NOT NULL,
    status            identifier_status NOT NULL DEFAULT ('PUBLISHED'),
    query             TEXT,
    query_normalized  TEXT,
    query_hash        VARCHAR(255),
    execution         TIMESTAMP,
    result_hash       VARCHAR(255),
    result_number     BIGINT,
    doi               VARCHAR(255),
    created           TIMESTAMP         NOT NULL DEFAULT NOW(),
    owned_by          VARCHAR(255)      NOT NULL,
    last_modified     TIMESTAMP,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id),
    FOREIGN KEY (vid) REFERENCES mdb_view (id)
);

CREATE TABLE IF NOT EXISTS mdb_identifier_licenses
(
    pid        VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    license_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (pid, license_id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (license_id) REFERENCES mdb_licenses (identifier)
);

CREATE TYPE title_type AS ENUM ('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER');

CREATE TABLE IF NOT EXISTS mdb_identifier_titles
(
    id               VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    pid              VARCHAR(36) NOT NULL,
    title            TEXT        NOT NULL,
    title_type       title_type,
    ordinal_position INT         NOT NULL,
    language         VARCHAR(2),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TYPE funder_identifier_type AS ENUM ('CROSSREF_FUNDER_ID', 'GRID', 'ISNI', 'ROR', 'OTHER');

CREATE TABLE IF NOT EXISTS mdb_identifier_funders
(
    id                     VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    pid                    VARCHAR(36)  NOT NULL,
    funder_name            VARCHAR(255) NOT NULL,
    funder_identifier      TEXT,
    funder_identifier_type funder_identifier_type,
    scheme_uri             TEXT,
    award_number           VARCHAR(255),
    award_title            TEXT,
    language               VARCHAR(255),
    ordinal_position       INT          NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TYPE description_type AS ENUM ('ABSTRACT', 'METHODS', 'SERIES_INFORMATION', 'TABLE_OF_CONTENTS', 'TECHNICAL_INFO', 'OTHER');

CREATE TABLE IF NOT EXISTS mdb_identifier_descriptions
(
    id               VARCHAR(36) NOT NULL DEFAULT gen_random_uuid(),
    pid              VARCHAR(36) NOT NULL,
    description      TEXT        NOT NULL,
    description_type description_type,
    language         VARCHAR(2),
    ordinal_position INT         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TYPE identifier_related_type AS ENUM ('DOI', 'URL', 'URN', 'ARK', 'ARXIV', 'BIBCODE', 'EAN13', 'EISSN', 'HANDLE', 'IGSN', 'ISBN', 'ISTC', 'LISSN', 'LSID', 'PMID', 'PURL', 'UPC', 'W3ID');
CREATE TYPE identifier_related_relation AS ENUM ('IS_CITED_BY', 'CITES', 'IS_SUPPLEMENT_TO', 'IS_SUPPLEMENTED_BY', 'IS_CONTINUED_BY', 'CONTINUES', 'IS_DESCRIBED_BY', 'DESCRIBES', 'HAS_METADATA', 'IS_METADATA_FOR', 'HAS_VERSION', 'IS_VERSION_OF', 'IS_NEW_VERSION_OF', 'IS_PREVIOUS_VERSION_OF', 'IS_PART_OF', 'HAS_PART', 'IS_PUBLISHED_IN', 'IS_REFERENCED_BY', 'REFERENCES', 'IS_DOCUMENTED_BY', 'DOCUMENTS', 'IS_COMPILED_BY', 'COMPILES', 'IS_VARIANT_FORM_OF', 'IS_ORIGINAL_FORM_OF', 'IS_IDENTICAL_TO', 'IS_REVIEWED_BY', 'REVIEWS', 'IS_DERIVED_FROM', 'IS_SOURCE_OF', 'IS_REQUIRED_BY', 'REQUIRES', 'IS_OBSOLETED_BY', 'OBSOLETES');

CREATE TABLE IF NOT EXISTS mdb_identifier_related
(
    id               VARCHAR(36)                 NOT NULL DEFAULT gen_random_uuid(),
    pid              VARCHAR(36)                 NOT NULL DEFAULT gen_random_uuid(),
    value            VARCHAR(255)                NOT NULL,
    type             identifier_related_type     NOT NULL,
    relation         identifier_related_relation NOT NULL,
    ordinal_position INT                         NOT NULL,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TYPE name_type AS ENUM ('PERSONAL', 'ORGANIZATIONAL');
CREATE TYPE scheme_type AS ENUM ('ROR', 'GRID', 'ISNI', 'ORCID');
CREATE TYPE affiliation_scheme_type AS ENUM ('ROR', 'GRID', 'ISNI');

CREATE TABLE IF NOT EXISTS mdb_identifier_creators
(
    id                                VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    pid                               VARCHAR(36)  NOT NULL,
    ordinal_position                  INT          NOT NULL,
    given_names                       TEXT,
    family_name                       TEXT,
    creator_name                      VARCHAR(255) NOT NULL,
    name_type                         name_type             DEFAULT 'PERSONAL',
    name_identifier                   TEXT,
    name_identifier_scheme            scheme_type,
    name_identifier_scheme_uri        TEXT,
    affiliation                       VARCHAR(255),
    affiliation_identifier            TEXT,
    affiliation_identifier_scheme     affiliation_scheme_type,
    affiliation_identifier_scheme_uri TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TABLE IF NOT EXISTS mdb_access
(
    username VARCHAR(255) NOT NULL,
    aDBID    VARCHAR(36)  NOT NULL,
    attime   TIMESTAMP,
    download BOOLEAN,
    created  TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (username, aDBID),
    FOREIGN KEY (aDBID) REFERENCES mdb_databases (id)
);

CREATE TYPE access_type AS ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL');

CREATE TABLE IF NOT EXISTS mdb_have_access
(
    username    VARCHAR(255) NOT NULL,
    database_id VARCHAR(36)  NOT NULL,
    access_type access_type  NOT NULL,
    created     TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (username, database_id),
    FOREIGN KEY (database_id) REFERENCES mdb_databases (id)
);

CREATE TABLE IF NOT EXISTS mdb_image_types
(
    id            VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    image_id      VARCHAR(36)  NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    value         VARCHAR(255) NOT NULL,
    size_min      INT,
    size_max      INT,
    size_default  INT,
    size_required BOOLEAN,
    size_step     INT,
    d_min         INT,
    d_max         INT,
    d_default     INT,
    d_required    BOOLEAN,
    d_step        INT,
    type_hint     TEXT,
    data_hint     TEXT,
    documentation TEXT         NOT NULL,
    is_generated  BOOLEAN      NOT NULL,
    is_quoted     BOOLEAN      NOT NULL,
    is_buildable  BOOLEAN      NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id),
    UNIQUE (value)
);

COMMENT ON COLUMN mdb_image_types.size_required IS 'When setting NULL, the service assumes the data type has no size';
COMMENT ON COLUMN mdb_image_types.d_required IS 'When setting NULL, the service assumes the data type has no d';

CREATE TABLE IF NOT EXISTS mdb_image_operators
(
    id            VARCHAR(36)  NOT NULL DEFAULT gen_random_uuid(),
    image_id      VARCHAR(36)  NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    value         VARCHAR(255) NOT NULL,
    documentation TEXT         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id),
    UNIQUE (image_id, value)
);

COMMIT;

BEGIN;

INSERT INTO mdb_licenses (identifier, uri, description)
VALUES ('CC0-1.0', 'https://creativecommons.org/publicdomain/zero/1.0/legalcode',
        'CC0 waives copyright interest in a work you''ve created and dedicates it to the world-wide public domain. Use CC0 to opt out of copyright entirely and ensure your work has the widest reach.'),
       ('CC-BY-4.0', 'https://creativecommons.org/licenses/by/4.0/legalcode',
        'The Creative Commons Attribution license allows re-distribution and re-use of a licensed work on the condition that the creator is appropriately credited.');

INSERT INTO mdb_images (id, name, registry, version, default_port, dialect, driver_class, jdbc_method)
VALUES ('32c13903-651a-404c-8fd3-f92708899a69', 'postgres', 'docker.io', '18-alpine', 5432,
        'org.hibernate.dialect.PostgreSQLDialect', 'org.postgresql.Driver', 'postgresql');

INSERT INTO mdb_image_types (image_id, display_name, value, size_min, size_max, size_default, size_required,
                             size_step, d_min, d_max, d_default, d_required, d_step, type_hint, data_hint,
                             documentation, is_quoted, is_buildable, is_generated)
VALUES ('32c13903-651a-404c-8fd3-f92708899a69', 'BIGINT(size)', 'bigint', 0, null, null, FALSE, 1, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/bigint/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'BINARY(size)', 'binary', 0, 255, 255, TRUE, 1, null, null, null, null,
        null, 'size in Bytes', null, 'https://mariadb.com/kb/en/binary/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'BIT(size)', 'bit', 0, 64, null, FALSE, 1, null, null, null, null, null,
        null, null, 'https://mariadb.com/kb/en/bit/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'BLOB(size)', 'blob', 0, 65535, null, FALSE, 1, null, null, null, null,
        null, 'size in Bytes', null, 'https://mariadb.com/kb/en/blob/', FALSE, FALSE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'BOOL', 'bool', null, null, null, null, null, null, null, null, null,
        null, null, null, 'https://mariadb.com/kb/en/bool/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'CHAR(size)', 'char', 0, 255, 255, FALSE, 1, null, null, null, null,
        null, null, null, 'https://mariadb.com/kb/en/char/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'DATE', 'date', null, null, null, null, null, null, null, null, null,
        null, 'min. 1000-01-01, max. 9999-12-31', 'e.g. YYYY-MM-DD, YY-MM-DD, YYMMDD, YYYY/MM/DD',
        'https://mariadb.com/kb/en/date/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'DATETIME(fsp)', 'datetime', 0, 6, null, null, 1, null, null, null,
        null, null, 'fsp=microsecond precision, min. 1000-01-01 00:00:00.0, max. 9999-12-31 23:59:59.9',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/datetime/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'DECIMAL(size, d)', 'decimal', 0, 65, 10, FALSE, 1, 0, 38, 4,
        FALSE, null, null, null, 'https://mariadb.com/kb/en/decimal/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'DOUBLE(size, d)', 'double', null, null, 10, FALSE, null, null, null,
        4, FALSE, null, null, null, 'https://mariadb.com/kb/en/double/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'ENUM(v1,v2,...)', 'enum', null, null, null, null, null, null, null,
        null, null, null, null, 'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/enum/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'FLOAT(size)', 'float', null, null, null, FALSE, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/float/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'INT(size)', 'int', null, null, null, FALSE, null, null, null, null,
        null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/int/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'LONGBLOB', 'longblob', null, null, null, null, null, null, null, null,
        null, null, 'max. 3.999 GiB', null, 'https://mariadb.com/kb/en/longblob/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'LONGTEXT', 'longtext', null, null, null, null, null, null, null, null,
        null, null, 'max. 3.999 GiB', null, 'https://mariadb.com/kb/en/longtext/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'MEDIUMBLOB', 'mediumblob', null, null, null, null, null, null, null,
        null, null, null, 'max. 15.999 MiB', null, 'https://mariadb.com/kb/en/mediumblob/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'MEDIUMINT', 'mediumint', null, null, null, null, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/mediumint/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'MEDIUMTEXT', 'mediumtext', null, null, null, null, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/mediumtext/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'SERIAL', 'serial', null, null, null, null, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/bigint/', TRUE, TRUE, TRUE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'SET(v1,v2,...)', 'set', null, null, null, null, null, null, null, null,
        null, null, null, 'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/set/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'SMALLINT(size)', 'smallint', 0, null, null, FALSE, null, null, null,
        null, null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/smallint/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TEXT(size)', 'text', 0, null, null, FALSE, null, null, null, null,
        null, null, 'size in Bytes', null, 'https://mariadb.com/kb/en/text/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TIME(fsp)', 'time', 0, 6, 0, FALSE, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'e.g. HH:MM:SS, HH:MM, HHMMSS, H:M:S',
        'https://mariadb.com/kb/en/time/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TIMESTAMP(fsp)', 'timestamp', 0, 6, 0, FALSE, null, null, null, null,
        null, null, 'fsp=microsecond precision, min. 0, max. 6',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/timestamp/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TINYBLOB', 'tinyblob', null, null, null, null, null, null, null, null,
        null, null, null, 'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/timestamp/', FALSE,
        TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TINYINT(size)', 'tinyint', 0, null, null, FALSE, null, null, null,
        null, null, null, null, 'size in Bytes', 'https://mariadb.com/kb/en/tinyint/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'TINYTEXT', 'tinytext', null, null, null, null, null, null, null, null,
        null, null, null, 'max. 255 characters', 'https://mariadb.com/kb/en/tinytext/', TRUE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'YEAR', 'year', 2, 4, null, FALSE, 2, null, null, null, null, null,
        'min. 1901, max. 2155', 'e.g. YYYY, YY', 'https://mariadb.com/kb/en/year/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'VARBINARY(size)', 'varbinary', 0, null, null, TRUE, null, null, null,
        null, null, null, null, null, 'https://mariadb.com/kb/en/varbinary/', FALSE, TRUE, FALSE),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'VARCHAR(size)', 'varchar', 0, 65532, 255, TRUE, null, null, null, null,
        null, null, null, null, 'https://mariadb.com/kb/en/varchar/', FALSE, TRUE, FALSE);


INSERT INTO mdb_image_operators (image_id, display_name, value, documentation)
VALUES ('32c13903-651a-404c-8fd3-f92708899a69', 'Equal operator', '=',
        'https://mariadb.com/kb/en/assignment-operators-assignment-operator/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'NULL-safe equal operator', '<=>',
        'https://mariadb.com/kb/en/null-safe-equal/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'Less-than operator', '<', 'https://mariadb.com/kb/en/less-than/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'Less than or equal operator', '<=',
        'https://mariadb.com/kb/en/less-than-or-equal/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'Greater-than operator', '>',
        'https://mariadb.com/kb/en/greater-than/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'Greater than or equal operator', '>=',
        'https://mariadb.com/kb/en/greater-than-or-equal/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'Not equal operator', '!=', 'https://mariadb.com/kb/en/not-equal/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'LIKE', 'LIKE', 'https://mariadb.com/kb/en/like/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'NOT LIKE', 'NOT LIKE', 'https://mariadb.com/kb/en/not-like/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'IN', 'IN', 'https://mariadb.com/kb/en/in/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'NOT IN', 'NOT IN', 'https://mariadb.com/kb/en/not-in/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'IS NOT NULL', 'IS NOT NULL', 'https://mariadb.com/kb/en/is-not-null/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'IS NULL', 'IS NULL', 'https://mariadb.com/kb/en/is-null/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'REGEXP', 'REGEXP', 'https://mariadb.com/kb/en/regexp/'),
       ('32c13903-651a-404c-8fd3-f92708899a69', 'NOT REGEXP', 'NOT REGEXP', 'https://mariadb.com/kb/en/not-regexp/');
COMMIT;