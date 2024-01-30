package at.tuwien.mapper;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface S3Mapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(S3Mapper.class);

}
