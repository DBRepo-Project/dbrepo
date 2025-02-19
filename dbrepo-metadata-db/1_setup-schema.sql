BEGIN;

CREATE TABLE IF NOT EXISTS `mdb_users`
(
    id               varchar(36)  not null,
    keycloak_id      varchar(36)  not null,
    username         varchar(255) not null,
    firstname        varchar(255),
    lastname         varchar(255),
    orcid            varchar(255),
    affiliation      varchar(255),
    is_internal      boolean      not null default false,
    mariadb_password varchar(255) not null,
    theme            varchar(255) not null default ('light'),
    language         varchar(3)   not null default ('en'),
    primary key (id),
    unique (keycloak_id),
    unique (username)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_images`
(
    id            varchar(36)  not null,
    registry      varchar(255) not null default 'docker.io',
    name          varchar(255) not null,
    version       varchar(255) not null,
    default_port  int          not null,
    dialect       varchar(255) not null,
    driver_class  varchar(255) not null,
    jdbc_method   varchar(255) not null,
    is_default    boolean      not null default false,
    created       timestamp    not null default now(),
    last_modified timestamp,
    primary key (id),
    unique (name, version),
    unique (is_default)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_containers`
(
    id                  varchar(36)  not null,
    internal_name       varchar(255) not null,
    name                varchar(255) not null,
    host                varchar(255) not null,
    port                int          not null default 3306,
    ui_host             varchar(255) not null default host,
    ui_port             int          not null default port,
    ui_additional_flags text,
    sidecar_host        varchar(255),
    sidecar_port        int,
    image_id            bigint       not null,
    created             timestamp    not null default now(),
    last_modified       timestamp,
    privileged_username varchar(255) not null,
    privileged_password varchar(255) not null,
    quota               int          not null default 50,
    primary key (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_data`
(
    id           varchar(36) not null,
    PROVENANCE   text,
    FileEncoding text,
    FileType     varchar(100),
    Version      text,
    Seperator    text,
    primary key (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_licenses`
(
    identifier  varchar(255) not null,
    uri         text         not null,
    description text         not null,
    primary key (identifier),
    unique (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_databases`
(
    id               varchar(36)     not null,
    cid              bigint unsigned not null,
    name             varchar(255)    not null,
    internal_name    varchar(255)    not null,
    exchange_name    varchar(255)    not null,
    description      text,
    engine           varchar(20),
    is_public        boolean         not null default true,
    is_schema_public boolean         not null default true,
    image            longblob,
    owned_by         varchar(36),
    contact_person   varchar(36),
    created          timestamp       not null default now(),
    last_modified    timestamp,
    primary key (id),
    foreign key (cid) references mdb_containers (id),
    foreign key (owned_by) references mdb_users (id),
    foreign key (contact_person) references mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_databases_subjects`
(
    dbid     bigint       not null,
    subjects varchar(255) not null,
    primary key (dbid, subjects)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_tables`
(
    id               varchar(36)     not null,
    tDBID            bigint unsigned not null,
    tName            varchar(64)     not null,
    internal_name    varchar(64)     not null,
    queue_name       varchar(255)    not null,
    routing_key      varchar(255),
    tDescription     varchar(2048),
    num_rows         bigint,
    data_length      bigint,
    max_data_length  bigint,
    avg_row_length   bigint,
    `separator`      char(1),
    quote            char(1),
    element_null     varchar(50),
    skip_lines       bigint,
    element_true     varchar(50),
    element_false    varchar(50),
    Version          text,
    created          timestamp       not null default now(),
    versioned        boolean         not null default true,
    is_public        boolean         not null default true,
    is_schema_public boolean         not null default true,
    owned_by         varchar(36)     not null,
    last_modified    timestamp,
    primary key (ID),
    unique (tDBID, internal_name),
    foreign key (tDBID) references mdb_databases (id),
    foreign key (owned_by) references mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns`
(
    id               varchar(36)     not null,
    tID              bigint unsigned not null,
    cName            varchar(64),
    internal_name    varchar(64)     not null,
    Datatype         enum ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','SERIAL','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    length           bigint unsigned NULL,
    ordinal_position int             not null,
    index_length     bigint unsigned NULL,
    description      varchar(2048),
    size             bigint unsigned,
    d                bigint unsigned,
    is_null_allowed  boolean         not null default true,
    val_min          NUMERIC         NULL,
    val_max          NUMERIC         NULL,
    mean             NUMERIC         NULL,
    median           NUMERIC         NULL,
    std_dev          Numeric         NULL,
    created          timestamp       not null default now(),
    last_modified    timestamp,
    foreign key (tID) references mdb_tables (ID) ON DELETE CASCADE,
    primary key (ID),
    unique (tID, internal_name)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_enums`
(
    id        varchar(36)     not null,
    column_id bigint unsigned not null,
    value     varchar(255)    not null,
    foreign key (column_id) references mdb_columns (ID) ON DELETE CASCADE,
    primary key (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_sets`
(
    id        varchar(36)     not null,
    column_id bigint unsigned not null,
    value     varchar(255)    not null,
    foreign key (column_id) references mdb_columns (ID) ON DELETE CASCADE,
    primary key (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_nom`
(
    cID           bigint unsigned,
    tID           bigint unsigned,
    maxlength     int,
    last_modified timestamp,
    created       timestamp not null default now(),
    primary key (cID),
    foreign key (cID) references mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_cat`
(
    cID           bigint unsigned,
    tID           bigint unsigned,
    num_cat       int,
    --    cat_array     TEXT[],
    last_modified timestamp,
    created       timestamp not null default now(),
    primary key (cID),
    foreign key (cID) references mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key`
(
    fkid      varchar(36)     not null,
    tid       bigint unsigned not null,
    rtid      bigint unsigned not null,
    name      varchar(255)    not null,
    on_update varchar(50)     NULL,
    on_delete varchar(50)     NULL,
    position  int             NULL,
    primary key (fkid),
    foreign key (tid) references mdb_tables (id) ON DELETE CASCADE,
    foreign key (rtid) references mdb_tables (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_primary_key`
(
    pkid varchar(36)     not null,
    tID  bigint unsigned not null,
    cid  bigint unsigned not null,
    primary key (pkid),
    foreign key (tID) references mdb_tables (id) ON DELETE CASCADE,
    foreign key (cid) references mdb_columns (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_foreign_key_reference`
(
    id   varchar(36)     not null,
    fkid bigint unsigned not null,
    cid  bigint unsigned not null,
    rcid bigint unsigned not null,
    primary key (id),
    unique (fkid, cid, rcid),
    foreign key (fkid) references mdb_constraints_foreign_key (fkid) ON UPDATE CASCADE,
    foreign key (cid) references mdb_columns (id),
    foreign key (rcid) references mdb_columns (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique`
(
    uid      varchar(36)     not null,
    name     varchar(255)    not null,
    tid      bigint unsigned not null,
    position int             NULL,
    primary key (uid),
    foreign key (tid) references mdb_tables (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `mdb_constraints_unique_columns`
(
    id  varchar(36)     not null,
    uid bigint unsigned not null,
    cid bigint unsigned not null,
    primary key (id),
    foreign key (uid) references mdb_constraints_unique (uid),
    foreign key (cid) references mdb_columns (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_constraints_checks`
(
    id     varchar(36)     not null,
    tid    bigint unsigned not null,
    checks varchar(255)    not null,
    primary key (id),
    foreign key (tid) references mdb_tables (id) ON DELETE CASCADE
) WITH SYSTEM VERSIONING;


CREATE TABLE IF NOT EXISTS `mdb_concepts`
(
    id          varchar(36)  not null,
    uri         text         not null,
    name        varchar(255) null,
    description text         null,
    created     timestamp    not null default now(),
    primary key (id),
    unique (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_units`
(
    id          varchar(36)  not null,
    uri         text         not null,
    name        varchar(255) null,
    description text         null,
    created     timestamp    not null default now(),
    primary key (id),
    unique (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_concepts`
(
    id      bigint unsigned not null,
    cID     bigint unsigned not null,
    created timestamp       not null default now(),
    primary key (id, cid),
    foreign key (cID) references mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_columns_units`
(
    id      bigint unsigned not null,
    cID     bigint unsigned not null,
    created timestamp       not null default now(),
    primary key (id, cID),
    foreign key (cID) references mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view`
(
    id               varchar(36)     not null,
    vdbid            bigint unsigned not null,
    vName            varchar(64)     not null,
    internal_name    varchar(64)     not null,
    Query            text            not null,
    query_hash       varchar(255)    not null,
    Public           boolean         not null default true,
    is_schema_public boolean         not null default true,
    InitialView      boolean         not null,
    created          timestamp       not null default now(),
    last_modified    timestamp,
    owned_by         varchar(36)     not null,
    primary key (id),
    foreign key (vdbid) references mdb_databases (id),
    foreign key (owned_by) references mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_messages`
(
    id            varchar(36)                       not null,
    type          enum ('ERROR', 'WARNING', 'INFO') not null default 'INFO',
    message       text                              not null,
    link          text                              NULL,
    link_text     varchar(255)                      NULL,
    display_start timestamp                         NULL,
    display_end   timestamp                         NULL,
    primary key (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_ontologies`
(
    id              varchar(36) not null,
    prefix          varchar(8)  not null,
    uri             text        not null,
    uri_pattern     text,
    sparql_endpoint text        NULL,
    rdf_path        text        NULL,
    last_modified   timestamp,
    created         timestamp   not null default now(),
    unique (prefix),
    unique (uri(200)),
    primary key (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_view_columns`
(
    id               varchar(36)     not null,
    view_id          bigint unsigned not null,
    name             varchar(64),
    internal_name    varchar(64)     not null,
    column_type      enum ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    ordinal_position int             not null,
    size             bigint unsigned,
    d                bigint unsigned,
    is_null_allowed  boolean         not null default true,
    primary key (id),
    foreign key (view_id) references mdb_view (id) ON DELETE CASCADE,
    unique (view_id, internal_name)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifiers`
(
    id                varchar(36)                                  not null,
    dbid              bigint unsigned                              not null,
    qid               bigint unsigned,
    vid               bigint unsigned,
    tid               bigint unsigned,
    publisher         varchar(255)                                 not null,
    language          varchar(2),
    publication_year  int                                          not null,
    publication_month int,
    publication_day   int,
    identifier_type   enum ('DATABASE', 'SUBSET', 'VIEW', 'TABLE') not null,
    status            enum ('DRAFT', 'PUBLISHED')                  not null default ('PUBLISHED'),
    query             text,
    query_normalized  text,
    query_hash        varchar(255),
    execution         timestamp,
    result_hash       varchar(255),
    result_number     bigint,
    doi               varchar(255),
    created           timestamp                                    not null default now(),
    owned_by          varchar(36)                                  not null,
    last_modified     timestamp,
    primary key (id), /* must be a single id from persistent identifier concept */
    foreign key (dbid) references mdb_databases (id),
    foreign key (owned_by) references mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_licenses`
(
    pid        bigint unsigned not null,
    license_id varchar(255)    not null,
    primary key (pid, license_id),
    foreign key (pid) references mdb_identifiers (id),
    foreign key (license_id) references mdb_licenses (identifier)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_titles`
(
    id         varchar(36)     not null,
    pid        bigint unsigned not null,
    title      text            not null,
    title_type enum ('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER'),
    language   varchar(2),
    primary key (id),
    foreign key (pid) references mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_funders`
(
    id                     varchar(36)     not null,
    pid                    bigint unsigned not null,
    funder_name            varchar(255)    not null,
    funder_identifier      text,
    funder_identifier_type enum ('CROSSREF_FUNDER_ID', 'GRID', 'ISNI', 'ROR', 'OTHER'),
    scheme_uri             text,
    award_number           varchar(255),
    award_title            text,
    language               varchar(255),
    primary key (id),
    foreign key (pid) references mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_descriptions`
(
    id               varchar(36)     not null,
    pid              bigint unsigned not null,
    description      text            not null,
    description_type enum ('ABSTRACT', 'METHODS', 'SERIES_INFORMATION', 'TABLE_OF_CONTENTS', 'TECHNICAL_INFO', 'OTHER'),
    language         varchar(2),
    primary key (id),
    foreign key (pid) references mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_related`
(
    id       varchar(36)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          not null,
    pid      bigint unsigned                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      not null,
    value    varchar(255)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         not null,
    type     enum ('DOI','URL','URN','ARK','ARXIV','BIBCODE','EAN13','EISSN','HANDLE','IGSN','ISBN','ISTC','LISSN','LSID','PMID','PURL','UPC','W3ID')                                                                                                                                                                                                                                                                                                                                                                                                                             not null,
    relation enum ('IS_CITED_BY','CITES','IS_SUPPLEMENT_TO','IS_SUPPLEMENTED_BY','IS_CONTINUED_BY','CONTINUES','IS_DESCRIBED_BY','DESCRIBES','HAS_METADATA','IS_METADATA_FOR','HAS_VERSION','IS_VERSION_OF','IS_NEW_VERSION_OF','IS_PREVIOUS_VERSION_OF','IS_PART_OF','HAS_PART','IS_PUBLISHED_IN','IS_REFERENCED_BY','references','IS_DOCUMENTED_BY','DOCUMENTS','IS_COMPILED_BY','COMPILES','IS_VARIANT_FORM_OF','IS_ORIGINAL_FORM_OF','IS_IDENTICAL_TO','IS_REVIEWED_BY','REVIEWS','IS_DERIVED_FROM','IS_SOURCE_OF','IS_REQUIRED_BY','REQUIRES','IS_OBSOLETED_BY','OBSOLETES') not null,
    primary key (id), /* must be a single id from persistent identifier concept */
    foreign key (pid) references mdb_identifiers (id),
    unique (pid, value)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_identifier_creators`
(
    id                                varchar(36)     not null,
    pid                               bigint unsigned not null,
    given_names                       text,
    family_name                       text,
    creator_name                      varchar(255)    not null,
    name_type                         enum ('PERSONAL', 'ORGANIZATIONAL') default 'PERSONAL',
    name_identifier                   text,
    name_identifier_scheme            enum ('ROR', 'GRID', 'ISNI', 'ORCID'),
    name_identifier_scheme_uri        text,
    affiliation                       varchar(255),
    affiliation_identifier            text,
    affiliation_identifier_scheme     enum ('ROR', 'GRID', 'ISNI'),
    affiliation_identifier_scheme_uri text,
    primary key (id),
    foreign key (pid) references mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_update`
(
    uUserID varchar(255)    not null,
    uDBID   bigint unsigned not null,
    created timestamp       not null default now(),
    primary key (uUserID, uDBID),
    foreign key (uDBID) references mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_access`
(
    aUserID  varchar(255) not null,
    aDBID    bigint unsigned references mdb_databases (id),
    attime   timestamp,
    download boolean,
    created  timestamp    not null default now(),
    primary key (aUserID, aDBID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_have_access`
(
    user_id     varchar(36)                             not null,
    database_id bigint unsigned references mdb_databases (id),
    access_type enum ('READ', 'WRITE_OWN', 'WRITE_ALL') not null,
    created     timestamp                               not null default now(),
    primary key (user_id, database_id),
    foreign key (user_id) references mdb_users (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_types`
(
    id            varchar(36)     not null,
    image_id      bigint unsigned not null,
    display_name  varchar(255)    not null,
    value         varchar(255)    not null,
    size_min      int unsigned,
    size_max      int unsigned,
    size_default  int unsigned,
    size_required boolean comment 'When setting NULL, the service assumes the data type has no size',
    size_step     int unsigned,
    d_min         int unsigned,
    d_max         int unsigned,
    d_default     int unsigned,
    d_required    boolean comment 'When setting NULL, the service assumes the data type has no d',
    d_step        int unsigned,
    type_hint     text,
    data_hint     text,
    documentation text            not null,
    is_generated  boolean         not null,
    is_quoted     boolean         not null,
    is_buildable  boolean         not null,
    primary key (id),
    foreign key (image_id) references `mdb_images` (`id`),
    unique (value)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_operators`
(
    id            varchar(36)     not null,
    image_id      bigint unsigned not null,
    display_name  varchar(255)    not null,
    value         varchar(255)    not null,
    documentation text            not null,
    primary key (id),
    foreign key (image_id) references `mdb_images` (`id`),
    unique (value)
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
                               size_step, d_min, d_max, d_default, d_required, d_step, type_hint, data_hint,
                               documentation, is_quoted, is_buildable, is_generated)
VALUES (1, 'BIGINT(size)', 'bigint', 0, null, null, false, 1, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bigint/', false, true, false),
       (1, 'BINARY(size)', 'binary', 0, 255, 255, true, 1, null, null, null, null, null, 'size in Bytes', null,
        'https://mariadb.com/kb/en/binary/', false, true, false),
       (1, 'BIT(size)', 'bit', 0, 64, null, false, 1, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bit/', false, true, false),
       (1, 'BLOB(size)', 'blob', 0, 65535, null, false, 1, null, null, null, null, null, 'size in Bytes', null,
        'https://mariadb.com/kb/en/blob/', false, false, false),
       (1, 'BOOL', 'bool', null, null, null, null, null, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/bool/', false, true, false),
       (1, 'CHAR(size)', 'char', 0, 255, 255, false, 1, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/char/', false, true, false),
       (1, 'DATE', 'date', null, null, null, null, null, null, null, null, null, null,
        'min. 1000-01-01, max. 9999-12-31', 'e.g. YYYY-MM-DD, YY-MM-DD, YYMMDD, YYYY/MM/DD',
        'https://mariadb.com/kb/en/date/', true, true, false),
       (1, 'DATETIME(fsp)', 'datetime', 0, 6, null, null, 1, null, null, null, null, null,
        'fsp=microsecond precision, min. 1000-01-01 00:00:00.0, max. 9999-12-31 23:59:59.9',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/datetime/', true, true, false),
       (1, 'DECIMAL(size, d)', 'decimal', 0, 65, null, false, 1, 0, 38, null, false, null, null, null,
        'https://mariadb.com/kb/en/decimal/', false, true, false),
       (1, 'DOUBLE(size, d)', 'double', null, null, null, false, null, null, null, null, false, null, null, null,
        'https://mariadb.com/kb/en/double/', false, true, false),
       (1, 'ENUM(v1,v2,...)', 'enum', null, null, null, null, null, null, null, null, null, null, null,
        'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/enum/', true, true, false),
       (1, 'FLOAT(size)', 'float', null, null, null, false, null, null, null, null, null, null, null, null,
        'https://mariadb.com/kb/en/float/', false, true, false),
       (1, 'INT(size)', 'int', null, null, null, false, null, null, null, null, null, null, 'size in Bytes', null,
        'https://mariadb.com/kb/en/int/', false, true, false),
       (1, 'LONGBLOB', 'longblob', null, null, null, null, null, null, null, null, null, null, 'max. 3.999 GiB', null,
        'https://mariadb.com/kb/en/longblob/', false, true, false),
       (1, 'LONGTEXT', 'longtext', null, null, null, null, null, null, null, null, null, null, 'max. 3.999 GiB', null,
        'https://mariadb.com/kb/en/longtext/', true, true, false),
       (1, 'MEDIUMBLOB', 'mediumblob', null, null, null, null, null, null, null, null, null, null, 'max. 15.999 MiB',
        null, 'https://mariadb.com/kb/en/mediumblob/', false, true, false),
       (1, 'MEDIUMINT', 'mediumint', null, null, null, null, null, null, null, null, null, null, 'size in Bytes', null,
        'https://mariadb.com/kb/en/mediumint/', false, true, false),
       (1, 'MEDIUMTEXT', 'mediumtext', null, null, null, null, null, null, null, null, null, null, 'size in Bytes',
        null, 'https://mariadb.com/kb/en/mediumtext/', true, true, false),
       (1, 'SERIAL', 'serial', null, null, null, null, null, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/bigint/', true, true, true),
       (1, 'SET(v1,v2,...)', 'set', null, null, null, null, null, null, null, null, null, null, null,
        'e.g. value1, value2, ...', 'https://mariadb.com/kb/en/set/', true, true, false),
       (1, 'SMALLINT(size)', 'smallint', 0, null, null, false, null, null, null, null, null, null, 'size in Bytes',
        null, 'https://mariadb.com/kb/en/smallint/', false, true, false),
       (1, 'TEXT(size)', 'text', 0, null, null, false, null, null, null, null, null, null, 'size in Bytes', null,
        'https://mariadb.com/kb/en/text/', true, true, false),
       (1, 'TIME(fsp)', 'time', 0, 6, 0, false, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'e.g. HH:MM:SS, HH:MM, HHMMSS, H:M:S',
        'https://mariadb.com/kb/en/time/', true, true, false),
       (1, 'TIMESTAMP(fsp)', 'timestamp', 0, 6, 0, false, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6',
        'e.g. YYYY-MM-DD HH:MM:SS, YY-MM-DD HH:MM:SS, YYYYMMDDHHMMSS, YYMMDDHHMMSS, YYYYMMDD, YYMMDD',
        'https://mariadb.com/kb/en/timestamp/', true, true, false),
       (1, 'TINYBLOB', 'tinyblob', null, null, null, null, null, null, null, null, null, null, null,
        'fsp=microsecond precision, min. 0, max. 6', 'https://mariadb.com/kb/en/timestamp/', false, true, false),
       (1, 'TINYINT(size)', 'tinyint', 0, null, null, false, null, null, null, null, null, null, null,
        'size in Bytes', 'https://mariadb.com/kb/en/tinyint/', false, true, false),
       (1, 'TINYTEXT', 'tinytext', null, null, null, null, null, null, null, null, null, null, null,
        'max. 255 characters', 'https://mariadb.com/kb/en/tinytext/', true, true, false),
       (1, 'YEAR', 'year', 2, 4, null, false, 2, null, null, null, null, null, 'min. 1901, max. 2155', 'e.g. YYYY, YY',
        'https://mariadb.com/kb/en/year/', false, true, false),
       (1, 'VARBINARY(size)', 'varbinary', 0, null, null, true, null, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/varbinary/', false, true, false),
       (1, 'varchar(size)', 'varchar', 0, 65532, 255, true, null, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/varchar/', false, true, false);

INSERT INTO `mdb_image_operators` (image_id, display_name, value, documentation)
VALUES (1, 'Equal operator', '=', 'https://mariadb.com/kb/en/assignment-operators-assignment-operator/'),
       (1, 'NULL-safe equal operator', '<=>', 'https://mariadb.com/kb/en/null-safe-equal/'),
       (1, 'Less-than operator', '<', 'https://mariadb.com/kb/en/less-than/'),
       (1, 'Less than or equal operator', '<=', 'https://mariadb.com/kb/en/less-than-or-equal/'),
       (1, 'Greater-than operator', '>', 'https://mariadb.com/kb/en/greater-than/'),
       (1, 'Greater than or equal operator', '>=', 'https://mariadb.com/kb/en/greater-than-or-equal/'),
       (1, 'Not equal operator', '!=', 'https://mariadb.com/kb/en/not-equal/'),
       (1, 'Addition operator', '+', 'https://mariadb.com/kb/en/addition-operator/'),
       (1, 'Division operator', '/', 'https://mariadb.com/kb/en/division-operator/'),
       (1, 'Modulo operator', '%', 'https://mariadb.com/kb/en/modulo-operator/'),
       (1, 'Multiplication operator', '*', 'https://mariadb.com/kb/en/multiplication-operator/'),
       (1, 'Subtraction operator', '-', 'https://mariadb.com/kb/en/subtraction-operator-/'),
       (1, 'LIKE', 'LIKE', 'https://mariadb.com/kb/en/like/'),
       (1, 'NOT LIKE', 'NOT LIKE', 'https://mariadb.com/kb/en/not-like/'),
       (1, 'IN', 'IN', 'https://mariadb.com/kb/en/in/'),
       (1, 'NOT IN', 'NOT IN', 'https://mariadb.com/kb/en/not-in/'),
       (1, 'IS', 'IS', 'https://mariadb.com/kb/en/is/'),
       (1, 'IS NOT', 'IS NOT', 'https://mariadb.com/kb/en/is-not/'),
       (1, 'IS not null', 'IS not null', 'https://mariadb.com/kb/en/is-not-null/'),
       (1, 'IS NULL', 'IS NULL', 'https://mariadb.com/kb/en/is-null/'),
       (1, 'ISNULL', 'ISNULL', 'https://mariadb.com/kb/en/isnull/'),
       (1, 'REGEXP', 'REGEXP', 'https://mariadb.com/kb/en/regexp/'),
       (1, 'NOT REGEXP', 'NOT REGEXP', 'https://mariadb.com/kb/en/not-regexp/'),
       (1, 'Bitwise AND', '&', 'https://mariadb.com/kb/en/bitwise_and/'),
       (1, 'Bitwise OR', '|', 'https://mariadb.com/kb/en/bitwise-or/'),
       (1, 'Bitwise XOR', '^', 'https://mariadb.com/kb/en/bitwise-xor/'),
       (1, 'Bitwise NOT', '~', 'https://mariadb.com/kb/en/bitwise-not/'),
       (1, 'Left shift', '<<', 'https://mariadb.com/kb/en/shift-left/'),
       (1, 'Right shift', '>>', 'https://mariadb.com/kb/en/shift-right/');

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