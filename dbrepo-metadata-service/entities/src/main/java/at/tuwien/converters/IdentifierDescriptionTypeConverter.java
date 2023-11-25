package at.tuwien.converters;

import at.tuwien.entities.identifier.DescriptionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierDescriptionTypeConverter implements AttributeConverter<DescriptionType, String> {

    @Override
    public String convertToDatabaseColumn(DescriptionType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public DescriptionType convertToEntityAttribute(String columnType) {
        return DescriptionType.valueOf(columnType.toUpperCase());
    }
}
