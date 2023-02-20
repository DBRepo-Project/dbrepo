package at.tuwien.mapper;

import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;

import java.util.Arrays;
import java.util.List;

@Mapper(componentModel = "spring")
public interface AuthenticationMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationMapper.class);

    default String authorizationToTokenHash(String authorization) {
        final List<String> parts = Arrays.asList(authorization.split(" "));
        log.trace("authorization was split into sub-parts (only first 10 letters): {}", parts.stream().map(p -> p.substring(0,10)));
        final String token = parts.get(1);
        log.trace("extracted token {} (only first 10 letters)", token.substring(0, 10));
        return DigestUtils.sha256Hex(token);
    }

}
