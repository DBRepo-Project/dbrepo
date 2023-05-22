package at.tuwien.mapper;

import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.entities.semantics.Ontology;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface OntologyMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OntologyMapper.class);

    OntologyDto ontologyToOntologyDto(Ontology data);

    OntologyBriefDto ontologyToOntologyBriefDto(Ontology data);

    Ontology ontologyCreateDtoToOntology(OntologyCreateDto data);

}
