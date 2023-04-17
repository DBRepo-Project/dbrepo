package at.tuwien.mapper;

import org.mapstruct.Mapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Collections;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface AuthenticationMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationMapper.class);

    default MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED));
        return mappingJackson2HttpMessageConverter;
    }
}
