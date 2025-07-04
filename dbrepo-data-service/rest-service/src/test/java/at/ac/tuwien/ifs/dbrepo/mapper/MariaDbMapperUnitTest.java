package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryStoreInsertException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MariaDbMapperUnitTest extends BaseTest {

    @Autowired
    private MariaDbMapper mariaDbMapper;

    public static Stream<Arguments> nameToInternalName_parameters() {
        return Stream.of(
                Arguments.arguments("dash_minus", "OE/NO-027", "oe_no_027"),
                Arguments.arguments("percent", "OE%NO-027", "oe_no_027"),
                Arguments.arguments("umlaut", "OE/NÖ-027", "oe_no__027"),
                Arguments.arguments("dot", "OE.NO-027", "oe_no_027"),
                Arguments.arguments("double_dot", "OE:NO-027", "oe_no_027")
        );
    }

    public static Stream<Arguments> normalizeQuery_parameters() {
        return Stream.of(
                Arguments.arguments("simple", "select `id` from `some_table`", Instant.ofEpochSecond(1751363087),
                        "select `id` from `some_table` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000'"),
                Arguments.arguments("simple_order", "select `id` from `some_table` order by `id` desc", Instant.ofEpochSecond(1751363087),
                        "select `id` from `some_table` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' order by `id` desc")
        );
    }

    @ParameterizedTest
    @MethodSource("nameToInternalName_parameters")
    public void nameToInternalName_succeeds(String name, String input, String expected) {

        /* test */
        assertEquals(expected, mariaDbMapper.nameToInternalName(input));
    }

    @ParameterizedTest
    @MethodSource("normalizeQuery_parameters")
    public void normalizeQuery_succeeds(String name, String query, Instant timestamp, String expected)
            throws QueryStoreInsertException, QueryMalformedException {

        /* test */
        assertEquals(expected, mariaDbMapper.normalizeQuery(query, timestamp));
    }

}
