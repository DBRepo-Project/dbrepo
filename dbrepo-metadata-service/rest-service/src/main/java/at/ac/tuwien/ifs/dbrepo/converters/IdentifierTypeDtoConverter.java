package at.ac.tuwien.ifs.dbrepo.converters;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IdentifierTypeDtoConverter implements Converter<String, IdentifierTypeDto> {

    @Override
    public IdentifierTypeDto convert(String source) {
        return IdentifierTypeDto.valueOf(source.toUpperCase());
    }
}
