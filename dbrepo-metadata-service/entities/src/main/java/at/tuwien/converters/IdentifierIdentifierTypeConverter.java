package at.tuwien.converters;

import at.tuwien.entities.identifier.IdentifierType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierIdentifierTypeConverter implements AttributeConverter<IdentifierType, String> {

    @Override
    public String convertToDatabaseColumn(IdentifierType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public IdentifierType convertToEntityAttribute(String columnType) {
        return IdentifierType.valueOf(columnType.toUpperCase());
    }
}
