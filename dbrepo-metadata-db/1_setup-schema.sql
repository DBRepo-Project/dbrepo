BEGIN;

CREATE TABLE IF NOT EXISTS `mdb_users`
(
    id               character varying(36)  NOT NULL,
    username         character varying(255) NOT NULL,
    firstname        character varying(255),
    lastname         character varying(255),
    email            character varying(255) NOT NULL,
    orcid            character varying(255),
    affiliation      character varying(255),
    mariadb_password character varying(255) NOT NULL,
    theme            character varying(255) NOT NULL default ('light'),
    language         character varying(3)   NOT NULL default ('en'),
    PRIMARY KEY (id),
    UNIQUE (username),
    UNIQUE (email)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_images`
(
    id            SERIAL,
    registry      character varying(255) NOT NULL DEFAULT 'docker.io',
    name          character varying(255) NOT NULL,
    version       character varying(255) NOT NULL,
    default_port  integer                NOT NULL,
    dialect       character varying(255) NOT NULL,
    driver_class  character varying(255) NOT NULL,
    jdbc_method   character varying(255) NOT NULL,
    is_default    BOOLEAN                NOT NULL DEFAULT FALSE,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (id),
    UNIQUE (name, version),
    UNIQUE (is_default)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_containers`
(
    id                  SERIAL,
    internal_name       character varying(255) NOT NULL,
    name                character varying(255) NOT NULL,
    host                character varying(255) NOT NULL,
    port                integer                NOT NULL default 3306,
    ui_host             character varying(255) NOT NULL default host,
    ui_port             integer                NOT NULL default port,
    ui_additional_flags text,
    sidecar_host        character varying(255),
    sidecar_port        integer,
    image_id            bigint                 NOT NULL,
    created             timestamp              NOT NULL DEFAULT NOW(),
    last_modified       timestamp,
    privileged_username character varying(255) NOT NULL,
    privileged_password character varying(255) NOT NULL,
    quota               integer                NOT NULL DEFAULT 50,
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_data`
(
    ID           SERIAL,
    PROVENANCE   text,
    FileEncoding text,
    FileType     character varying(100),
    Version      text,
    Seperator    text,
    PRIMARY KEY (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_licenses`
(
    identifier  character varying(255) NOT NULL,
    uri         text                   NOT NULL,
    description text                   NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_databases`
(
    id             SERIAL,
    cid            BIGINT UNSIGNED        NOT NULL,
    name           character varying(255) NOT NULL,
    internal_name  character varying(255) NOT NULL,
    exchange_name  character varying(255) NOT NULL,
    description    text,
    engine         character varying(20),
    is_public      boolean                NOT NULL DEFAULT TRUE,
    image          longblob,
    created_by     character varying(36),
    owned_by       character varying(36),
    contact_person character varying(36),
    created        timestamp              NOT NULL DEFAULT NOW(),
    last_modified  timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (cid) REFERENCES mdb_containers (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (id),
    FOREIGN KEY (owned_by) REFERENCES mdb_users (id),
    FOREIGN KEY (contact_person) REFERENCES mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_databases_subjects`
(
    dbid     BIGINT                 NOT NULL,
    subjects character varying(255) NOT NULL,
    PRIMARY KEY (dbid, subjects)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_tables`
(
    ID              SERIAL,
    tDBID           BIGINT UNSIGNED       NOT NULL,
    tName           VARCHAR(64)           NOT NULL,
    internal_name   VARCHAR(64)           NOT NULL,
    queue_name      VARCHAR(255)          NOT NULL,
    routing_key     VARCHAR(255),
    tDescription    VARCHAR(2048),
    num_rows        BIGINT,
    data_length     BIGINT,
    max_data_length BIGINT,
    avg_row_length  BIGINT,
    `separator`     CHAR(1),
    quote           CHAR(1),
    element_null    VARCHAR(50),
    skip_lines      BIGINT,
    element_true    VARCHAR(50),
    element_false   VARCHAR(50),
    Version         TEXT,
    created         timestamp             NOT NULL DEFAULT NOW(),
    versioned       boolean               not null default true,
    created_by      character varying(36) NOT NULL,
    owned_by        character varying(36) NOT NULL,
    last_modified   timestamp,
    PRIMARY KEY (ID),
    UNIQUE (tDBID, internal_name),
    FOREIGN KEY (tDBID) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (id),
    FOREIGN KEY (owned_by) REFERENCES mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns`
(
    ID               SERIAL,
    tID              BIGINT UNSIGNED NOT NULL,
    cName            VARCHAR(64),
    internal_name    VARCHAR(64)     NOT NULL,
    Datatype         ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    length           BIGINT UNSIGNED NULL,
    ordinal_position INTEGER         NOT NULL,
    index_length     BIGINT UNSIGNED NULL,
    description      VARCHAR(2048),
    size             BIGINT UNSIGNED,
    d                BIGINT UNSIGNED,
    auto_generated   BOOLEAN                  DEFAULT false,
    is_null_allowed  BOOLEAN         NOT NULL DEFAULT true,
    val_min          NUMERIC         NULL,
    val_max          NUMERIC         NULL,
    mean             NUMERIC         NULL,
    median           NUMERIC         NULL,
    std_dev          Numeric         NULL,
    created          timestamp       NOT NULL DEFAULT NOW(),
    last_modified    timestamp,
    FOREIGN KEY (tID) REFERENCES mdb_tables (ID) ON DELETE CASCADE,
    PRIMARY KEY (ID),
    UNIQUE (tID, internal_name)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_enums`
(
    id        SERIAL,
    column_id BIGINT UNSIGNED        NOT NULL,
    value     CHARACTER VARYING(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID) ON DELETE CASCADE,
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_sets`
(
    id        SERIAL,
    column_id BIGINT UNSIGNED        NOT NULL,
    value     CHARACTER VARYING(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID) ON DELETE CASCADE,
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_nom`
(
    cID           BIGINT UNSIGNED,
    tID           BIGINT UNSIGNED,
    maxlength     INTEGER,
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (cID),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_cat`
(
    cID           BIGINT UNSIGNED,
    tID           BIGINT UNSIGNED,
    num_cat       INTEGER,
    --    cat_array     TEXT[],
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (cID),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key`
(
    fkid      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tid       BIGINT UNSIGNED NOT NULL,
    rtid      BIGINT UNSIGNED NOT NULL,
    name      VARCHAR(255)    NOT NULL,
    on_update VARCHAR(50)     NULL,
    on_delete VARCHAR(50)     NULL,
    position  INT             NULL,
    PRIMARY KEY (fkid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE,
    FOREIGN KEY (rtid) REFERENCES mdb_tables (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_primary_key`
(
    pkid SERIAL,
    tID  BIGINT UNSIGNED NOT NULL,
    cid  BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (pkid),
    FOREIGN KEY (tID) REFERENCES mdb_tables (id) ON DELETE CASCADE,
    FOREIGN KEY (cid) REFERENCES mdb_columns (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key_reference`
(
    id   SERIAL,
    fkid BIGINT UNSIGNED NOT NULL,
    cid  BIGINT UNSIGNED NOT NULL,
    rcid BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (fkid, cid, rcid),
    FOREIGN KEY (fkid) REFERENCES mdb_constraints_foreign_key (fkid) ON UPDATE CASCADE,
    FOREIGN KEY (cid) REFERENCES mdb_columns (id),
    FOREIGN KEY (rcid) REFERENCES mdb_columns (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique`
(
    uid      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name     VARCHAR(255)    NOT NULL,
    tid      BIGINT UNSIGNED NOT NULL,
    position INT             NULL,
    PRIMARY KEY (uid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique_columns`
(
    id  SERIAL,
    uid BIGINT UNSIGNED NOT NULL,
    cid BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_constraints_unique (uid),
    FOREIGN KEY (cid) REFERENCES mdb_columns (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_checks`
(
    id     SERIAL,
    tid    BIGINT UNSIGNED NOT NULL,
    checks VARCHAR(255)    NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;


CREATE TABLE IF NOT EXISTS `mdb_concepts`
(
    id          SERIAL,
    uri         text         not null,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     timestamp    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_units`
(
    id          SERIAL,
    uri         text         not null,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     timestamp    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_concepts`
(
    id      BIGINT UNSIGNED NOT NULL,
    cID     BIGINT UNSIGNED NOT NULL,
    created timestamp       NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, cid),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_units`
(
    id      BIGINT UNSIGNED NOT NULL,
    cID     BIGINT UNSIGNED NOT NULL,
    created timestamp       NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, cID),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view`
(
    id            SERIAL,
    vdbid         BIGINT UNSIGNED       NOT NULL,
    vName         VARCHAR(64)           NOT NULL,
    internal_name VARCHAR(64)           NOT NULL,
    Query         TEXT                  NOT NULL,
    query_hash    VARCHAR(255)          NOT NULL,
    Public        BOOLEAN               NOT NULL,
    InitialView   BOOLEAN               NOT NULL,
    created       timestamp             NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    created_by    character varying(36) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_banner_messages`
(
    id            SERIAL,
    type          ENUM ('ERROR', 'WARNING', 'INFO') NOT NULL default 'INFO',
    message       TEXT                              NOT NULL,
    link          TEXT                              NULL,
    link_text     VARCHAR(255)                      NULL,
    display_start timestamp                         NULL,
    display_end   timestamp                         NULL,
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_ontologies`
(
    id              SERIAL,
    prefix          VARCHAR(8) NOT NULL,
    uri             TEXT       NOT NULL,
    uri_pattern     TEXT,
    sparql_endpoint TEXT       NULL,
    rdf_path        TEXT       NULL,
    last_modified   timestamp,
    created         timestamp  NOT NULL DEFAULT NOW(),
    UNIQUE (prefix),
    UNIQUE (uri(200)),
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view_columns`
(
    id               SERIAL,
    view_id          BIGINT UNSIGNED NOT NULL,
    dfID             BIGINT UNSIGNED,
    name             VARCHAR(64),
    internal_name    VARCHAR(64)     NOT NULL,
    column_type      ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    ordinal_position INTEGER         NOT NULL,
    size             BIGINT UNSIGNED,
    d                BIGINT UNSIGNED,
    auto_generated   BOOLEAN                  DEFAULT false,
    is_null_allowed  BOOLEAN         NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    FOREIGN KEY (view_id) REFERENCES mdb_view (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifiers`
(
    id                SERIAL,
    dbid              BIGINT UNSIGNED                              NOT NULL,
    qid               BIGINT UNSIGNED,
    vid               BIGINT UNSIGNED,
    tid               BIGINT UNSIGNED,
    publisher         VARCHAR(255)                                 NOT NULL,
    language          VARCHAR(2),
    publication_year  INTEGER                                      NOT NULL,
    publication_month INTEGER,
    publication_day   INTEGER,
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
    created_by        VARCHAR(36)                                  NOT NULL,
    last_modified     TIMESTAMP,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_licenses`
(
    pid        BIGINT UNSIGNED NOT NULL,
    license_id VARCHAR(255)    NOT NULL,
    PRIMARY KEY (pid, license_id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (license_id) REFERENCES mdb_licenses (identifier)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_titles`
(
    id         SERIAL,
    pid        BIGINT UNSIGNED NOT NULL,
    title      text            NOT NULL,
    title_type ENUM ('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER'),
    language   VARCHAR(2),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_funders`
(
    id                     SERIAL,
    pid                    BIGINT UNSIGNED NOT NULL,
    funder_name            VARCHAR(255)    NOT NULL,
    funder_identifier      TEXT,
    funder_identifier_type ENUM ('CROSSREF_FUNDER_ID', 'GRID', 'ISNI', 'ROR', 'OTHER'),
    scheme_uri             text,
    award_number           VARCHAR(255),
    award_title            text,
    language               VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_descriptions`
(
    id               SERIAL,
    pid              BIGINT UNSIGNED NOT NULL,
    description      text            NOT NULL,
    description_type ENUM ('ABSTRACT', 'METHODS', 'SERIES_INFORMATION', 'TABLE_OF_CONTENTS', 'TECHNICAL_INFO', 'OTHER'),
    language         VARCHAR(2),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_related_identifiers`
(
    id       SERIAL,
    pid      BIGINT UNSIGNED NOT NULL,
    value    varchar(255)    NOT NULL,
    type     varchar(255)    NOT NULL,
    relation varchar(255)    NOT NULL,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    UNIQUE (pid, value)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_creators`
(
    id                                SERIAL,
    pid                               BIGINT UNSIGNED NOT NULL,
    given_names                       text,
    family_name                       text,
    creator_name                      VARCHAR(255)    NOT NULL,
    name_type                         ENUM ('PERSONAL', 'ORGANIZATIONAL') default 'PERSONAL',
    name_identifier                   text,
    name_identifier_scheme            ENUM ('ROR', 'GRID', 'ISNI', 'ORCID'),
    name_identifier_scheme_uri        text,
    affiliation                       VARCHAR(255),
    affiliation_identifier            text,
    affiliation_identifier_scheme     ENUM ('ROR', 'GRID', 'ISNI'),
    affiliation_identifier_scheme_uri text,
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_update`
(
    uUserID character varying(255) NOT NULL,
    uDBID   BIGINT UNSIGNED        NOT NULL,
    created timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (uUserID, uDBID),
    FOREIGN KEY (uDBID) REFERENCES mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_access`
(
    aUserID  character varying(255) NOT NULL,
    aDBID    BIGINT UNSIGNED REFERENCES mdb_databases (id),
    attime   TIMESTAMP,
    download BOOLEAN,
    created  timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_have_access`
(
    user_id     character varying(36)                   NOT NULL,
    database_id BIGINT UNSIGNED REFERENCES mdb_databases (id),
    access_type ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL') NOT NULL,
    created     timestamp                               NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, database_id),
    FOREIGN KEY (user_id) REFERENCES mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_types`
(
    id            SERIAL,
    image_id      BIGINT UNSIGNED NOT NULL,
    display_name  varchar(255)    NOT NULL,
    value         varchar(255)    NOT NULL,
    size_min      INT UNSIGNED,
    size_max      INT UNSIGNED,
    size_default  INT UNSIGNED,
    size_required BOOLEAN COMMENT 'When setting NULL, the service assumes the data type has no size',
    size_step     INT UNSIGNED,
    d_min         INT UNSIGNED,
    d_max         INT UNSIGNED,
    d_default     INT UNSIGNED,
    d_required    BOOLEAN COMMENT 'When setting NULL, the service assumes the data type has no d',
    d_step        INT UNSIGNED,
    hint          TEXT,
    documentation TEXT            NOT NULL,
    is_quoted     BOOLEAN         NOT NULL,
    is_buildable  BOOLEAN         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (image_id) REFERENCES `mdb_images` (`id`),
    UNIQUE (value)
) WITH SYSTEM VERSIONING;

COMMIT;

BEGIN;

INSERT INTO `mdb_licenses` (identifier, uri, description)
VALUES ('CC0-1.0', 'https://creativecommons.org/publicdomain/zero/1.0/legalcode',
        'CC0 waives copyright interest in a work you''ve created and dedicates it to the world-wide public domain. Use CC0 to opt out of copyright entirely and ensure your work has the widest reach.'),
       ('CC-BY-4.0', 'https://creativecommons.org/licenses/by/4.0/legalcode',
        'The Creative Commons Attribution license allows re-distribution and re-use of a licensed work on the condition that the creator is appropriately credited.');

INSERT INTO `mdb_images` (name, registry, version, default_port, dialect, driver_class, jdbc_method)
VALUES ('mariadb', 'docker.io', '11.1.3', 3306, 'org.hibernate.dialect.MariaDBDialect', 'org.mariadb.jdbc.Driver',
        'mariadb');

INSERT INTO `mdb_image_types` (image_id, display_name, value, size_min, size_max, size_default, size_required,
                               size_step, d_min, d_max, d_default, d_required, d_step, hint, documentation, is_quoted,
                               is_buildable)
VALUES (1, 'BIGINT(size)', 'bigint', 0, null, null, false, 1, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bigint/', false, true),
       (1, 'BINARY(size)', 'binary', 0, 255, 255, true, 1, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/binary/', false, true),
       (1, 'BIT(size)', 'bit', 0, 64, null, false, 1, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bit/', false, true),
       (1, 'BLOB(size)', 'blob', 0, 65535, null, false, 1, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/blob/', false, false),
       (1, 'BOOL', 'bool', null, null, null, null, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bool/', false, true),
       (1, 'CHAR(size)', 'char', 0, 255, 255, false, 1, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/char/', false, true),
       (1, 'DATE', 'date', null, null, null, null, null, null, null, null, null, null,
        'min. 1000-01-01, max. 9999-12-31', 'https://mariadb.com/kb/en/date/', true, true),
       (1, 'DATETIME(fsp)', 'datetime', 0, 6, null, null, 1, null, null, null, null, null,
        'fsp=microsecond precision, min. 1000-01-01 00:00:00.0, max. 9999-12-31 23:59:59.9',
        'https://mariadb.com/kb/en/datetime/', true, true),
       (1, 'DECIMAL(size, d)', 'decimal', 0, 65, null, false, 1, 0, 38, null, false, null, null,
        'https://mariadb.com/kb/en/decimal/', false, true),
       (1, 'DOUBLE(size, d)', 'double', null, null, null, false, null, null, null, null, false, null, null,
        'https://mariadb.com/kb/en/double/', false, true),
       (1, 'ENUM(v1,v2,...)', 'enum', null, null, null, null, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/enum/', true, true),
       (1, 'FLOAT(size)', 'float', null, null, null, false, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/float/', false, true),
       (1, 'INT(size)', 'int', null, null, null, false, null, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/int/', false, true),
       (1, 'LONGBLOB', 'longblob', null, null, null, null, null, null, null, null, null, null, 'max. 3.999 GiB',
        'https://mariadb.com/kb/en/longblob/', false, true),
       (1, 'LONGTEXT', 'longtext', null, null, null, null, null, null, null, null, null, null, 'max. 3.999 GiB',
        'https://mariadb.com/kb/en/longtext/', true, true),
       (1, 'MEDIUMBLOB', 'mediumblob', null, null, null, null, null, null, null, null, null, null, 'max. 15.999 MiB',
        'https://mariadb.com/kb/en/mediumblob/', false, true),
       (1, 'MEDIUMINT', 'mediumint', null, null, null, null, null, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/mediumint/', false, true),
       (1, 'MEDIUMTEXT', 'mediumtext', null, null, null, null, null, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/mediumtext/', true, true),
       (1, 'SET(v1,v2,...)', 'set', null, null, null, null, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/set/', true, true),
       (1, 'SMALLINT(size)', 'smallint', 0, null, null, false, null, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/smallint/', false, true),
       (1, 'TEXT(size)', 'text', 0, null, null, false, null, null, null, null, null, null, 'size in Bytes',
        'https://mariadb.com/kb/en/text/', true, true),
       (1, 'TIME(fsp)', 'time', 0, 6, 0, false, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/time/', true, true),
       (1, 'TIMESTAMP(fsp)', 'timestamp', 0, 6, 0, false, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/timestamp/', true, true),
       (1, 'TINYBLOB', 'tinyblob', null, null, null, null, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/timestamp/', false, true),
       (1, 'TINYINT(size)', 'tinyint', 0, null, null, false, null, null, null, null, null, null,
        'size in Bytes', 'https://mariadb.com/kb/en/tinyint/', false, true),
       (1, 'TINYTEXT', 'tinytext', null, null, null, null, null, null, null, null, null, null,
        'max. 255 characters', 'https://mariadb.com/kb/en/tinytext/', true, true),
       (1, 'YEAR', 'year', 2, 4, null, false, 2, null, null, null, null, null, 'min. 1901, max. 2155',
        'https://mariadb.com/kb/en/year/', false, true),
       (1, 'VARBINARY(size)', 'varbinary', 0, null, null, true, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/varbinary/', false, true),
       (1, 'VARCHAR(size)', 'varchar', 0, 65532, 255, true, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/varchar/', false, true);

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
