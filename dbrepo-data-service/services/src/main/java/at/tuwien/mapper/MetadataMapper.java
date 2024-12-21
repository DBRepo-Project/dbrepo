package at.tuwien.mapper;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", imports = {PrivilegedDatabaseDto.class, PrivilegedContainerDto.class, ImageDto.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    default String queryDtoToViewName(QueryDto subset) {
        return subset.getQueryHash();
    }

    PrivilegedContainerDto containerDtoToPrivilegedContainerDto(ContainerDto data);

    DatabaseDto privilegedDatabaseDtoToDatabaseDto(PrivilegedDatabaseDto data);

    DatabaseBriefDto privilegedDatabaseDtoToDatabaseBriefDto(PrivilegedDatabaseDto data);

    TableDto privilegedTableDtoToTableDto(PrivilegedTableDto data);

    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ViewColumnDto columnDtoToViewColumnDto(ColumnDto data);

    @Mappings({
            @Mapping(target = "database", expression = "java(PrivilegedDatabaseDto.builder().container(PrivilegedContainerDto.builder().image(new ImageDto()).build()).build())")
    })
    PrivilegedTableDto tableDtoToPrivilegedTableDto(TableDto data);

    PrivilegedViewDto viewDtoToPrivilegedViewDto(ViewDto data);

    ContainerDto privilegedContainerDtoToContainerDto(PrivilegedContainerDto data);

    PrivilegedUserDto userDtoToPrivilegedUserDto(UserDto data);

    UserBriefDto userDtoToUserBriefDto(UserDto data);

    @Mappings({
            @Mapping(target = "databaseId", source = "tdbid")
    })
    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    IdentifierBriefDto identifierDtoToIdentifierBriefDto(IdentifierDto data);

}
