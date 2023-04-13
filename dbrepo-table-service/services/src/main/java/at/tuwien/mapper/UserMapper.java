package at.tuwien.mapper;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import at.tuwien.entities.user.UserAttribute;
import org.mapstruct.Mapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


@Mapper(componentModel = "spring")
public interface UserMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserMapper.class);

    UserDetailsDto userDtoToUserDetailsDto(UserDto data);

    UserDto userToUserDto(User data);

    UserBriefDto userToUserBriefDto(User data);

    User signupRequestDtoToUser(SignupRequestDto data);

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        final GrantedAuthority authority = new SimpleGrantedAuthority(data.getAuthority());
        log.trace("mapped granted authority {} to granted authority {}", data, authority);
        return authority;
    }

    default UserAttribute tripleToUserAttribute(String userId, String name, String value) {
        return UserAttribute.builder()
                .userId(userId)
                .name(name)
                .value(value)
                .build();
    }
}
