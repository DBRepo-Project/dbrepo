package at.tuwien.converters;

import at.tuwien.entities.identifier.NameType;
import at.tuwien.entities.identifier.RelatedType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierNameTypeConverter implements AttributeConverter<NameType, String> {

    @Override
    public String convertToDatabaseColumn(NameType nameType) {
        if (nameType == null) {
            return null;
        }
        return nameType.name()
                .toLowerCase();
    }

    @Override
    public NameType convertToEntityAttribute(String nameType) {
        if (nameType == null) {
            return null;
        }
        return NameType.valueOf(nameType.toUpperCase());
    }
}
