package at.tuwien.converters;

import at.tuwien.entities.database.LanguageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LanguageTypeConverter implements AttributeConverter<LanguageType, String> {

    @Override
    public String convertToDatabaseColumn(LanguageType languageType) {
        if (languageType == null) {
            return null;
        }
        return languageType.name()
                .toLowerCase();
    }

    @Override
    public LanguageType convertToEntityAttribute(String languageType) {
        if (languageType == null) {
            return null;
        }
        return LanguageType.valueOf(languageType.toUpperCase());
    }
}
