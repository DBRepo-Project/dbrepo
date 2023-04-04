package at.tuwien.converters;

import at.tuwien.api.identifier.IdentifierTypeDto;
import org.springframework.core.convert.converter.Converter;

public class IdentifierTypeConverter implements Converter<String, IdentifierTypeDto> {

    @Override
    public IdentifierTypeDto convert(String source) {
        return IdentifierTypeDto.valueOf(source.toUpperCase());
    }
}
