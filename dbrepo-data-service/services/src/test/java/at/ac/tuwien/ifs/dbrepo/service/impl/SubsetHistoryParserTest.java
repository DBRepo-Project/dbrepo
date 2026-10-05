package at.ac.tuwien.ifs.dbrepo.service.impl;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SubsetHistoryParserTest {
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
}
