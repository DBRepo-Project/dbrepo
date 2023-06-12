package at.tuwien.mapper;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface ContainerMapper {

    @Mappings({
            @Mapping(target = "id", source = "id"),
    })
    ContainerDto containerToContainerDto(Container data);

}
