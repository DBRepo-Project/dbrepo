package at.tuwien.mapper;

import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /* keep */
    @Mappings({
            @Mapping(target = "id", expression = "java(data.getId().toString())")
    })
    UserDetailsDto userBriefDtoToUserDetailsDto(UserBriefDto data);

    default GrantedAuthority grantedAuthorityDtoToGrantedAuthority(GrantedAuthorityDto data) {
        return new SimpleGrantedAuthority(data.getAuthority());
    }
}
