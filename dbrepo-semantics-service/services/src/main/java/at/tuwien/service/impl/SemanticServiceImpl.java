package at.tuwien.service.impl;

import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.jpa.TableColumnConceptRepository;
import at.tuwien.repository.jpa.TableColumnUnitRepository;
import at.tuwien.service.SemanticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SemanticServiceImpl implements SemanticService {

    private final TableColumnConceptRepository tableColumnConceptRepository;
    private final TableColumnUnitRepository tableColumnUnitRepository;
    private final OntologyMapper ontologyMapper;

    @Autowired
    public SemanticServiceImpl(TableColumnConceptRepository tableColumnConceptRepository,
                               TableColumnUnitRepository tableColumnUnitRepository, OntologyMapper ontologyMapper) {
        this.tableColumnConceptRepository = tableColumnConceptRepository;
        this.tableColumnUnitRepository = tableColumnUnitRepository;
        this.ontologyMapper = ontologyMapper;
    }

    @Override
    public TableColumnConcept saveConcept(ConceptSaveDto data) {
        final TableColumnConcept entity = ontologyMapper.conceptSaveDtoToTableColumnConcept(data);
        final TableColumnConcept concept = tableColumnConceptRepository.save(entity);
        log.info("Saved concept with uri {}", concept.getUri());
        return concept;
    }

    @Override
    public TableColumnUnit saveUnit(UnitSaveDto data) {
        final TableColumnUnit entity = ontologyMapper.unitSaveDtoToTableColumnUnit(data);
        final TableColumnUnit unit = tableColumnUnitRepository.save(entity);
        log.info("Saved unit with uri {}", unit.getUri());
        return unit;
    }

}
