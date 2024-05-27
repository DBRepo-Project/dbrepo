package at.tuwien.mapper;

import at.tuwien.api.database.*;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ContainerMapper.class, UserMapper.class, ImageMapper.class, UserMapper.class,
        TableMapper.class, IdentifierMapper.class, ViewMapper.class})
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

    /* keep */
    @Named("internalMapping")
    default String nameToInternalName(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(data).replaceAll("_");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        final String name = slug.toLowerCase(Locale.ENGLISH);
        log.debug("mapping name {} to internal name {}", data, name);
        return name;
    }

    /* keep */
    @Named("languageMapping")
    LanguageType languageTypeDtoToLanguageType(LanguageTypeDto data);

    @Named("engineMapping")
    default String containerImageToEngine(ContainerImage data) {
        return data.getName() + ":" + data.getVersion();
    }

    @Mappings({
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseDto databaseToDatabaseDto(Database data);

    @Mappings({
            @Mapping(target = "created", source = "created", dateFormat = "dd-MM-yyyy HH:mm"),
    })
    DatabaseBriefDto databaseToDatabaseBriefDto(Database data);

    AccessType accessTypeDtoToAccessType(AccessTypeDto data);

    AccessTypeDto accessTypeToAccessTypeDto(AccessType data);

    DatabaseAccessDto databaseAccessToDatabaseAccessDto(DatabaseAccess data);

}
