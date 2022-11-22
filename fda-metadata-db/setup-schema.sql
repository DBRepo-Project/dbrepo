BEGIN;

CREATE SEQUENCE mdb_images_environment_item_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_images_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_images_date_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_containers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_user_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_user_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_data_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_databases_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_tables_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_columns_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_columns_enum_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_view_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_columns_concepts_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_identifiers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_related_identifiers_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_creators_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_time_secrets_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE mdb_tokens_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS mdb_users
(
    UserID               bigint       not null DEFAULT nextval(mdb_user_seq),
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
    created              timestamp    NOT NULL DEFAULT NOW(),
    last_modified        timestamp,
    PRIMARY KEY (UserID),
    UNIQUE (username),
    UNIQUE (Main_Email),
    UNIQUE (OID)
);

CREATE TABLE mdb_images
(
    id            bigint                 NOT NULL DEFAULT nextval(mdb_images_seq),
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
);

CREATE TABLE mdb_time_secrets
(
    id        bigint                 not null default nextval(mdb_time_secrets_seq),
    uid       bigint                 not null,
    token     character varying(255) NOT NULL,
    processed boolean                NOT NULL default false,
    created   timestamp              NOT NULL DEFAULT NOW(),
    valid_to  timestamp              NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (uid) REFERENCES mdb_users (UserID)
);

CREATE TABLE mdb_tokens
(
    id         bigint       not null default nextval(mdb_tokens_seq),
    token_hash varchar(255) NOT NULL,
    creator    bigint       not null,
    created    timestamp    NOT NULL DEFAULT NOW(),
    expires    timestamp    NOT NULL,
    last_used  timestamp,
    deleted    timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (creator) REFERENCES mdb_users (UserID)
);

CREATE TABLE mdb_images_date
(
    id              bigint                 NOT NULL DEFAULT nextval(mdb_images_date_seq),
    iid             bigint                 NOT NULL,
    database_format character varying(255) NOT NULL,
    unix_format     character varying(255) NOT NULL,
    example         character varying(255) NOT NULL,
    has_time        boolean                NOT NULL,
    created_at      timestamp              NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    FOREIGN KEY (iid) REFERENCES mdb_images (id),
    UNIQUE (database_format)
);

CREATE TABLE IF NOT EXISTS mdb_containers
(
    id            bigint                 NOT NULL DEFAULT nextval(mdb_containers_seq),
    HASH          character varying(255) NOT NULL,
    INTERNAL_NAME character varying(255) NOT NULL,
    NAME          character varying(255) NOT NULL,
    PORT          integer,
    image_id      bigint,
    ip_address    character varying(255),
    created       timestamp              NOT NULL DEFAULT NOW(),
    created_by    bigint                 NOT NULL,
    LAST_MODIFIED timestamp,
    deleted       timestamp,
    PRIMARY KEY (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (image_id) REFERENCES mdb_images (id)
);

CREATE TABLE mdb_images_environment_item
(
    id            bigint                                                                      NOT NULL DEFAULT nextval(mdb_images_environment_item_seq),
    `key`         character varying(255)                                                      NOT NULL,
    value         character varying(255)                                                      NOT NULL,
    etype         ENUM ('PRIVILEGED_USERNAME', 'PRIVILEGED_PASSWORD', 'USERNAME', 'PASSWORD') NOT NULL,
    iid           bigint                                                                      NOT NULL,
    created       timestamp                                                                   NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    PRIMARY KEY (id, iid),
    FOREIGN KEY (iid) REFERENCES mdb_images (id)
);

CREATE TABLE IF NOT EXISTS mdb_data
(
    ID           bigint DEFAULT nextval(mdb_data_seq),
    PROVENANCE   TEXT,
    FileEncoding TEXT,
    FileType     VARCHAR(100),
    Version      TEXT,
    Seperator    TEXT,
    PRIMARY KEY (ID)
);

CREATE TABLE IF NOT EXISTS mdb_user_roles
(
    uid           bigint       not null,
    role          varchar(255) not null,
    created       timestamp    NOT NULL DEFAULT NOW(),
    last_modified timestamp,
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
    id            bigint                 NOT NULL DEFAULT nextval(mdb_databases_seq),
    name          character varying(255) NOT NULL,
    internal_name character varying(255) NOT NULL,
    exchange      character varying(255) NOT NULL,
    Description   TEXT,
    Engine        VARCHAR(20),
    is_public     BOOLEAN                NOT NULL DEFAULT TRUE,
    Creator       BIGINT,
    Contactperson BIGINT,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    deleted       timestamp              NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (Creator) REFERENCES mdb_users (UserID),
    FOREIGN KEY (Contactperson) REFERENCES mdb_users (UserID),
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
    ID            bigint                 NOT NULL DEFAULT nextval(mdb_tables_seq),
    tDBID         bigint                 NOT NULL,
    internal_name character varying(255) NOT NULL,
    topic         character varying(255) NOT NULL,
    tName         VARCHAR(50),
    tDescription  TEXT,
    NumCols       INTEGER,
    NumRows       INTEGER,
    separator     CHAR(1),
    quote         CHAR(1),
    element_null  VARCHAR(50),
    skip_lines    BIGINT,
    element_true  VARCHAR(50),
    element_false VARCHAR(50),
    Version       TEXT,
    created       timestamp              NOT NULL DEFAULT NOW(),
    created_by    bigint                 NOT NULL,
    last_modified timestamp,
    PRIMARY KEY (tDBID, ID),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (tDBID) REFERENCES mdb_databases (id)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS
(
    ID               bigint       NOT NULL DEFAULT nextval(mdb_columns_seq),
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
    PRIMARY KEY (cDBID, tID, ID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_ENUMS
(
    ID            bigint                 NOT NULL DEFAULT nextval(mdb_columns_enum_seq),
    eDBID         bigint                 NOT NULL,
    tID           bigint                 NOT NULL,
    cID           bigint                 NOT NULL,
    enum_values   CHARACTER VARYING(255) NOT NULL,
    created       timestamp              NOT NULL DEFAULT NOW(),
    last_modified timestamp,
    FOREIGN KEY (eDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (ID, eDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_nom
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    maxlength     INTEGER,
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
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
--    Histogram     INTEGER[],
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_COLUMNS_cat
(
    cDBID         bigint,
    tID           bigint,
    cID           bigint,
    num_cat       INTEGER,
--    cat_array     TEXT[],
    last_modified timestamp,
    created       timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_concepts
(
    URI        TEXT,
    name       TEXT,
    created    timestamp NOT NULL DEFAULT NOW(),
    created_by bigint,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (URI)
);

CREATE TABLE IF NOT EXISTS mdb_columns_concepts
(
    cDBID   bigint    NOT NULL,
    tID     bigint    NOT NULL,
    cID     bigint    NOT NULL,
    URI     TEXT REFERENCES mdb_concepts (URI),
    created timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cDBID, tID, cID) REFERENCES mdb_COLUMNS (cDBID, tID, ID),
    PRIMARY KEY (cDBID, tID, cID)
);

CREATE TABLE IF NOT EXISTS mdb_VIEW
(
    id            bigint       NOT NULL DEFAULT nextval(mdb_view_seq),
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
    deleted       timestamp,
    created_by    bigint       NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    FOREIGN KEY (vdbid) REFERENCES mdb_databases (id),
    PRIMARY KEY (vdbid, id)
);

CREATE TABLE IF NOT EXISTS mdb_identifiers
(
    id                bigint                                        DEFAULT nextval(mdb_identifiers_seq),
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
    deleted           timestamp,
    PRIMARY KEY (id), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (cid) REFERENCES mdb_containers (id),
    FOREIGN KEY (dbid) REFERENCES mdb_databases (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    UNIQUE (cid, dbid, qid)
);

CREATE TABLE IF NOT EXISTS mdb_related_identifiers
(
    id            bigint             DEFAULT nextval(mdb_related_identifiers_seq),
    iid           bigint    NOT NULL,
    value         text      NOT NULL,
    type          varchar(255),
    relation      varchar(255),
    created       timestamp NOT NULL DEFAULT NOW(),
    created_by    bigint    NOT NULL,
    last_modified timestamp,
    deleted       timestamp,
    PRIMARY KEY (id, iid), /* must be a single id from persistent identifier concept */
    FOREIGN KEY (iid) REFERENCES mdb_identifiers (id),
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID)
);

CREATE TABLE IF NOT EXISTS mdb_creators
(
    id            bigint                DEFAULT nextval(mdb_creators_seq),
    pid           bigint       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    affiliation   VARCHAR(255),
    orcid         VARCHAR(255),
    created       timestamp    NOT NULL DEFAULT NOW(),
    created_by    bigint       NOT NULL,
    last_modified timestamp    NOT NULL,
    FOREIGN KEY (created_by) REFERENCES mdb_users (UserID),
    PRIMARY KEY (id, pid),
    FOREIGN KEY (pid) REFERENCES mdb_identifiers (id)
);

CREATE TABLE IF NOT EXISTS mdb_views_databases
(
    mdb_view_id  bigint,
    databases_id bigint REFERENCES mdb_databases (id),
    created      timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (mdb_view_id, databases_id) REFERENCES mdb_VIEW (id, vdbid),
    PRIMARY KEY (mdb_view_id, databases_id)
);

CREATE TABLE IF NOT EXISTS mdb_feed
(
    fDBID   bigint,
    fID     bigint,
    fUserId INTEGER REFERENCES mdb_users (UserID),
    fDataID INTEGER REFERENCES mdb_data (ID),
    created timestamp NOT NULL DEFAULT NOW(),
    FOREIGN KEY (fDBID, fID) REFERENCES mdb_tables (tDBID, ID),
    PRIMARY KEY (fDBID, fID, fUserId, fDataID)
);

CREATE TABLE IF NOT EXISTS mdb_update
(
    uUserID INTEGER REFERENCES mdb_users (UserID),
    uDBID   bigint REFERENCES mdb_databases (id),
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (uUserID, uDBID)
);

CREATE TABLE IF NOT EXISTS mdb_access
(
    aUserID  INTEGER REFERENCES mdb_users (UserID),
    aDBID    bigint REFERENCES mdb_databases (id),
    attime   TIMESTAMP,
    download BOOLEAN,
    created  timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (aUserID, aDBID)
);

CREATE TABLE IF NOT EXISTS mdb_have_access
(
    hUserID bigint REFERENCES mdb_users (UserID),
    hDBID   bigint REFERENCES mdb_databases (id),
    hType ENUM('R', 'W'),
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (hUserID, hDBID)
);

CREATE TABLE IF NOT EXISTS mdb_owns
(
    oUserID INTEGER REFERENCES mdb_users (UserID),
    oDBID   bigint REFERENCES mdb_databases (ID),
    created timestamp NOT NULL DEFAULT NOW(),
    PRIMARY KEY (oUserID, oDBID)
);

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
