package at.tuwien.mapper;

import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Mapper(componentModel = "spring")
public interface MetadataMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetadataMapper.class);

    default String instantToDatestamp(Instant data) {
        final String datestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.systemDefault())
                .format(data);
        log.trace("mapped instant {} to string {}", data, datestamp);
        return datestamp;
    }

}
