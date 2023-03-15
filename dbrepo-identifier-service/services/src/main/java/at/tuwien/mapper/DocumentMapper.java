package at.tuwien.mapper;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DocumentMapper.class);

    Date instantToDate(Instant data);

}
