package at.tuwien.mapper;

import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.entities.container.image.ContainerImage;
import org.mapstruct.Mapper;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageMapper.class);

    ContainerImage createImageDtoToContainerImage(ImageCreateDto data);

    ImageBriefDto containerImageToImageBriefDto(ContainerImage data);

    ImageDto containerImageToImageDto(ContainerImage data);

    default Instant dateToInstant(String date) {
        return Instant.parse(date);
    }

}
