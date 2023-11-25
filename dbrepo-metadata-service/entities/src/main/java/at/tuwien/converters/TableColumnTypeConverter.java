package at.tuwien.converters;

import at.tuwien.entities.database.table.columns.TableColumnType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TableColumnTypeConverter implements AttributeConverter<TableColumnType, String> {

    @Override
    public String convertToDatabaseColumn(TableColumnType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public TableColumnType convertToEntityAttribute(String columnType) {
        return TableColumnType.valueOf(columnType.toUpperCase());
    }
}
