package at.tuwien.mapper;

import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.user.User;
import org.mapstruct.Mapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDetailsDto userDtoToUserDetailsDto(UserDto data);

    UserDto userToUserDto(User data);

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        return new SimpleGrantedAuthority(data.getAuthority());
    }
}
