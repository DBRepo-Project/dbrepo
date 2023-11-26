package at.tuwien.converters;

import at.tuwien.entities.identifier.IdentifierType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierIdentifierTypeConverter implements AttributeConverter<IdentifierType, String> {

    @Override
    public String convertToDatabaseColumn(IdentifierType identifierType) {
        if (identifierType == null) {
            return null;
        }
        return identifierType.name()
                .toLowerCase();
    }

    @Override
    public IdentifierType convertToEntityAttribute(String identifierType) {
        if (identifierType == null) {
            return null;
        }
        return IdentifierType.valueOf(identifierType.toUpperCase());
    }
}
