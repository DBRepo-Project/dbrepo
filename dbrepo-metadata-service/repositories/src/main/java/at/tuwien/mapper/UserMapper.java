package at.tuwien.mapper;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.auth.TokenIntrospectDto;
import at.tuwien.api.keycloak.*;
import at.tuwien.api.user.*;
import at.tuwien.api.user.UserAttributesDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);

    @Mappings({
            @Mapping(target = "id", expression = "java(data.getId().toString())")
    })
    UserDetailsDto userBriefDtoToUserDetailsDto(UserBriefDto data);

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        final GrantedAuthority authority = new SimpleGrantedAuthority(data.getAuthority());
        log.trace("mapped granted authority {} to granted authority {}", data, authority);
        return authority;
    }

    default UpdateCredentialsDto passwordToUpdateCredentialsDto(String password) {
        return UpdateCredentialsDto.builder()
                .credentials(List.of(CredentialDto.builder()
                        .temporary(false)
                        .type(CredentialTypeDto.PASSWORD)
                        .value(password)
                        .build()))
                .build();
    }

    default UserCreateDto signupRequestDtoToUserCreateDto(SignupRequestDto data) {
        return UserCreateDto.builder()
                .username(data.getUsername())
                .email(data.getEmail())
                .credentials(List.of(CredentialDto.builder()
                        .type(CredentialTypeDto.PASSWORD)
                        .temporary(false)
                        .value(data.getPassword())
                        .build()))
                .enabled(true)
                .build();
    }

    /* keep */
    UserBriefDto keycloakUserDtoToUserBriefDto(at.tuwien.api.keycloak.UserDto data);

    /* keep */
    @Mappings({
            @Mapping(target = "id", expression = "java(data.getId().toString())")
    })
    UserDetailsDto userDtoToUserDetailsDto(UserDto data);

    /* keep */
    UserBriefDto userToUserBriefDto(User data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    /* keep */
    UserDto userToUserDto(User data);

    default UserDetailsDto tokenIntrospectDtoToUserDetailsDto(TokenIntrospectDto data) {
        return UserDetailsDto.builder()
                .id(data.getSub())
                .username(data.getUsername())
                .authorities(Arrays.stream(data.getRealmAccess().getRoles())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
    }

    User signupRequestDtoToUser(SignupRequestDto data);

}
