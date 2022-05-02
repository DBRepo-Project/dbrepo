package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.entities.database.table.columns.concepts.Concept;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.net.URI;

@Mapper(componentModel = "spring")
public interface ConceptMapper {

    /* keep */
    @Mappings({
            @Mapping(target = "uri", source = "uri", qualifiedByName = "uriMapping")
    })
    ConceptDto conceptToConceptDto(Concept data);

    /* keep */
    @Named("uriMapping")
    default String uriToString(URI data) {
        return data.toString();
    }

}
