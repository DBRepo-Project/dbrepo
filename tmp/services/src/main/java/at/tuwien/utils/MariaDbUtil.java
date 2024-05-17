package at.tuwien.utils;

import at.tuwien.api.database.table.columns.ColumnTypeDto;

import java.util.List;

public class MariaDbUtil {

    /**
     * https://mariadb.com/kb/en/string-data-types/
     */
    final static List<ColumnTypeDto> stringDataTypes = List.of(ColumnTypeDto.BINARY,
            ColumnTypeDto.BLOB,
            ColumnTypeDto.CHAR,
            ColumnTypeDto.ENUM,
            ColumnTypeDto.MEDIUMBLOB,
            ColumnTypeDto.LONGBLOB,
            ColumnTypeDto.LONGTEXT,
            ColumnTypeDto.TEXT,
            ColumnTypeDto.TINYTEXT,
            ColumnTypeDto.SET);

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
