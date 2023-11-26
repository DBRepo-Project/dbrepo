package at.tuwien.converters;

import at.tuwien.entities.identifier.NameIdentifierSchemeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierNameIdentifierSchemeTypeConverter implements AttributeConverter<NameIdentifierSchemeType, String> {

    @Override
    public String convertToDatabaseColumn(NameIdentifierSchemeType nameIdentifierSchemeType) {
        if (nameIdentifierSchemeType == null) {
            return null;
        }
        return nameIdentifierSchemeType.name()
                .toLowerCase();
    }

    @Override
    public NameIdentifierSchemeType convertToEntityAttribute(String nameIdentifierSchemeType) {
        if (nameIdentifierSchemeType == null) {
            return null;
        }
        return NameIdentifierSchemeType.valueOf(nameIdentifierSchemeType.toUpperCase());
    }
}
