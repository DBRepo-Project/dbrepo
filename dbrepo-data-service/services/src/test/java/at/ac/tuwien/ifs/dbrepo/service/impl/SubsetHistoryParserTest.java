package at.ac.tuwien.ifs.dbrepo.service.impl;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SubsetHistoryParserTest {
    @Test void valuesQueriesAreReadOnlySelectionsWithoutTableBindings() {
        final String sql = SubsetHistory.rewrite("VALUES (1, 2, 3)", "example", name -> {
            fail("VALUES must not reference a table: " + name);
            return "";
        });
        assertTrue(sql.startsWith("select "), sql);
        assertTrue(sql.contains("1 as"), sql);
    }

    @Test void writeStatementsCannotEnterTheHistoricalReadPath() {
        for (String sql : List.of("DELETE FROM example.first", "UPDATE example.first SET value=0",
                "DROP TABLE example.first", "INSERT INTO example.first(value) VALUES(1)")) {
            assertThrows(org.jooq.impl.ParserException.class,
                    () -> SubsetHistory.rewrite(sql, "example", name -> "select 1"), sql);
        }
    }

    @Test void rewritesJoinsAliasesAndQualifiedColumnsWithoutEditingLiterals() {
        final List<String> visited = new ArrayList<>();
        final String sql = SubsetHistory.rewrite("select a.value,b.value from example.first a join example.second b"
                + " on a.pk=b.pk where a.value <> 'example.first'", "example", name -> {
            visited.add(name); return "select pk,value from historical_" + name;
        });
        assertEquals(List.of("first", "second"), visited, sql);
        assertTrue(sql.contains("historical_first"), sql);
        assertTrue(sql.contains("historical_second"), sql);
        assertTrue(sql.contains("'example.first'"), sql);
        assertFalse(sql.contains("as `first` as `a`"), sql);
    }

    @Test void rewritesNestedSelectsAndFullyQualifiedFields() {
        final String sql = SubsetHistory.rewrite("select example.first.value from example.first"
                + " where pk in (select pk from second)", "example", name -> "select pk,value from historical_" + name);
        assertFalse(sql.contains("`example`.`first`.`value`"), sql);
        assertTrue(sql.contains("historical_first"), sql);
        assertTrue(sql.contains("historical_second"), sql);
    }

    @Test void fullyQualifiedAsterisksReferToTheRewrittenAlias() {
        final String sql = SubsetHistory.rewrite("select example.first.* from example.first", "example",
                name -> "select pk,value from historical_" + name);
        assertFalse(sql.contains("`example`.`first`"), sql);
        assertTrue(sql.contains("`first`.*"), sql);
    }
}
