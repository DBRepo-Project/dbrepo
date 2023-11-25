package at.tuwien.converters;

import at.tuwien.entities.identifier.NameType;
import at.tuwien.entities.identifier.RelatedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierNameTypeConverter implements AttributeConverter<NameType, String> {

    @Override
    public String convertToDatabaseColumn(NameType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public NameType convertToEntityAttribute(String columnType) {
        return NameType.valueOf(columnType.toUpperCase());
    }
}
