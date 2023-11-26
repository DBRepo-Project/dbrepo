package at.tuwien.converters;

import at.tuwien.entities.identifier.DescriptionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierDescriptionTypeConverter implements AttributeConverter<DescriptionType, String> {

    @Override
    public String convertToDatabaseColumn(DescriptionType descriptionType) {
        if (descriptionType == null) {
            return null;
        }
        return descriptionType.name()
                .toLowerCase();
    }

    @Override
    public DescriptionType convertToEntityAttribute(String descriptionType) {
        if (descriptionType == null) {
            return null;
        }
        return DescriptionType.valueOf(descriptionType.toUpperCase());
    }
}
