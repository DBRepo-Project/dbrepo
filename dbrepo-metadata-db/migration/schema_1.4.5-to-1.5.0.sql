ALTER TABLE mdb_image_types
    DROP SYSTEM VERSIONING;
TRUNCATE mdb_image_types;
ALTER TABLE mdb_image_types
    DROP
        COLUMN hint;
ALTER TABLE mdb_image_types
    ADD COLUMN type_hint TEXT;
ALTER TABLE mdb_image_types
    ADD COLUMN data_hint TEXT;
ALTER TABLE mdb_image_types
    ADD COLUMN is_generated BOOLEAN NOT NULL;
ALTER TABLE mdb_image_types
    ADD SYSTEM VERSIONING;

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
       (1, 'VARCHAR(size)', 'varchar', 0, 65532, 255, true, null, null, null, null, null, null, null,
        null, 'https://mariadb.com/kb/en/varchar/', false, true, false);

ALTER TABLE mdb_related_identifiers
    DROP SYSTEM VERSIONING;
ALTER TABLE mdb_related_identifiers
    MODIFY type ENUM ('DOI','URL','URN','ARK','ARXIV','BIBCODE','EAN13','EISSN','HANDLE','IGSN','ISBN','ISTC','LISSN','LSID','PMID','PURL','UPC','W3ID') NOT NULL;
ALTER TABLE mdb_related_identifiers
    MODIFY relation ENUM ('IS_CITED_BY','CITES','IS_SUPPLEMENT_TO','IS_SUPPLEMENTED_BY','IS_CONTINUED_BY','CONTINUES','IS_DESCRIBED_BY','DESCRIBES','HAS_METADATA','IS_METADATA_FOR','HAS_VERSION','IS_VERSION_OF','IS_NEW_VERSION_OF','IS_PREVIOUS_VERSION_OF','IS_PART_OF','HAS_PART','IS_PUBLISHED_IN','IS_REFERENCED_BY','REFERENCES','IS_DOCUMENTED_BY','DOCUMENTS','IS_COMPILED_BY','COMPILES','IS_VARIANT_FORM_OF','IS_ORIGINAL_FORM_OF','IS_IDENTICAL_TO','IS_REVIEWED_BY','REVIEWS','IS_DERIVED_FROM','IS_SOURCE_OF','IS_REQUIRED_BY','REQUIRES','IS_OBSOLETED_BY','OBSOLETES') NOT NULL;
ALTER TABLE mdb_related_identifiers
    ADD SYSTEM VERSIONING;

CREATE TABLE IF NOT EXISTS `mdb_image_operators`
(
    id
                  SERIAL,
    image_id
                  BIGINT
                               NOT
                                   NULL,
    display_name
                  varchar(255) NOT NULL,
    value         varchar(255) NOT NULL,
    documentation TEXT         NOT NULL,
    PRIMARY KEY
        (
         id
            ),
    FOREIGN KEY
        (
         image_id
            ) REFERENCES `mdb_images`
        (
         `id`
            ),
    UNIQUE
        (
         value
            )
) WITH SYSTEM VERSIONING;

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
       (1, 'IS NOT NULL', 'IS NOT NULL', 'https://mariadb.com/kb/en/is-not-null/'),
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