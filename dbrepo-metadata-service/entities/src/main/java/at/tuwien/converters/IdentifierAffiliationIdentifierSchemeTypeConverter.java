package at.tuwien.converters;

import at.tuwien.entities.identifier.AffiliationIdentifierSchemeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierAffiliationIdentifierSchemeTypeConverter implements AttributeConverter<AffiliationIdentifierSchemeType, String> {

    @Override
    public String convertToDatabaseColumn(AffiliationIdentifierSchemeType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public AffiliationIdentifierSchemeType convertToEntityAttribute(String columnType) {
        return AffiliationIdentifierSchemeType.valueOf(columnType.toUpperCase());
    }
}
