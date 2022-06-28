package at.tuwien.mapper;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.document.file.FileKeyDto;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface DocumentMapper {

    default FileKeyDto importDtoToFileKeyDto(ImportDto data) {
        return FileKeyDto.builder()
                .key(importDtoToFilename(data))
                .build();
    }

    default String importDtoToFilename(ImportDto data) {
        return data.getLocation().substring(data.getLocation().lastIndexOf("/") + 1);
    }

}
