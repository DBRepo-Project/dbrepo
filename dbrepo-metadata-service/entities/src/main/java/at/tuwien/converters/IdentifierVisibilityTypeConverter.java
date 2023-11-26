package at.tuwien.converters;

import at.tuwien.entities.identifier.VisibilityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierVisibilityTypeConverter implements AttributeConverter<VisibilityType, String> {

    @Override
    public String convertToDatabaseColumn(VisibilityType visibilityType) {
        if (visibilityType == null) {
            return null;
        }
        return visibilityType.name()
                .toLowerCase();
    }

    @Override
    public VisibilityType convertToEntityAttribute(String visibilityType) {
        if (visibilityType == null) {
            return null;
        }
        return VisibilityType.valueOf(visibilityType.toUpperCase());
    }
}
