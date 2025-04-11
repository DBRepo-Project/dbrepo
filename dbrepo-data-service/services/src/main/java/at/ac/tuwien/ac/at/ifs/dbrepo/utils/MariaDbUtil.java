package at.ac.tuwien.ac.at.ifs.dbrepo.utils;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;

import java.util.List;

public class MariaDbUtil {

    /**
     * https://mariadb.com/kb/en/string-data-types/
     */
    final static List<ColumnTypeDto> stringDataTypes = List.of(
            ColumnTypeDto.BINARY,
            ColumnTypeDto.VARBINARY,
            ColumnTypeDto.TINYBLOB,
            ColumnTypeDto.MEDIUMBLOB,
            ColumnTypeDto.LONGBLOB,
            ColumnTypeDto.BLOB,
            ColumnTypeDto.CHAR,
            ColumnTypeDto.VARCHAR,
            ColumnTypeDto.ENUM,
            ColumnTypeDto.SET,
            ColumnTypeDto.TINYTEXT,
            ColumnTypeDto.MEDIUMTEXT,
            ColumnTypeDto.LONGTEXT,
            ColumnTypeDto.TEXT);

    /**
     * https://mariadb.com/kb/en/numeric-data-type-overview/
     */
    final public static List<ColumnTypeDto> numericDataTypes = List.of(
            ColumnTypeDto.TINYINT,
            ColumnTypeDto.BOOL,
            ColumnTypeDto.SMALLINT,
            ColumnTypeDto.MEDIUMINT,
            ColumnTypeDto.INT,
            ColumnTypeDto.BIGINT,
            ColumnTypeDto.DECIMAL,
            ColumnTypeDto.FLOAT,
            ColumnTypeDto.DOUBLE,
            ColumnTypeDto.BIT);

    /**
     * https://mariadb.com/kb/en/date-and-time-data-types/
     */
    final static List<ColumnTypeDto> dateDataTypes = List.of(ColumnTypeDto.DATE,
            ColumnTypeDto.DATETIME,
            ColumnTypeDto.TIME,
            ColumnTypeDto.TIMESTAMP,
            ColumnTypeDto.YEAR);

    public static boolean needValueQuotes(ColumnTypeDto columnType) {
        return stringDataTypes.contains(columnType) || dateDataTypes.contains(columnType);
    }

}
