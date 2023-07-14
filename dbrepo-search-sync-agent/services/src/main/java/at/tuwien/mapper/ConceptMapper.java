package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConceptMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConceptMapper.class);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

}
