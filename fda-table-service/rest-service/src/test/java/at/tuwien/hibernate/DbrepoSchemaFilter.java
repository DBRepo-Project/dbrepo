package at.tuwien.hibernate;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * Do not create table for class {@link at.tuwien.entities.database.table.columns.concepts.Concept} when using JUnit test
 */
public class DbrepoSchemaFilter implements SchemaFilter {

    public static final DbrepoSchemaFilter INSTANCE = new DbrepoSchemaFilter();

    @Override
    public boolean includeNamespace(Namespace namespace) {
        return true;
    }

    @Override
    public boolean includeTable(Table table) {
        return !table.getName().matches("mdb_concepts");
    }

    @Override
    public boolean includeSequence(Sequence sequence) {
        return true;
    }
}
