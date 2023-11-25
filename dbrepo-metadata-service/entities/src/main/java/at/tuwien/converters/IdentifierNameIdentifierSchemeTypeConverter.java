package at.tuwien.converters;

import at.tuwien.entities.identifier.NameIdentifierSchemeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierNameIdentifierSchemeTypeConverter implements AttributeConverter<NameIdentifierSchemeType, String> {

    @Override
    public String convertToDatabaseColumn(NameIdentifierSchemeType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public NameIdentifierSchemeType convertToEntityAttribute(String columnType) {
        return NameIdentifierSchemeType.valueOf(columnType.toUpperCase());
    }
}
