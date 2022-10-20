CREATE USER root;
CREATE DATABASE root;

BEGIN;

CREATE
    TYPE gender AS ENUM ('F', 'M', 'T');
CREATE
    TYPE access_type AS ENUM ('READ', 'WRITE_OWN', 'WRITE_ALL');
CREATE
    TYPE image_environment_type AS ENUM ('USERNAME', 'PASSWORD', 'PRIVILEGED_USERNAME', 'PRIVILEGED_PASSWORD');
CREATE
    TYPE role_type AS ENUM ('ROLE_RESEARCHER', 'ROLE_DEVELOPER', 'ROLE_DATA_STEWARD');

CREATE
    CAST
    (character varying AS image_environment_type)
    WITH INOUT AS ASSIGNMENT;
CREATE
    CAST
    (character varying AS role_type)
    WITH INOUT AS ASSIGNMENT;
CREATE
    CAST
    (character varying AS access_type)
    WITH INOUT AS ASSIGNMENT;

CREATE SEQUENCE public.mdb_images_environment_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_images_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_images_date_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_containers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_user_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_user_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_data_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_databases_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_tables_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_columns_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_columns_enum_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_view_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_columns_concepts_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_identifiers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_related_identifiers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_creators_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_time_secrets_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.mdb_tokens_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS mdb_users
(
    UserID               bigint                      not null DEFAULT nextval('mdb_user_seq'),
    external_id          VARCHAR(255) UNIQUE,
    OID                  bigint,
    username             VARCHAR(255)                not null,
    First_name           VARCHAR(50),
    Last_name            VARCHAR(50),
    Gender               gender,
    Preceding_titles     VARCHAR(255),
    Postpositioned_title VARCHAR(255),
    orcid                VARCHAR(16),
    theme_dark           BOOLEAN                     NOT NULL DEFAULT false,
    affiliation          VARCHAR(255),
    Main_Email           VARCHAR(255)                not null,
    main_email_verified  bool                        not null default false,
    password             VARCHAR(255)                not null,
    created              timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified        timestamp without time zone,
    PRIMARY KEY (UserID),
    UNIQUE (username),
    UNIQUE (Main_Email),
    UNIQUE (OID)
);

CREATE TABLE public.mdb_images
(
    id            bigint                      NOT NULL DEFAULT nextval('mdb_images_seq'),
    repository    character varying(255)      NOT NULL,
    tag           character varying(255)      NOT NULL,
    default_port  integer                     NOT NULL,
    dialect       character varying(255)      NOT NULL,
    driver_class  character varying(255)      NOT NULL,
    jdbc_method   character varying(255)      NOT NULL,
    compiled      timestamp without time zone,
    hash          character varying(255),
    size          bigint,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    PRIMARY KEY (id),
    UNIQUE (repository, tag)
);

CREATE TABLE public.mdb_time_secrets
(
    id        bigint                      not null default nextval('mdb_time_secrets_seq'),
    uid       bigint                      not null,
    token     character varying(255)      NOT NULL,
    processed boolean                     NOT NULL default false,
    created   timestamp without time zone NOT NULL DEFAULT NOW(),
    valid_to  timestamp without time zone NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_users (UserID)
);

CREATE TABLE public.mdb_tokens
(
    id         bigint                      not null default nextval('mdb_tokens'),
    token_hash varchar(255)                NOT NULL,
    creator    bigint                      not null,
    created    timestamp without time zone NOT NULL DEFAULT NOW(),
    expires    timestamp without time zone NOT NULL,
    last_used  timestamp without time zone,
    deleted    timestamp without time zone,
    PRIMARY KEY (id),
    FOREIGN KEY (creator) REFERENCES mdb_users (UserID)
);

CREATE TABLE public.mdb_images_date
(
    id              bigint                      NOT NULL DEFAULT nextval('mdb_images_date_seq'),
    iid             bigint                      NOT NULL,
    database_format character varying(255)      NOT NULL,
    unix_format     character varying(255)      NOT NULL,
    example         character varying(255)      NOT NULL,
    has_time        boolean                     NOT NULL,
    created_at      timestamp without time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (iid) REFERENCES mdb_images (id),
    UNIQUE (database_format)
);

CREATE TABLE IF NOT EXISTS mdb_containers
(
    id            bigint                      NOT NULL DEFAULT nextval('mdb_containers_seq'),
    HASH          character varying(255)      NOT NULL,
    INTERNAL_NAME character varying(255)      NOT NULL,
    NAME          character varying(255)      NOT NULL,
    PORT          integer,
    image_id      bigint,
    ip_address    character varying(255),
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by    bigint                      NOT NULL,
    LAST_MODIFIED timestamp without time zone,
    deleted       timestamp without time zone,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES mdb_USERS (UserID),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id)
);

CREATE TABLE public.mdb_images_environment_item
(
    id            bigint                      NOT NULL DEFAULT nextval('mdb_images_environment_item_seq'),
    key           character varying(255)      NOT NULL,
    value         character varying(255)      NOT NULL,
    etype         image_environment_type      NOT NULL,
    iid           bigint                      NOT NULL,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    PRIMARY KEY (id, iid),
    FOREIGN KEY (iid) REFERENCES mdb_images (id)
);

CREATE TABLE IF NOT EXISTS mdb_data
(
    ID           bigint DEFAULT nextval('mdb_data_seq'),
    PROVENANCE   TEXT,
    FileEncoding TEXT,
    FileType     VARCHAR(100),
    Version      TEXT,
    Seperator    TEXT,
    PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS mdb_user_roles
(
    uid           bigint                      not null,
    role          varchar(255)                not null,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    PRIMARY KEY (uid),
    FOREIGN KEY (uid) REFERENCES mdb_users (UserID),
    UNIQUE (uid, role)
);

CREATE TABLE IF NOT EXISTS mdb_licenses
(
    identifier character varying(255) NOT NULL,
    uri        TEXT                   NOT NULL,
    PRIMARY KEY (identifier),
    UNIQUE (uri)
);

CREATE TABLE IF NOT EXISTS mdb_databases
(
    id            bigint                      NOT NULL DEFAULT nextval('mdb_databases_seq'),
    name          character varying(255)      NOT NULL,
    internal_name character varying(255)      NOT NULL,
    exchange      character varying(255)      NOT NULL,
    Description   TEXT,
    Engine        VARCHAR(20)                          DEFAULT 'Postgres',
    is_public     BOOLEAN                     NOT NULL DEFAULT TRUE,
    Creator       BIGINT,
    Contactperson BIGINT,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    deleted       timestamp without time zone NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (Creator) REFERENCES mdb_USERS (UserID),
    FOREIGN KEY (Contactperson) REFERENCES mdb_USERS (UserID),
    FOREIGN KEY (id) REFERENCES mdb_containers (id) /* currently we only support one-to-one */
);

CREATE TABLE IF NOT EXISTS mdb_databases_subjects
(
    dbid     BIGINT                 NOT NULL,
    subjects character varying(255) NOT NULL,
    PRIMARY KEY (dbid, subjects)
);

CREATE TABLE IF NOT EXISTS mdb_tables
(
    ID            bigint                      NOT NULL DEFAULT nextval('mdb_tables_seq'),
    tDBID         bigint                      NOT NULL,
    internal_name character varying(255)      NOT NULL,
    topic         character varying(255)      NOT NULL,
    tName         VARCHAR(50),
    tDescription  TEXT,
    NumCols       INTEGER,
    NumRows       INTEGER,
    separator     CHAR(1)                              DEFAULT ',',
    quote         CHAR(1),
    element_null  VARCHAR(50),
    skip_lines    BIGINT,
    element_true  VARCHAR(50),
    element_false VARCHAR(50),
    Version       TEXT,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by    bigint                      NOT NULL,
    last_modified timestamp without time zone,
    PRIMARY KEY (tDBID, ID),
    FOREIGN KEY (created_by) REFERENCES mdb_USERS (UserID),
    FOREIGN KEY (tDBID) REFERENCES mdb_DATABASES (id)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS
(
    ID               bigint                      NOT NULL DEFAULT nextval('mdb_columns_seq'),
    cDBID            bigint                      NOT NULL,
    tID              bigint                      NOT NULL,
    dfID             bigint,
    cName            VARCHAR(100),
    internal_name    VARCHAR(100)                NOT NULL,
    Datatype         VARCHAR(50),
    ordinal_position INTEGER                     NOT NULL,
    is_primary_key   BOOLEAN                     NOT NULL,
    is_unique        BOOLEAN                     NOT NULL,
    auto_generated   BOOLEAN                              DEFAULT false,
    is_null_allowed  BOOLEAN                     NOT NULL,
    foreign_key      VARCHAR(255),
    reference_table  VARCHAR(255),
    created_by       bigint                      NOT NULL,
    check_expression character varying(255),
    created          timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified    timestamp without time zone,
    FOREIGN KEY (cDBID, tID) REFERENCES mdb_TABLES (tDBID, ID),
    FOREIGN KEY (created_by) REFERENCES mdb_USERS (UserID),
    PRIMARY KEY (cDBID, tID, ID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_ENUMS
(
    ID            bigint                      NOT NULL DEFAULT nextval('mdb_columns_enum_seq'),
    eDBID         bigint                      NOT NULL,
    tID           bigint                      NOT NULL,
    cID           bigint                      NOT NULL,
    enum_values   CHARACTER VARYING(255)      NOT NULL,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    FOREIGN KEY (eDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (ID, eDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_nom
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    maxlength     INTEGER,
    last_modified timestamp without time zone,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_num
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
    Histogram     INTEGER[],
    last_modified timestamp without time zone,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_cat
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    num_cat       INTEGER,
    cat_array     TEXT[],
    last_modified timestamp without time zone,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_concepts
(
    URI        TEXT,
    name       TEXT,
    created    timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by bigint,
    FOREIGN KEY (created_by) REFERENCES mdb_USERS (UserID),
    PRIMARY KEY (URI)
);

CREATE TABLE IF NOT EXISTS mdb_columns_concepts
(
    cDBID   bigint                      NOT NULL,
    tID     bigint                      NOT NULL,
    cID     bigint                      NOT NULL,
    URI     TEXT REFERENCES mdb_concepts (URI),
    created timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_VIEW
(
    id            bigint                      NOT NULL DEFAULT nextval('mdb_view_seq'),
    vcid          bigint                      NOT NULL,
    vdbid         bigint                      NOT NULL,
    vName         VARCHAR(255)                NOT NULL,
    internal_name VARCHAR(255)                NOT NULL,
    Query         TEXT                        NOT NULL,
    Public        BOOLEAN                     NOT NULL,
    NumCols       INTEGER,
    NumRows       INTEGER,
    InitialView   BOOLEAN                     NOT NULL,
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    last_modified timestamp without time zone,
    deleted       timestamp without time zone,
    created_by    bigint                      NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_USERS (UserID),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id),
    PRIMARY KEY (vdbid, id)
);

CREATE TABLE IF NOT EXISTS mdb_identifiers
(
    id                bigint                               DEFAULT nextval('mdb_identifiers_seq'),
    cid               bigint                      NOT NULL,
    dbid              bigint                      NOT NULL,
    qid               bigint,
    title             VARCHAR(255)                NOT NULL,
    publisher         VARCHAR(255)                NOT NULL,
    language          VARCHAR(50),
    license           VARCHAR(50),
    description       TEXT,
    visibility        VARCHAR(10)                 NOT NULL DEFAULT 'SELF',
    publication_year  INTEGER                     NOT NULL,
    publication_month INTEGER,
    publication_day   INTEGER,
    identifier_type   varchar(50)                 NOT NULL,
    query             TEXT,
    query_normalized  TEXT,
    query_hash        VARCHAR(255),
    execution         timestamp,
    result_hash       VARCHAR(255),
    result_number     bigint,
    doi               VARCHAR(255),
    created           timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by        bigint                      NOT NULL,
    last_modified     timestamp without time zone,
    deleted           timestamp without time zone,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (cid) REFERENCES mdb_containers (id),
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    UNIQUE (cid, dbid, qid)
);

CREATE TABLE IF NOT EXISTS mdb_related_identifiers
(
    id            bigint                               DEFAULT nextval('mdb_related_identifiers_seq'),
    iid           bigint                      NOT NULL,
    value         text                        NOT NULL,
    type          varchar(255),
    relation      varchar(255),
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by    bigint                      NOT NULL,
    last_modified timestamp without time zone,
    deleted       timestamp without time zone,
    PRIMARY KEY (id, iid), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (iid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID)
);

CREATE TABLE IF NOT EXISTS mdb_creators
(
    id            bigint                               DEFAULT nextval('mdb_creators_seq'),
    pid           bigint                      NOT NULL,
    name          VARCHAR(255)                NOT NULL,
    affiliation   VARCHAR(255),
    orcid         VARCHAR(255),
    created       timestamp without time zone NOT NULL DEFAULT NOW(),
    created_by    bigint                      NOT NULL,
    last_modified timestamp without time zone NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (id, pid),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TABLE IF NOT EXISTS mdb_views_databases
(
    mdb_view_id  bigint,
    databases_id bigint REFERENCES mdb_DATABASES (id),
    created      timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (mdb_view_id, databases_id) REFERENCES mdb_VIEW (id, vdbid),
    PRIMARY KEY (mdb_view_id, databases_id)
);

CREATE TABLE IF NOT EXISTS mdb_feed
(
    fDBID   bigint,
    fID     bigint,
    fUserId bigint REFERENCES mdb_USERS (UserID),
    fDataID INTEGER REFERENCES mdb_DATA (ID),
    created timestamp without time zone NOT NULL DEFAULT NOW(),
    FOREIGN KEY (fDBID, fID) REFERENCES mdb_TABLES (tDBID, ID),
    PRIMARY KEY (fDBID, fID, fUserId, fDataID)
);

CREATE TABLE IF NOT EXISTS mdb_update
(
    uUserID bigint REFERENCES mdb_USERS (UserID),
    uDBID   bigint REFERENCES mdb_DATABASES (id),
    created timestamp without time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY (uUserID, uDBID)
);

CREATE TABLE IF NOT EXISTS mdb_access
(
    aUserID  bigint REFERENCES mdb_USERS (UserID),
    aDBID    bigint REFERENCES mdb_DATABASES (id),
    attime   TIMESTAMP,
    download BOOLEAN,
    created  timestamp without time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID)
);

CREATE TABLE IF NOT EXISTS mdb_have_access
(
    hUserID bigint REFERENCES mdb_USERS (UserID),
    hDBID   bigint REFERENCES mdb_DATABASES (id),
    hType   access_type,
    created timestamp without time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY (hUserID, hDBID)
);

CREATE TABLE IF NOT EXISTS mdb_owns
(
    oUserID bigint REFERENCES mdb_USERS (UserID),
    oDBID   bigint REFERENCES mdb_DATABASES (ID),
    created timestamp without time zone NOT NULL DEFAULT NOW(),
    PRIMARY KEY (oUserID, oDBID)
);

COMMIT;

BEGIN;

INSERT INTO mdb_users (username, Main_Email, password)
VALUES ('system', 'noreply@dbrepo.ossdip.at', (SELECT md5(random()::text)));

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

INSERT INTO mdb_images_environment_item (key, value, etype, iid)
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
