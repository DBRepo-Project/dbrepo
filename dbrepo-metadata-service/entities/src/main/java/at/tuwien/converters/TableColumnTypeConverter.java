package at.tuwien.converters;

import at.tuwien.entities.database.table.columns.TableColumnType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TableColumnTypeConverter implements AttributeConverter<TableColumnType, String> {

    @Override
    public String convertToDatabaseColumn(TableColumnType tableColumnType) {
        if (tableColumnType == null) {
            return null;
        }
        return tableColumnType.name()
                .toLowerCase();
    }

    @Override
    public TableColumnType convertToEntityAttribute(String tableColumnType) {
        if (tableColumnType == null) {
            return null;
        }
        return TableColumnType.valueOf(tableColumnType.toUpperCase());
    }
}
