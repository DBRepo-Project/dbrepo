package at.tuwien.mapper;

import at.tuwien.api.container.image.ImageDto;
import at.tuwien.entities.container.image.ContainerImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageMapper.class);

    /* keep */
    ImageDto containerImageToImageDto(ContainerImage data);

}
