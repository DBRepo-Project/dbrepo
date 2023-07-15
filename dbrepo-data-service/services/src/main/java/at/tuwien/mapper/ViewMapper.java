package at.tuwien.mapper;

import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.View;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {DatabaseMapper.class, UserMapper.class})
public interface ViewMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewMapper.class);

    View viewDtoToView(ViewDto data);

}
