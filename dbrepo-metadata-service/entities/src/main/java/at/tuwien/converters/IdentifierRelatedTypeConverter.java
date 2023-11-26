package at.tuwien.converters;

import at.tuwien.entities.identifier.RelatedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierRelatedTypeConverter implements AttributeConverter<RelatedType, String> {

    @Override
    public String convertToDatabaseColumn(RelatedType relatedType) {
        if (relatedType == null) {
            return null;
        }
        return relatedType.name()
                .toLowerCase();
    }

    @Override
    public RelatedType convertToEntityAttribute(String relatedType) {
        if (relatedType == null) {
            return null;
        }
        return RelatedType.valueOf(relatedType.toUpperCase());
    }
}
