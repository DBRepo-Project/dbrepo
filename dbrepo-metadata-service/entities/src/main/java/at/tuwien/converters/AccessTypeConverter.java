package at.tuwien.converters;

import at.tuwien.entities.database.AccessType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AccessTypeConverter implements AttributeConverter<AccessType, String> {

    @Override
    public String convertToDatabaseColumn(AccessType accessType) {
        if (accessType == null) {
            return null;
        }
        return accessType.name()
                .toLowerCase();
    }

    @Override
    public AccessType convertToEntityAttribute(String accessType) {
        if (accessType == null) {
            return null;
        }
        return AccessType.valueOf(accessType.toUpperCase());
    }
}
