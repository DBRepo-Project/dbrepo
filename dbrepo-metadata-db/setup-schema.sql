BEGIN;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_images`
(
    id            bigint                 NOT NULL AUTO_INCREMENT,
    name          character varying(255) NOT NULL,
    version       character varying(255) NOT NULL,
    default_port  integer                NOT NULL,
    dialect       character varying(255) NOT NULL,
    driver_class  character varying(255) NOT NULL,
    jdbc_method   character varying(255) NOT NULL,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (id),
    UNIQUE (name, version)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_images_date`
(
    id              bigint                 NOT NULL AUTO_INCREMENT,
    iid             bigint                 NOT NULL,
    database_format character varying(255) NOT NULL,
    unix_format     character varying(255) NOT NULL,
    example         character varying(255) NOT NULL,
    has_time        boolean                NOT NULL,
    created_at      timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (iid) REFERENCES mdb_images (id),
    UNIQUE (database_format, unix_format, example)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_containers`
(
    id                  bigint                 NOT NULL AUTO_INCREMENT,
    INTERNAL_NAME       character varying(255) NOT NULL,
    NAME                character varying(255) NOT NULL,
    HOST                character varying(255) NOT NULL,
    PORT                integer                NOT NULL,
    image_id            bigint                 NOT NULL,
    created             timestamp              NOT NULL DEFAULT NOW(),
    LAST_MODIFIED       timestamp,
    privileged_username character varying(255) NOT NULL,
    privileged_password character varying(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_data`
(
    ID           bigint NOT NULL AUTO_INCREMENT,
    PROVENANCE   TEXT,
    FileEncoding TEXT,
    FileType     VARCHAR(100),
    Version      TEXT,
    Seperator    TEXT,
    PRIMARY KEY (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_licenses`
(
    identifier character varying(255) NOT NULL,
    uri        TEXT                   NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (uri)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_databases`
(
    id             bigint                 NOT NULL AUTO_INCREMENT,
    cid            bigint                 NOT NULL,
    name           character varying(255) NOT NULL,
    internal_name  character varying(255) NOT NULL,
    exchange_name  character varying(255) NOT NULL,
    description    TEXT,
    engine         character varying(20),
    is_public      BOOLEAN                NOT NULL DEFAULT TRUE,
    created_by     character varying(255),
    owned_by       character varying(255),
    contact_person character varying(255),
    created        timestamp              NOT NULL DEFAULT NOW(),
    last_modified  timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (cid) REFERENCES mdb_containers (id) /* currently we only support one-to-one */
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_databases_subjects`
(
    dbid     BIGINT                 NOT NULL,
    subjects character varying(255) NOT NULL,
    PRIMARY KEY (dbid, subjects)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_tables`
(
    ID            bigint                 NOT NULL AUTO_INCREMENT,
    tDBID         bigint                 NOT NULL,
    internal_name character varying(255) NOT NULL,
    queue_name    character varying(255) NOT NULL,
    routing_key   character varying(255) NOT NULL,
    tName         VARCHAR(50),
    tDescription  TEXT,
    NumCols       INTEGER,
    NumRows       INTEGER,
    `separator`   CHAR(1),
    quote         CHAR(1),
    element_null  VARCHAR(50),
    skip_lines    BIGINT,
    element_true  VARCHAR(50),
    element_false VARCHAR(50),
    Version       TEXT,
    created       timestamp              NOT NULL DEFAULT NOW(),
    versioned     boolean                not null default true,
    created_by    character varying(255) NOT NULL,
    owned_by      character varying(255) NOT NULL,
    last_modified timestamp,
    PRIMARY KEY (ID),
    FOREIGN KEY (tDBID) REFERENCES mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns`
(
    ID               bigint       NOT NULL AUTO_INCREMENT,
    tID              bigint       NOT NULL,
    dfID             bigint,
    cName            VARCHAR(100),
    internal_name    VARCHAR(100) NOT NULL,
    Datatype         ENUM ('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR'),
    length           INT          NULL,
    ordinal_position INTEGER      NOT NULL,
    is_primary_key   BOOLEAN      NOT NULL,
    index_length     INT          NULL,
    size             INT,
    d                INT,
    auto_generated   BOOLEAN               DEFAULT false,
    is_null_allowed  BOOLEAN      NOT NULL DEFAULT true,
    created          timestamp    NOT NULL DEFAULT NOW(),
    last_modified    timestamp,
    FOREIGN KEY (tID) REFERENCES mdb_tables (ID),
    PRIMARY KEY (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_enums`
(
    id        bigint                 NOT NULL AUTO_INCREMENT,
    column_id bigint                 NOT NULL,
    value     CHARACTER VARYING(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID),
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_sets`
(
    id        bigint                 NOT NULL AUTO_INCREMENT,
    column_id bigint                 NOT NULL,
    value     CHARACTER VARYING(255) NOT NULL,
    FOREIGN KEY (column_id) REFERENCES mdb_columns (ID),
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_nom`
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    maxlength     INTEGER,
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_columns (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_num`
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    SIunit        TEXT,
    MaxVal        NUMERIC,
    MinVal        NUMERIC,
    Mean          NUMERIC,
    Median        NUMERIC,
    Sd            Numeric,
--    Histogram     INTEGER[],
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_columns (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_cat`
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    num_cat       INTEGER,
--    cat_array     TEXT[],
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_columns (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_constraints_foreign_key`
(
    fkid      BIGINT      NOT NULL AUTO_INCREMENT,
    tid       BIGINT      NOT NULL,
    rtid      BIGINT      NOT NULL,
    on_update VARCHAR(50) NULL,
    on_delete VARCHAR(50) NULL,
    position  INT         NULL,
    PRIMARY KEY (fkid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id),
    FOREIGN KEY (rtid) REFERENCES mdb_tables (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_constraints_foreign_key_reference`
(
    id   BIGINT NOT NULL AUTO_INCREMENT,
    fkid BIGINT NOT NULL,
    cid  BIGINT NOT NULL,
    rcid BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (fkid) REFERENCES mdb_constraints_foreign_key (fkid) ON UPDATE CASCADE,
    FOREIGN KEY (cid) REFERENCES mdb_columns (id),
    FOREIGN KEY (rcid) REFERENCES mdb_columns (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_constraints_unique`
(
    uid      BIGINT NOT NULL AUTO_INCREMENT,
    tid      BIGINT NOT NULL,
    position INT    NULL,
    PRIMARY KEY (uid),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id)
);

CREATE TABLE IF NOT EXISTS `fda`.`mdb_constraints_unique_columns`
(
    id  BIGINT NOT NULL AUTO_INCREMENT,
    uid BIGINT NOT NULL,
    cid BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_constraints_unique (uid),
    FOREIGN KEY (cid) REFERENCES mdb_columns (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_constraints_checks`
(
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    tid    BIGINT       NOT NULL,
    checks VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (tid) REFERENCES mdb_tables (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_concepts`
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    uri         text         not null,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     timestamp    NOT NULL DEFAULT NOW(),
    created_by  character varying(255),
    PRIMARY KEY (id),
    UNIQUE (uri)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_units`
(
    id          bigint       NOT NULL AUTO_INCREMENT,
    uri         text         not null,
    name        VARCHAR(255) null,
    description TEXT         null,
    created     timestamp    NOT NULL DEFAULT NOW(),
    created_by  character varying(255),
    PRIMARY KEY (id),
    UNIQUE (uri)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_concepts`
(
    id      bigint    NOT NULL,
    cID     bigint    NOT NULL,
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_columns_units`
(
    id      bigint    NOT NULL,
    cID     bigint    NOT NULL,
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (cID) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_view`
(
    id            bigint                 NOT NULL AUTO_INCREMENT,
    vdbid         bigint                 NOT NULL,
    vName         VARCHAR(255)           NOT NULL,
    internal_name VARCHAR(255)           NOT NULL,
    Query         TEXT                   NOT NULL,
    query_hash    VARCHAR(255)           NOT NULL,
    Public        BOOLEAN                NOT NULL,
    NumCols       INTEGER,
    NumRows       INTEGER,
    InitialView   BOOLEAN                NOT NULL,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    created_by    character varying(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_banner_messages`
(
    id            bigint                            NOT NULL AUTO_INCREMENT,
    type          ENUM ('ERROR', 'WARNING', 'INFO') NOT NULL default 'INFO',
    message       TEXT                              NOT NULL,
    link          TEXT                              NULL,
    link_text     VARCHAR(255)                      NULL,
    display_start timestamp                         NULL,
    display_end   timestamp                         NULL,
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_ontologies`
(
    id              bigint                 NOT NULL AUTO_INCREMENT,
    prefix          VARCHAR(8)             NOT NULL,
    uri             TEXT                   NOT NULL,
    uri_pattern     TEXT,
    sparql_endpoint TEXT                   NULL,
    last_modified   timestamp,
    created         timestamp              NOT NULL DEFAULT NOW(),
    created_by      character varying(255) NULL,
    UNIQUE (prefix),
    UNIQUE (uri),
    PRIMARY KEY (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_view_columns`
(
    id       BIGINT  NOT NULL AUTO_INCREMENT,
    cid      BIGINT  NOT NULL,
    vid      BIGINT  NOT NULL,
    position INTEGER NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (vid) REFERENCES mdb_view (id),
    FOREIGN KEY (cid) REFERENCES mdb_columns (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifiers`
(
    id                bigint                              NOT NULL AUTO_INCREMENT,
    dbid              bigint,
    qid               bigint,
    vid               bigint,
    publisher         VARCHAR(255)                        NOT NULL,
    language          VARCHAR(2),
    visibility        ENUM ('SELF', 'EVERYONE')           NOT NULL default 'EVERYONE',
    publication_year  INTEGER                             NOT NULL,
    publication_month INTEGER,
    publication_day   INTEGER,
    identifier_type   ENUM ('DATABASE', 'SUBSET', 'VIEW') NOT NULL,
    query             TEXT,
    query_normalized  TEXT,
    query_hash        VARCHAR(255),
    execution         timestamp,
    result_hash       VARCHAR(255),
    result_number     bigint,
    doi               VARCHAR(255),
    created           timestamp                           NOT NULL DEFAULT NOW(),
    created_by        character varying(255)              NOT NULL,
    last_modified     timestamp,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    UNIQUE (dbid, qid)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifier_licenses`
(
    pid        bigint       NOT NULL,
    license_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (pid, license_id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (license_id) REFERENCES mdb_licenses (identifier)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifier_titles`
(
    id         bigint NOT NULL AUTO_INCREMENT,
    pid        bigint NOT NULL,
    title      text   NOT NULL,
    title_type ENUM ('ALTERNATIVE_TITLE', 'SUBTITLE', 'TRANSLATED_TITLE', 'OTHER'),
    language   VARCHAR(2),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifier_funders`
(
    id                     bigint       NOT NULL AUTO_INCREMENT,
    pid                    bigint       NOT NULL,
    funder_name            VARCHAR(255) NOT NULL,
    funder_identifier      TEXT,
    funder_identifier_type ENUM ('CROSSREF_FUNDER_ID', 'GRID', 'ISNI', 'ROR', 'OTHER'),
    scheme_uri             text,
    award_number           VARCHAR(255),
    award_title            text,
    language               VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifier_descriptions`
(
    id               bigint NOT NULL AUTO_INCREMENT,
    pid              bigint NOT NULL,
    description      text   NOT NULL,
    description_type ENUM ('ABSTRACT', 'METHODS', 'SERIES_INFORMATION', 'TABLE_OF_CONTENTS', 'TECHNICAL_INFO', 'OTHER'),
    language         VARCHAR(2),
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_related_identifiers`
(
    id       bigint       NOT NULL AUTO_INCREMENT,
    pid      bigint       NOT NULL,
    value    varchar(255) NOT NULL,
    type     varchar(255),
    relation varchar(255),
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    UNIQUE (pid, value)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_identifier_creators`
(
    id                                bigint       NOT NULL AUTO_INCREMENT,
    pid                               bigint       NOT NULL,
    given_names                       text,
    family_name                       text,
    creator_name                      VARCHAR(255) NOT NULL,
    name_type                         ENUM ('PERSONAL', 'ORGANIZATIONAL') default 'PERSONAL',
    name_identifier                   text,
    name_identifier_scheme            ENUM ('ROR', 'GRID', 'ISNI', 'ORCID'),
    name_identifier_scheme_uri        text,
    affiliation                       VARCHAR(255),
    affiliation_identifier            text,
    affiliation_identifier_scheme     ENUM ('ROR', 'GRID', 'ISNI'),
    affiliation_identifier_scheme_uri text,
    PRIMARY KEY (id),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id),
    UNIQUE (pid, creator_name)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_feed`
(
    fDBID   bigint,
    fID     bigint,
    fUserId character varying(255) not null,
    fDataID bigint REFERENCES mdb_data (ID),
    created timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (fDBID, fID, fUserId, fDataID),
    FOREIGN KEY (fDBID, fID) REFERENCES mdb_tables (tDBID, ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_update`
(
    uUserID character varying(255) NOT NULL,
    uDBID   bigint                 NOT NULL,
    created timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (uUserID, uDBID),
    FOREIGN KEY (uDBID) REFERENCES mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_access`
(
    aUserID  character varying(255) NOT NULL,
    aDBID    bigint REFERENCES mdb_databases (id),
    attime   TIMESTAMP,
    download BOOLEAN,
    created  timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `fda`.`mdb_have_access`
(
    user_id     character varying(255)                  NOT NULL,
    database_id bigint REFERENCES mdb_databases (id),
    access_type ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL') NOT NULL,
    created     timestamp                               NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, database_id)
) WITH SYSTEM VERSIONING;

COMMIT;
BEGIN;

INSERT INTO `fda`.`mdb_licenses` (identifier, uri)
VALUES ('MIT', 'https://opensource.org/licenses/MIT'),
       ('GPL-3.0-only', 'https://www.gnu.org/licenses/gpl-3.0-standalone.html'),
       ('BSD-3-Clause', 'https://opensource.org/licenses/BSD-3-Clause'),
       ('BSD-4-Clause', 'http://directory.fsf.org/wiki/License:BSD_4Clause'),
       ('Apache-2.0', 'https://opensource.org/licenses/Apache-2.0'),
       ('CC0-1.0', 'https://creativecommons.org/publicdomain/zero/1.0/legalcode'),
       ('CC-BY-4.0', 'https://creativecommons.org/licenses/by/4.0/legalcode');

INSERT INTO `fda`.`mdb_images` (name, version, default_port, dialect, driver_class, jdbc_method)
VALUES ('mariadb', '10.5', 3306, 'org.hibernate.dialect.MariaDBDialect', 'org.mariadb.jdbc.Driver', 'mariadb');

INSERT INTO `fda`.`mdb_images_date` (iid, database_format, unix_format, example, has_time)
VALUES (1, '%Y-%c-%d %H:%i:%S.%f', 'yyyy-MM-dd HH:mm:ss.SSSSSS', '2022-01-30 13:44:25.499', true),
       (1, '%Y-%c-%d %H:%i:%S', 'yyyy-MM-dd HH:mm:ss', '2022-01-30 13:44:25', true),
       (1, '%Y-%c-%d', 'yyyy-MM-dd', '2022-01-30', false),
       (1, '%H:%i:%S', 'HH:mm:ss', '13:44:25', true);

INSERT INTO `fda`.`mdb_ontologies` (prefix, uri, uri_pattern, sparql_endpoint)
VALUES ('om', 'http://www.ontology-of-units-of-measure.org/resource/om-2/',
        'http://www.ontology-of-units-of-measure.org/resource/om-2/.*', null),
       ('wd', 'http://www.wikidata.org/', 'http://www.wikidata.org/entity/.*', 'https://query.wikidata.org/sparql'),
       ('mo', 'http://purl.org/ontology/mo/', 'http://purl.org/ontology/mo/.*', null),
       ('dc', 'http://purl.org/dc/elements/1.1/', null, null),
       ('xsd', 'http://www.w3.org/2001/XMLSchema#', null, null),
       ('tl', 'http://purl.org/NET/c4dm/timeline.owl#', null, null),
       ('foaf', 'http://xmlns.com/foaf/0.1/', null, null),
       ('schema', 'http://schema.org/', null, null),
       ('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#', null, null),
       ('rdfs', 'http://www.w3.org/2000/01/rdf-schema#', null, null),
       ('owl', 'http://www.w3.org/2002/07/owl#', null, null),
       ('prov', 'http://www.w3.org/ns/prov#', null, null),
       ('db', 'http://dbpedia.org', 'http://dbpedia.org/ontology/.*', 'http://dbpedia.org/sparql');
COMMIT;
