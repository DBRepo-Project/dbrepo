package at.tuwien.converters;

import at.tuwien.entities.identifier.AffiliationIdentifierSchemeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class IdentifierAffiliationIdentifierSchemeTypeConverter implements AttributeConverter<AffiliationIdentifierSchemeType, String> {

    @Override
    public String convertToDatabaseColumn(AffiliationIdentifierSchemeType affiliationIdentifierSchemeType) {
        if (affiliationIdentifierSchemeType == null) {
            return null;
        }
        return affiliationIdentifierSchemeType.name()
                .toLowerCase();
    }

    @Override
    public AffiliationIdentifierSchemeType convertToEntityAttribute(String affiliationIdentifierSchemeType) {
        if (affiliationIdentifierSchemeType == null) {
            return null;
        }
        return AffiliationIdentifierSchemeType.valueOf(affiliationIdentifierSchemeType.toUpperCase());
    }
}
