package at.tuwien.converters;

import at.tuwien.entities.identifier.VisibilityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierVisibilityTypeConverter implements AttributeConverter<VisibilityType, String> {

    @Override
    public String convertToDatabaseColumn(VisibilityType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public VisibilityType convertToEntityAttribute(String columnType) {
        return VisibilityType.valueOf(columnType.toUpperCase());
    }
}
