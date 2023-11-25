package at.tuwien.converters;

import at.tuwien.entities.database.LanguageType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LanguageTypeConverter implements AttributeConverter<LanguageType, String> {

    @Override
    public String convertToDatabaseColumn(LanguageType columnType) {
        return columnType.name()
                .toLowerCase();
    }

    @Override
    public LanguageType convertToEntityAttribute(String columnType) {
        return LanguageType.valueOf(columnType.toUpperCase());
    }
}
