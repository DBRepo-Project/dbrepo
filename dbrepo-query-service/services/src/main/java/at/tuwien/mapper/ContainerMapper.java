package at.tuwien.mapper;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContainerMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContainerMapper.class);

    /* keep */
    ContainerDto containerToContainerDto(Container data);

}
