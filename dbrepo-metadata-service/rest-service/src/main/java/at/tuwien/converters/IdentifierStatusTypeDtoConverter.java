package at.tuwien.converters;

import at.tuwien.api.identifier.IdentifierStatusTypeDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IdentifierStatusTypeDtoConverter implements Converter<String, IdentifierStatusTypeDto> {

    @Override
    public IdentifierStatusTypeDto convert(String source) {
        return IdentifierStatusTypeDto.valueOf(source.toUpperCase());
    }
}
