BEGIN;

CREATE TABLE IF NOT EXISTS mdb_users
(
    UserID               bigint       not null AUTO_INCREMENT,
    external_id          VARCHAR(255) UNIQUE,
    OID                  bigint,
    username             VARCHAR(255) not null,
    First_name           VARCHAR(50),
    Last_name            VARCHAR(50),
    Gender               ENUM ('M', 'F', 'D'),
    Preceding_titles     VARCHAR(255),
    Postpositioned_title VARCHAR(255),
    orcid                VARCHAR(16),
    theme_dark           BOOLEAN      NOT NULL DEFAULT false,
    affiliation          VARCHAR(255),
    Main_Email           VARCHAR(255) not null,
    main_email_verified  bool         not null default false,
    password             VARCHAR(255) not null,
    database_password    VARCHAR(255) not null,
    created              timestamp    NOT NULL DEFAULT NOW(),
    last_modified        timestamp,
    PRIMARY KEY (UserID),
    UNIQUE (username),
    UNIQUE (Main_Email),
    UNIQUE (OID)
) WITH SYSTEM VERSIONING;

CREATE TABLE mdb_images
(
    id            bigint                 NOT NULL AUTO_INCREMENT,
    repository    character varying(255) NOT NULL,
    tag           character varying(255) NOT NULL,
    default_port  integer                NOT NULL,
    dialect       character varying(255) NOT NULL,
    driver_class  character varying(255) NOT NULL,
    jdbc_method   character varying(255) NOT NULL,
    compiled      timestamp,
    hash          character varying(255),
    size          bigint,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (id),
    UNIQUE (repository, tag)
) WITH SYSTEM VERSIONING;

CREATE TABLE mdb_time_secrets
(
    id        bigint                 not null AUTO_INCREMENT,
    uid       bigint                 not null,
    token     character varying(255) NOT NULL,
    processed boolean                NOT NULL default false,
    created   timestamp              NOT NULL DEFAULT NOW(),
    valid_to  timestamp              NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_users (UserID)
) WITH SYSTEM VERSIONING;

CREATE TABLE mdb_tokens
(
    id         bigint       not null AUTO_INCREMENT,
    token_hash varchar(255) NOT NULL,
    creator    bigint       not null,
    created    timestamp    NOT NULL DEFAULT NOW(),
    expires    timestamp    NOT NULL,
    last_used  timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (creator) REFERENCES mdb_users (UserID)
) WITH SYSTEM VERSIONING;

CREATE TABLE mdb_images_date
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

CREATE TABLE IF NOT EXISTS mdb_containers
(
    id            bigint                 NOT NULL AUTO_INCREMENT,
    HASH          character varying(255) NOT NULL,
    INTERNAL_NAME character varying(255) NOT NULL,
    NAME          character varying(255) NOT NULL,
    PORT          integer,
    image_id      bigint                 NOT NULL,
    ip_address    character varying(255),
    created       timestamp              NOT NULL DEFAULT NOW(),
    created_by    bigint                 NOT NULL,
    LAST_MODIFIED timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE mdb_images_environment_item
(
    id            bigint                                                                      NOT NULL AUTO_INCREMENT,
    `key`         character varying(255)                                                      NOT NULL,
    value         character varying(255)                                                      NOT NULL,
    etype         ENUM ('PRIVILEGED_USERNAME', 'PRIVILEGED_PASSWORD', 'USERNAME', 'PASSWORD') NOT NULL,
    iid           bigint                                                                      NOT NULL,
    created       timestamp                                                                   NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (id, iid),
    FOREIGN KEY (iid) REFERENCES mdb_images (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_data
(
    ID           bigint NOT NULL AUTO_INCREMENT,
    PROVENANCE   TEXT,
    FileEncoding TEXT,
    FileType     VARCHAR(100),
    Version      TEXT,
    Seperator    TEXT,
    PRIMARY KEY (ID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_user_roles
(
    uid           bigint       not null,
    role          varchar(255) not null,
    created       timestamp    NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (uid),
    FOREIGN KEY (uid) REFERENCES mdb_users (UserID),
    UNIQUE (uid, role)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_licenses
(
    identifier character varying(255) NOT NULL,
    uri        TEXT                   NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (uri)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_databases
(
    id             bigint                 NOT NULL AUTO_INCREMENT,
    name           character varying(255) NOT NULL,
    internal_name  character varying(255) NOT NULL,
    exchange_name  character varying(255) NOT NULL,
    description    TEXT,
    engine         character varying(20),
    is_public      BOOLEAN                NOT NULL DEFAULT TRUE,
    created_by     bigint,
    contact_person bigint,
    created        timestamp              NOT NULL DEFAULT NOW(),
    last_modified  timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (contact_person) REFERENCES mdb_users (UserID),
    FOREIGN KEY (id) REFERENCES mdb_containers (id) /* currently we only support one-to-one */
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_databases_subjects
(
    dbid     BIGINT                 NOT NULL,
    subjects character varying(255) NOT NULL,
    PRIMARY KEY (dbid, subjects)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_tables
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
    created_by    bigint                 NOT NULL,
    last_modified timestamp,
    PRIMARY KEY (ID, tDBID),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (tDBID) REFERENCES mdb_databases (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_columns
(
    ID               bigint       NOT NULL AUTO_INCREMENT,
    cDBID            bigint       NOT NULL,
    tID              bigint       NOT NULL,
    dfID             bigint,
    cName            VARCHAR(100),
    internal_name    VARCHAR(100) NOT NULL,
    Datatype         VARCHAR(50),
    ordinal_position INTEGER      NOT NULL,
    is_primary_key   BOOLEAN      NOT NULL,
    is_unique        BOOLEAN      NOT NULL,
    auto_generated   BOOLEAN               DEFAULT false,
    is_null_allowed  BOOLEAN      NOT NULL,
    foreign_key      VARCHAR(255),
    reference_table  VARCHAR(255),
    created_by       bigint       NOT NULL,
    check_expression character varying(255),
    created          timestamp    NOT NULL DEFAULT NOW(),
    last_modified    timestamp,
    FOREIGN KEY (cDBID, tID) REFERENCES mdb_tables (tDBID, ID),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (ID, cDBID, tID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_columns_enums
(
    ID            bigint                 NOT NULL AUTO_INCREMENT,
    eDBID         bigint                 NOT NULL,
    tID           bigint                 NOT NULL,
    cID           bigint                 NOT NULL,
    enum_values   CHARACTER VARYING(255) NOT NULL,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    FOREIGN KEY (eDBID, tID, cID) REFERENCES mdb_columns (cDBID, tID, ID),
    PRIMARY KEY (ID, eDBID, tID, cID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_columns_nom
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

CREATE TABLE IF NOT EXISTS mdb_columns_num
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

CREATE TABLE IF NOT EXISTS mdb_columns_cat
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

CREATE TABLE IF NOT EXISTS mdb_concepts
(
    uri        text      not null,
    name       VARCHAR(255),
    created    timestamp NOT NULL DEFAULT NOW(),
    created_by bigint,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (uri(200))
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_columns_concepts
(
    cDBID   bigint    NOT NULL,
    tID     bigint    NOT NULL,
    cID     bigint    NOT NULL,
    uri     text      NOT NULL,
    created timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_columns (cDBID, tID, ID),
    #FOREIGN KEY (uri) REFERENCES mdb_concepts (uri), -- does not work in MariaDB as of 10.5+
    PRIMARY KEY (cDBID, tID, cID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_view
(
    id            bigint       NOT NULL AUTO_INCREMENT,
    vcid          bigint       NOT NULL,
    vdbid         bigint       NOT NULL,
    vName         VARCHAR(255) NOT NULL,
    internal_name VARCHAR(255) NOT NULL,
    Query         TEXT         NOT NULL,
    Public        BOOLEAN      NOT NULL,
    NumCols       INTEGER,
    NumRows       INTEGER,
    InitialView   BOOLEAN      NOT NULL,
    created       timestamp    NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    created_by    bigint       NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id),
    PRIMARY KEY (id, vdbid)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_identifiers
(
    id                bigint                               NOT NULL AUTO_INCREMENT,
    cid               bigint                               NOT NULL,
    dbid              bigint                               NOT NULL,
    qid               bigint,
    title             VARCHAR(255)                         NOT NULL,
    publisher         VARCHAR(255)                         NOT NULL,
    language          VARCHAR(50),
    license           VARCHAR(50),
    description       TEXT,
    visibility        ENUM ('SELF', 'TRUSTED', 'EVERYONE') NOT NULL,
    publication_year  INTEGER                              NOT NULL,
    publication_month INTEGER,
    publication_day   INTEGER,
    identifier_type   varchar(50)                          NOT NULL,
    query             TEXT,
    query_normalized  TEXT,
    query_hash        VARCHAR(255),
    execution         timestamp,
    result_hash       VARCHAR(255),
    result_number     bigint,
    doi               VARCHAR(255),
    created           timestamp                            NOT NULL DEFAULT NOW(),
    created_by        bigint                               NOT NULL,
    last_modified     timestamp,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (cid) REFERENCES mdb_containers (id),
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    UNIQUE (cid, dbid, qid)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_related_identifiers
(
    id            bigint       NOT NULL AUTO_INCREMENT,
    iid           bigint       NOT NULL,
    value         varchar(255) NOT NULL,
    type          varchar(255),
    relation      varchar(255),
    created       timestamp    NOT NULL DEFAULT NOW(),
    created_by    bigint       NOT NULL,
    last_modified timestamp,
    PRIMARY KEY (id, iid), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (iid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_creators
(
    id            bigint       NOT NULL AUTO_INCREMENT,
    pid           bigint       NOT NULL,
    firstname     VARCHAR(255) NOT NULL,
    lastname      VARCHAR(255) NOT NULL,
    affiliation   VARCHAR(255),
    orcid         VARCHAR(255),
    created       timestamp    NOT NULL DEFAULT NOW(),
    created_by    bigint       NOT NULL,
    last_modified timestamp    NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (id, pid),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_feed
(
    fDBID   bigint,
    fID     bigint,
    fUserId bigint REFERENCES mdb_users (UserID),
    fDataID bigint REFERENCES mdb_data (ID),
    created timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (fDBID, fID) REFERENCES mdb_tables (tDBID, ID),
    PRIMARY KEY (fDBID, fID, fUserId, fDataID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_update
(
    uUserID bigint REFERENCES mdb_users (UserID),
    uDBID   bigint REFERENCES mdb_databases (id),
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (uUserID, uDBID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_access
(
    aUserID  bigint REFERENCES mdb_users (UserID),
    aDBID    bigint REFERENCES mdb_databases (id),
    attime   TIMESTAMP,
    download BOOLEAN,
    created  timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_have_access
(
    user_id     bigint REFERENCES mdb_users (UserID),
    database_id bigint REFERENCES mdb_databases (id),
    access_type ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL') NOT NULL,
    created     timestamp                               NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, database_id)
) WITH SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS mdb_owns
(
    oUserID bigint REFERENCES mdb_users (UserID),
    oDBID   bigint REFERENCES mdb_databases (ID),
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (oUserID, oDBID)
) WITH SYSTEM VERSIONING;

CREATE VIEW IF NOT EXISTS mdb_invalid_tokens AS
(
SELECT `id`, `token_hash`, `creator`, `created`, `expires`, `last_used`
FROM (SELECT `id`, `token_hash`, `creator`, `created`, `expires`, `last_used`
      FROM `mdb_tokens` FOR SYSTEM_TIME ALL) as t
WHERE NOT EXISTS(SELECT `token_hash`
                 FROM mdb_tokens AS tt
                 WHERE ROW_END > NOW()
                   AND tt.`token_hash` = t.`token_hash`)
GROUP BY `id`);

COMMIT;
BEGIN;

INSERT INTO mdb_licenses (identifier, uri)
VALUES ('MIT', 'https://opensource.org/licenses/MIT'),
       ('GPL-3.0-only', 'https://www.gnu.org/licenses/gpl-3.0-standalone.html'),
       ('BSD-3-Clause', 'https://opensource.org/licenses/BSD-3-Clause'),
       ('BSD-4-Clause', 'http://directory.fsf.org/wiki/License:BSD_4Clause'),
       ('Apache-2.0', 'https://opensource.org/licenses/Apache-2.0'),
       ('CC0-1.0', 'https://creativecommons.org/publicdomain/zero/1.0/legalcode'),
       ('CC-BY-4.0', 'https://creativecommons.org/licenses/by/4.0/legalcode');

INSERT INTO mdb_images (repository, tag, default_port, dialect, driver_class, jdbc_method)
VALUES ('mariadb', '10.5', 3306, 'org.hibernate.dialect.MariaDBDialect', 'org.mariadb.jdbc.Driver', 'mariadb');

INSERT INTO mdb_images_environment_item (`key`, value, etype, iid)
VALUES ('ROOT', 'root', 'PRIVILEGED_USERNAME', 1),
       ('MARIADB_ROOT_PASSWORD', 'mariadb', 'PRIVILEGED_PASSWORD', 1),
       ('MARIADB_USER', 'mariadb', 'USERNAME', 1),
       ('MARIADB_PASSWORD', 'mariadb', 'PASSWORD', 1);

INSERT INTO mdb_images_date (iid, database_format, unix_format, example, has_time)
VALUES (1, '%Y-%c-%d', 'yyyy-MM-dd', '2022-01-30', false),
       (1, '%d.%c.%Y', 'yyyy-MM-dd', '30.01.2022', false),
       (1, '%d.%c.%y', 'yyyy-MM-dd', '30.01.22', false),
       (1, '%c/%d/%Y', 'yyyy-MM-dd', '01/30/2022', false),
       (1, '%c/%d/%y', 'yyyy-MM-dd', '01/30/22', false),
       (1, '%Y-%c-%dT%H:%i:%S.%f', 'yyyy-MM-dd''T''HH:mm:ss.SSSSSS', '2022-01-30T13:44:25.499', true),
       (1, '%Y-%c-%d %H:%i:%S.%f', 'yyyy-MM-dd HH:mm:ss.SSSSSS', '2022-01-30 13:44:25.499', true),
       (1, '%Y-%c-%dT%H:%i:%S', 'yyyy-MM-dd''T''HH:mm:ss', '2022-01-30T13:44:25', true),
       (1, '%Y-%c-%d %H:%i:%S', 'yyyy-MM-dd HH:mm:ss', '2022-01-30 13:44:25', true),
       (1, '%Y-%c-%d %H:%i', 'yyyy-MM-dd HH:mm', '2022-01-30 13:44', true);

COMMIT;
