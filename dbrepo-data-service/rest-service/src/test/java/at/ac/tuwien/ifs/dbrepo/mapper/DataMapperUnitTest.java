package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockFileDatabase;
import org.jooq.tools.jdbc.MockStatement;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DataMapperUnitTest extends BaseTest {

    @Autowired
    private DataMapper dataMapper;

    public static Stream<Arguments> incompatible_parameters() {
        return Stream.of(
                Arguments.arguments("INTERVAL"),
                Arguments.arguments("SQLNULL"),
                Arguments.arguments("HUGEINT"),
                Arguments.arguments("UHUGEINT")
        );
    }

    public static Stream<Arguments> compatible_parameters() {
        return Stream.of(
                Arguments.arguments("UBIGINT", ColumnTypeDto.BIGINT),
                Arguments.arguments("TIMESTAMP_MS", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("TIMESTAMP_NS", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("TIMESTAMP_S", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("TIMESTAMP_TZ", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("TIMESTAMP WITHOUT TIME ZONE", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("TIMESTAMP WITH TIME ZONE", ColumnTypeDto.TIMESTAMP),
                Arguments.arguments("UUID", ColumnTypeDto.VARCHAR),
                Arguments.arguments("TIME_TZ", ColumnTypeDto.TIME),
                Arguments.arguments("INTEGER", ColumnTypeDto.INT),
                Arguments.arguments("UINTEGER", ColumnTypeDto.INT),
                Arguments.arguments("SMALLINT", ColumnTypeDto.SMALLINT),
                Arguments.arguments("USMALLINT", ColumnTypeDto.SMALLINT),
                Arguments.arguments("TINYINT", ColumnTypeDto.TINYINT),
                Arguments.arguments("UTINYINT", ColumnTypeDto.TINYINT),
                Arguments.arguments("BOOLEAN", ColumnTypeDto.BOOL)
        );
    }

    public static Stream<Arguments> decimal_parameters() {
        return Stream.of(
                Arguments.arguments("1.753426267664687E9")
        );
    }

    @ParameterizedTest
    @MethodSource("incompatible_parameters")
    public void duckDbDataTypeToMariaDbColumnTypeDto_fails(String data) {

        /* test */
        assertNull(dataMapper.duckDbDataTypeToMariaDbColumnTypeDto(data));
    }

    @ParameterizedTest
    @MethodSource("compatible_parameters")
    public void duckDbDataTypeToMariaDbColumnTypeDto_succeeds(String data, ColumnTypeDto expected) {

        /* test */
        assertEquals(expected, dataMapper.duckDbDataTypeToMariaDbColumnTypeDto(data));
    }

    @ParameterizedTest
    @MethodSource("decimal_parameters")
    public void prepareStatementWithColumnTypeObject_succeeds(Object data) throws IOException,
            SQLException {
        final MockFileDatabase database = new MockFileDatabase("junit.db");
        final PreparedStatement preparedStatement = new MockStatement(new MockConnection(database), database);

        /* test */
        dataMapper.prepareStatementWithColumnTypeObject(preparedStatement, ColumnTypeDto.DECIMAL, 1, data);
    }

}
