package at.tuwien.mapper;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.auth.CredentialDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import org.mapstruct.Mapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);

    UserDetailsDto userDtoToUserDetailsDto(UserDto data);

    UserDto userToUserDto(User data);

    UserBriefDto userToUserBriefDto(User data);

    default CreateUserDto signupRequestDtoToCreateUserDto(SignupRequestDto data) {
        return CreateUserDto.builder()
                .username(data.getUsername())
                .email(data.getEmail())
                .enabled(true)
                .credentials(List.of(CredentialDto.builder()
                        .temporary(false)
                        .type("password")
                        .value(data.getPassword())
                        .build()))
                .build();
    }

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        final GrantedAuthority authority = new SimpleGrantedAuthority(data.getAuthority());
        log.trace("mapped granted authority {} to granted authority {}", data, authority);
        return authority;
    }
}
