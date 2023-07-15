package at.tuwien.mapper;

import at.tuwien.api.container.image.ImageDateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageDate;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {DatabaseMapper.class, UserMapper.class})
public interface ViewMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewMapper.class);

    View viewDtoToView(ViewDto data);

    @Mappings({
            @Mapping(target = "constraints", ignore = true),
            @Mapping(target = "tdbid", source = "databaseId")
    })
    Table tableDtoToTable(TableDto data);

    @Mappings({
            @Mapping(target = "cdbid", source = "databaseId"),
            @Mapping(target = "tid", source = "tableId"),
            @Mapping(target = "dateFormat", ignore = true)
    })
    TableColumn columnDtoToTableColumn(ColumnDto data);

}
