package at.tuwien.converters;

import org.springframework.util.StringUtils;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.net.URI;

@Converter(autoApply = true)
public class UriConverter implements AttributeConverter<URI, String> {

    @Override
    public String convertToDatabaseColumn(URI entityValue) {
        return (entityValue == null) ? null : entityValue.toString();
    }

    @Override
    public URI convertToEntityAttribute(String databaseValue) {
        return (StringUtils.hasLength(databaseValue) ? URI.create(databaseValue.trim()) : null);
    }
}
