package at.tuwien.converters;

import at.tuwien.entities.identifier.IdentifierFunderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierFunderTypeConverter implements AttributeConverter<IdentifierFunderType, String> {

    @Override
    public String convertToDatabaseColumn(IdentifierFunderType identifierFunderType) {
        if (identifierFunderType == null) {
            return null;
        }
        return identifierFunderType.name()
                .toLowerCase();
    }

    @Override
    public IdentifierFunderType convertToEntityAttribute(String identifierFunderType) {
        if (identifierFunderType == null) {
            return null;
        }
        return IdentifierFunderType.valueOf(identifierFunderType.toUpperCase());
    }
}
