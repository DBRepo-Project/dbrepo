package at.tuwien.utils;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MariaDbUtilTest {

    @Test
    public void needValueQuotes_succeeds() {
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.TINYBLOB));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.MEDIUMBLOB));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.LONGBLOB));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.BLOB));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.CHAR));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.VARCHAR));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.ENUM));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.SET));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.TINYTEXT));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.MEDIUMTEXT));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.LONGTEXT));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.TEXT));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.BINARY));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.VARBINARY));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.DATETIME));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.DATE));
        assertTrue(MariaDbUtil.needValueQuotes(ColumnTypeDto.TIMESTAMP));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.INT));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.TINYINT));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.MEDIUMINT));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.DOUBLE));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.DECIMAL));
        assertFalse(MariaDbUtil.needValueQuotes(ColumnTypeDto.BOOL));
    }
}
