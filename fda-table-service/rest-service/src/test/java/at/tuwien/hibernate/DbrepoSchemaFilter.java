package at.tuwien.hibernate;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;

import java.util.List;

/**
 * Do not create table for class {@link at.tuwien.entities.database.table.columns.concepts.ColumnConcept} when using JUnit test
 */
public class DbrepoSchemaFilter implements SchemaFilter {

    public static final DbrepoSchemaFilter INSTANCE = new DbrepoSchemaFilter();

    @Override
    public boolean includeNamespace(Namespace namespace) {
        return true;
    }

    @Override
    public boolean includeTable(Table table) {
        final List<String> exclude = List.of("mdb_units", "mdb_concepts");
        if (table.getSchema().matches("fda") && exclude.contains(table.getName())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean includeSequence(Sequence sequence) {
        return true;
    }
}
