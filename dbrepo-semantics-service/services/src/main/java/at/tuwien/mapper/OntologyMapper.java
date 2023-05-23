package at.tuwien.mapper;

import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.api.semantics.OntologyBriefDto;
import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.semantics.Ontology;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface OntologyMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OntologyMapper.class);

    OntologyDto ontologyToOntologyDto(Ontology data);

    OntologyBriefDto ontologyToOntologyBriefDto(Ontology data);

    Ontology ontologyCreateDtoToOntology(OntologyCreateDto data);

    ConceptDto tableColumnConceptToConceptDto(TableColumnConcept data);

    UnitDto tableColumnUnitToUnitDto(TableColumnUnit data);

    TableColumnUnit unitSaveDtoToTableColumnUnit(UnitSaveDto data);

    TableColumnConcept conceptSaveDtoToTableColumnConcept(ConceptSaveDto data);

}
