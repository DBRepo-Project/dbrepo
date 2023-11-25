package at.tuwien.converters;

import at.tuwien.entities.identifier.IdentifierFunderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierFunderTypeConverter implements AttributeConverter<IdentifierFunderType, String> {

    @Override
    public String convertToDatabaseColumn(IdentifierFunderType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public IdentifierFunderType convertToEntityAttribute(String columnType) {
        return IdentifierFunderType.valueOf(columnType.toUpperCase());
    }
}
