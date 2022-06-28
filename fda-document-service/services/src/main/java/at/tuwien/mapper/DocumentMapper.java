package at.tuwien.mapper;

import at.tuwien.api.document.file.FileKeyDto;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface DocumentMapper {

    default FileKeyDto stringToFileKeyDto(String data) {
        return FileKeyDto.builder()
                .key(data)
                .build();
    }

}
