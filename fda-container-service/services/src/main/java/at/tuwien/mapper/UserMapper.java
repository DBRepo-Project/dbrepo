package at.tuwien.mapper;

import at.tuwien.api.auth.TokenIntrospectDto;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import at.tuwien.entities.user.UserAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);

    /* keep */
    @Mappings({
            @Mapping(target = "orcid", expression = "java(data.getAttributes().stream().filter(a -> a.getName().equals(\"orcid\")).findFirst().get().getValue())")
    })
    UserBriefDto userToUserBriefDto(User data);

    /* keep */
    @Mappings({
            @Mapping(target = "orcid", expression = "java(data.getAttributes().stream().filter(a -> a.getName().equals(\"orcid\")).findFirst().get().getValue())")
    })
    UserDto userToUserDto(User data);

    UserDetailsDto userBriefDtoToUserDetailsDto(UserBriefDto data);

    default UserDetailsDto tokenIntrospectDtoToUserDetailsDto(TokenIntrospectDto data) {
        return UserDetailsDto.builder()
                .id(data.getSub())
                .username(data.getUsername())
                .authorities(Arrays.stream(data.getRealmAccess().getRoles())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
    }

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        final GrantedAuthority authority = new SimpleGrantedAuthority(data.getAuthority());
        log.trace("mapped granted authority {} to granted authority {}", data, authority);
        return authority;
    }
}
