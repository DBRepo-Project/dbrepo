package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.database.DatabaseAccess;
import org.apache.http.auth.BasicUserPrincipal;
import org.mapstruct.Mapper;

import java.security.Principal;

@Mapper(componentModel = "spring")
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

    default Principal userDetailsDtoToPrincipal(UserDetailsDto data) {
        return new BasicUserPrincipal(data.getUsername());
    }

    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

}
