package at.tuwien.converters;

import at.tuwien.entities.identifier.RelatedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierRelatedTypeConverter implements AttributeConverter<RelatedType, String> {

    @Override
    public String convertToDatabaseColumn(RelatedType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public RelatedType convertToEntityAttribute(String columnType) {
        return RelatedType.valueOf(columnType.toUpperCase());
    }
}
