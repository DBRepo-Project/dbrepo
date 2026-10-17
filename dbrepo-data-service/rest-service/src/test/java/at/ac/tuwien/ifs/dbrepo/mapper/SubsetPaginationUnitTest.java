package at.ac.tuwien.ifs.dbrepo.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubsetPaginationUnitTest {
    private final MariaDbMapper mapper = Mappers.getMapper(MariaDbMapper.class);

    @Test
    void paginationStaysWithinTheStoredQueryBounds() {
        final String query = "SELECT id FROM samples ORDER BY id DESC LIMIT 10 OFFSET 2";
        assertEquals("SELECT id FROM samples ORDER BY id DESC LIMIT 3 OFFSET 8", mapper.paginateSubset(query, 2L, 3L));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "LIMIT 2 | 0 | 10 | LIMIT 2 OFFSET 0",
            "LIMIT 2 | 1 | 10 | LIMIT 0 OFFSET 2",
            "LIMIT 1, 2 | 1 | 1 | LIMIT 1 OFFSET 2",
            "LIMIT 2 OFFSET 5 | 2 | 1 | LIMIT 0 OFFSET 7",
            "LIMIT 18446744073709551615 | 0 | 10 | LIMIT 10 OFFSET 0"
    })
    void paginationCombinesRatherThanReplacesBounds(String original, long page, long size, String expected) {
        assertEquals("SELECT id FROM samples " + expected,
                mapper.paginateSubset("SELECT id FROM samples " + original, page, size));
    }

    @Test
    void nestedLimitsQuotedKeywordsAndCommentsAreNotOuterBounds() {
        final String query = "WITH s AS (SELECT 'LIMIT 9' AS `LIMIT` FROM samples LIMIT 3) "
                + "SELECT `LIMIT` FROM s /* LIMIT 4 */ ORDER BY `LIMIT` -- LIMIT 5";
        assertEquals(query + "\nLIMIT 2 OFFSET 2", mapper.paginateSubset(query, 1L, 2L));
    }

    @Test
    void paginationKeepsTrailingTerminatorsAndComments() {
        final String query = "SELECT id FROM samples ORDER BY id; -- stored query";
        assertEquals("SELECT id FROM samples ORDER BY id\nLIMIT 2 OFFSET 2; -- stored query",
                mapper.paginateSubset(query, 1L, 2L));
        assertEquals("SELECT id FROM samples LIMIT 1 OFFSET 3; -- stored query",
                mapper.paginateSubset("SELECT id FROM samples LIMIT /* bound */ 4; -- stored query", 1L, 3L));
    }

    @Test
    void qualifiedReservedColumnNamesAreNotLimitClauses() {
        final String query = "SELECT s.limit FROM samples s ORDER BY s.limit";
        assertEquals(query + "\nLIMIT 2 OFFSET 0", mapper.paginateSubset(query, 0L, 2L));
    }

    @Test
    void largePageNumbersCannotOverflowIntoEarlierRows() {
        assertEquals("SELECT id FROM samples LIMIT 0 OFFSET 2",
                mapper.paginateSubset("SELECT id FROM samples LIMIT 2", Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals("SELECT id FROM samples\nLIMIT 0 OFFSET 0",
                mapper.paginateSubset("SELECT id FROM samples", Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    void unpaginatedQueriesRemainUnchanged() {
        final String query = "SELECT id FROM samples ORDER BY id LIMIT 10";
        assertEquals(query, mapper.paginateSubset(query, null, null));
        assertEquals(query, mapper.paginateSubset(query, 0L, null));
        assertEquals(query, mapper.paginateSubset(query, null, 10L));
    }
}
