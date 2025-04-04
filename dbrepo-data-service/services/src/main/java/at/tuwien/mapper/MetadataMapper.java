package at.tuwien.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import org.keycloak.representations.AccessTokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.UUID;

@Mapper(componentModel = "spring", imports = {DatabaseDto.class, ContainerDto.class, ImageDto.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    ContainerDto containerDtoToContainerDto(ContainerDto data);

    @Mappings({
            @Mapping(target = "id", source = "userId"),
            @Mapping(target = "username", source = "privilegedUsername"),
            @Mapping(target = "password", source = "privilegedPassword"),
    })
    UserDto createDatabaseDtoToPrivilegedUserDto(CreateDatabaseDto data);

    @Mappings({
            @Mapping(target = "username", source = "readonlyUsername"),
            @Mapping(target = "password", source = "readonlyPassword"),
    })
    UserDto createDatabaseDtoToReadonlyUserDto(CreateDatabaseDto data);

    DatabaseBriefDto databaseDtoToDatabaseBriefDto(DatabaseDto data);

    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ViewColumnDto columnDtoToViewColumnDto(ColumnDto data);

    TableDto tableDtoToTableDto(TableDto data);

    ViewDto viewDtoToViewDto(ViewDto data);

    ContainerDto ContainerDtoToContainerDto(ContainerDto data);

    UserDto userDtoToUserDto(UserDto data);

    @Mappings({
            @Mapping(target = "accessToken", source = "token")
    })
    TokenDto accessTokenResponseToTokenDto(AccessTokenResponse data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    IdentifierBriefDto identifierDtoToIdentifierBriefDto(IdentifierDto data);

    default String metricToUri(String baseUrl, UUID databaseId, UUID tableId, UUID subsetId, UUID viewId) {
        final StringBuilder uri = new StringBuilder(baseUrl)
                .append("/database/")
                .append(databaseId);
        if (tableId != null) {
            uri.append("/table/")
                    .append(tableId);
        } else if (subsetId != null) {
            uri.append("/subset/")
                    .append(subsetId);
        } else if (viewId != null) {
            uri.append("/view/")
                    .append(viewId);
        }
        log.trace("count uri: {}", uri);
        return uri.toString();
    }

}
