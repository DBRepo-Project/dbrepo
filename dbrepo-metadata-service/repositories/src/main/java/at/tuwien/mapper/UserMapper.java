package at.tuwien.mapper;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.auth.TokenIntrospectDto;
import at.tuwien.api.keycloak.*;
import at.tuwien.api.user.*;
import at.tuwien.api.user.UserAttributesDto;
import at.tuwien.api.user.UserDto;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
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

    @Mappings({
            @Mapping(target = "attributes", expression = "java(data)")
    })
    UpdateAttributesDto userAttributesDtoToUpdateAttributesDto(at.tuwien.api.keycloak.UserAttributesDto data);

    default UpdateCredentialsDto passwordToUpdateCredentialsDto(String password) {
        return UpdateCredentialsDto.builder()
                .credentials(List.of(CredentialDto.builder()
                        .temporary(false)
                        .type(CredentialTypeDto.PASSWORD)
                        .value(password)
                        .build()))
                .build();
    }

    default at.tuwien.api.keycloak.UserAttributesDto userUpdateDtoToUserAttributesDto(UserUpdateDto data) {
        return at.tuwien.api.keycloak.UserAttributesDto.builder()
                .orcid(List.of(data.getOrcid()))
                .affiliation(List.of(data.getAffiliation()))
                .build();
    }

    default at.tuwien.api.keycloak.UserAttributesDto userThemeSetDtoToUserAttributesDto(UserThemeSetDto data) {
        return at.tuwien.api.keycloak.UserAttributesDto.builder()
                .themeDark(List.of(String.valueOf(data.getThemeDark())))
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
                .attributes(at.tuwien.api.keycloak.UserAttributesDto.builder()
                        .themeDark(List.of("false"))
                        .mariadbPassword(List.of("*" + DigestUtils.sha1Hex(DigestUtils.sha1(
                                data.getPassword().getBytes(StandardCharsets.UTF_8))).toUpperCase()))
                        .affiliation(List.of())
                        .orcid(List.of())
                        .build())
                .build();
    }

    /* keep */
    UserBriefDto keycloakUserDtoToUserBriefDto(at.tuwien.api.keycloak.UserDto data);

    /* keep */
    UserDto keycloakUserDtoToUserDto(at.tuwien.api.keycloak.UserDto data);

    /* keep */
    default UserAttributesDto map(at.tuwien.api.keycloak.UserAttributesDto data) {
        return UserAttributesDto.builder()
                .themeDark(Boolean.getBoolean(data.getThemeDark().get(0)))
                .orcid(data.getOrcid().get(0))
                .affiliation(data.getAffiliation().get(0))
                .build();
    }

    /* keep */
    @Mappings({
            @Mapping(target = "id", expression = "java(data.getId().toString())")
    })
    UserDetailsDto userDtoToUserDetailsDto(UserDto data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    default UserDetailsDto tokenIntrospectDtoToUserDetailsDto(TokenIntrospectDto data) {
        return UserDetailsDto.builder()
                .id(data.getSub())
                .username(data.getUsername())
                .authorities(Arrays.stream(data.getRealmAccess().getRoles())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()))
                .build();
    }

}
