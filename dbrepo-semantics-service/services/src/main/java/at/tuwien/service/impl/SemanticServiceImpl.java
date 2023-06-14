package at.tuwien.service.impl;

import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.mdb.TableColumnConceptRepository;
import at.tuwien.repository.mdb.TableColumnUnitRepository;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.UnitIdxRepository;
import at.tuwien.service.SemanticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
public class SemanticServiceImpl implements SemanticService {

    private final OntologyMapper ontologyMapper;
    private final UnitIdxRepository unitIdxRepository;
    private final ConceptIdxRepository conceptIdxRepository;
    private final TableColumnUnitRepository tableColumnUnitRepository;
    private final TableColumnConceptRepository tableColumnConceptRepository;

    @Autowired
    public SemanticServiceImpl(TableColumnConceptRepository tableColumnConceptRepository,
                               TableColumnUnitRepository tableColumnUnitRepository, OntologyMapper ontologyMapper,
                               UnitIdxRepository unitIdxRepository, ConceptIdxRepository conceptIdxRepository) {
        this.ontologyMapper = ontologyMapper;
        this.unitIdxRepository = unitIdxRepository;
        this.conceptIdxRepository = conceptIdxRepository;
        this.tableColumnUnitRepository = tableColumnUnitRepository;
        this.tableColumnConceptRepository = tableColumnConceptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConcept> findAllConcepts() {
        return tableColumnConceptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnUnit> findAllUnits() {
        return tableColumnUnitRepository.findAll();
    }

    @Override
    @Transactional
    public TableColumnConcept saveConcept(ConceptSaveDto data) {
        final TableColumnConcept entity = ontologyMapper.conceptSaveDtoToTableColumnConcept(data);
        final TableColumnConcept concept = tableColumnConceptRepository.save(entity);
        log.info("Saved concept with uri {} in metadata database", concept.getUri());
        conceptIdxRepository.save(concept);
        log.info("Saved concept with uri {} in open search database", concept.getUri());
        return concept;
    }

    @Override
    @Transactional
    public TableColumnUnit saveUnit(UnitSaveDto data) {
        final TableColumnUnit entity = ontologyMapper.unitSaveDtoToTableColumnUnit(data);
        final TableColumnUnit unit = tableColumnUnitRepository.save(entity);
        log.info("Saved unit with uri {} in metadata database", unit.getUri());
        unitIdxRepository.save(unit);
        log.info("Saved unit with uri {} in open search database", unit.getUri());
        return unit;
    }

}
