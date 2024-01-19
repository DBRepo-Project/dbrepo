package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {TableMapper.class})
public interface SemanticMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SemanticMapper.class);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);
}
