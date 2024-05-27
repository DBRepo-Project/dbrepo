package at.tuwien.mapper;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", imports = {PrivilegedDatabaseDto.class, PrivilegedContainerDto.class, ImageDto.class})
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    PrivilegedContainerDto containerDtoToPrivilegedContainerDto(ContainerDto data);

    DatabaseDto privilegedDatabaseDtoToDatabaseDto(PrivilegedDatabaseDto data);

    TableDto privilegedTableDtoToTableDto(PrivilegedTableDto data);

    ColumnDto viewColumnDtoToColumnDto(ViewColumnDto data);

    ViewColumnDto columnDtoToViewColumnDto(ColumnDto data);

    /* keep */
    TableBriefDto tableDtoToTableBriefDto(TableDto data);

    @Mappings({
            @Mapping(target = "database", expression = "java(PrivilegedDatabaseDto.builder().container(PrivilegedContainerDto.builder().image(new ImageDto()).build()).build())")
    })
    PrivilegedTableDto tableDtoToPrivilegedTableDto(TableDto data);

    PrivilegedViewDto viewDtoToPrivilegedViewDto(ViewDto data);

    ContainerDto privilegedContainerDtoToContainerDto(PrivilegedContainerDto data);

}
