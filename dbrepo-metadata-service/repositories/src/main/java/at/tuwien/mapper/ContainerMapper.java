package at.tuwien.mapper;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerCreateDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Mapper(componentModel = "spring", uses = {ImageMapper.class, DatabaseMapper.class, UserMapper.class})
public interface ContainerMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContainerMapper.class);

    @Mappings({
            @Mapping(target = "internalName", source = "name", qualifiedByName = "internalNameMapping")
    })
    Container containerCreateRequestDtoToContainer(ContainerCreateDto data);

    ContainerDto containerToContainerDto(Container data);

    @Mappings({
            @Mapping(target = "id", source = "id")
    })
    ContainerBriefDto containerToDatabaseContainerBriefDto(Container data);

    // https://stackoverflow.com/questions/1657193/java-code-library-for-generating-slugs-for-use-in-pretty-urls#answer-1657250
    @Named("internalNameMapping")
    default String containerToInternalContainerName(String containerName) {
        final Pattern NONLATIN = Pattern.compile("[^\\w-]");
        final Pattern WHITESPACE = Pattern.compile("[\\s]");
        String nowhitespace = WHITESPACE.matcher(containerName).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        final String name = "dbrepo-userdb-" + slug.toLowerCase(Locale.ENGLISH);
        log.trace("mapped container name {} to internal name {}", containerName, name);
        return name;
    }
}
