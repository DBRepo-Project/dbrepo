package at.tuwien.mapper;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.UUID;

@Mapper(componentModel = "spring", imports = {DatabaseDto.class, ContainerDto.class, ImageDto.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    default String queryDtoToViewName(QueryDto subset) {
        return subset.getQueryHash();
    }

    ContainerDto containerDtoToContainerDto(ContainerDto data);

    DatabaseBriefDto databaseDtoToDatabaseBriefDto(DatabaseDto data);

    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ViewColumnDto columnDtoToViewColumnDto(ColumnDto data);

    TableDto tableDtoToTableDto(TableDto data);

    ViewDto viewDtoToViewDto(ViewDto data);

    ContainerDto ContainerDtoToContainerDto(ContainerDto data);

    UserDto userDtoToUserDto(UserDto data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    @Mappings({
            @Mapping(target = "databaseId", source = "tdbid")
    })
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
