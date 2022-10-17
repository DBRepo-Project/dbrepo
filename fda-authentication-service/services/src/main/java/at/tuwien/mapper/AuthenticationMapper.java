package at.tuwien.mapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationMapper.class);

    default String authorizationToTokenHash(String authorization) {
        final String[] parts = authorization.split(" ");
        log.trace("authorization was split into parts: {}", List.of(parts));
        final String token = parts[1];
        log.trace("extracted token {} (only first 10 letters)", token.substring(0, 10));
        return DigestUtils.sha256Hex(token);
    }

}
