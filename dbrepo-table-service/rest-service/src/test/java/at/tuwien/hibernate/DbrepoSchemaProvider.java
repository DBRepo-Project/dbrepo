package at.tuwien.hibernate;

import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/* keep */
public class DbrepoSchemaProvider implements SchemaFilterProvider {

    @Override
    public SchemaFilter getCreateFilter() {
        return DbrepoSchemaFilter.INSTANCE;
    }

    @Override
    public SchemaFilter getDropFilter() {
        return DbrepoSchemaFilter.INSTANCE;
    }

    @Override
    public SchemaFilter getMigrateFilter() {
        return DbrepoSchemaFilter.INSTANCE;
    }

    @Override
    public SchemaFilter getValidateFilter() {
        return DbrepoSchemaFilter.INSTANCE;
    }
}
