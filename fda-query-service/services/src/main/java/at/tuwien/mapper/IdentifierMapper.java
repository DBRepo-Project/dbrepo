package at.tuwien.mapper;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.GrantedAuthorityDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import org.mapstruct.Mapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Mapper(componentModel = "spring")
public interface IdentifierMapper {

    Identifier identifierDtoToIdentifier(IdentifierDto data);

    IdentifierDto identifierToIdentifierDto(Identifier data);
}
