package at.tuwien.entities.database.table.columns;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum TableColumnType {
    CHAR,
    VARCHAR,
    BINARY,
    VARBINARY,
    TINYBLOB,
    TINYTEXT,
    TEXT,
    BLOB,
    MEDIUMTEXT,
    MEDIUMBLOB,
    LONGTEXT,
    LONGBLOB,
    ENUM,
    SET,
    SERIAL,
    BIT,
    TINYINT,
    BOOL,
    SMALLINT,
    MEDIUMINT,
    INT,
    BIGINT,
    FLOAT,
    DOUBLE,
    DECIMAL,
    DATE,
    DATETIME,
    TIMESTAMP,
    TIME,
    YEAR;
}