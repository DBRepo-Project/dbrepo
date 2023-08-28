package at.tuwien.service.impl;

import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.SemanticEntityNotFoundException;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.UnitIdxRepository;
import at.tuwien.service.EntityService;
import at.tuwien.service.SemanticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class SemanticServiceImpl implements SemanticService {

    private final TableMapper tableMapper;
    private final OntologyMapper ontologyMapper;
    private final UnitIdxRepository unitIdxRepository;
    private final ConceptIdxRepository conceptIdxRepository;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final OntologyRepository ontologyRepository;
    private final TableColumnUnitRepository tableColumnUnitRepository;
    private final TableColumnConceptRepository tableColumnConceptRepository;
    private final EntityService entityService;

    @Autowired
    public SemanticServiceImpl(TableMapper tableMapper, UnitRepository unitRepository,
                               ConceptRepository conceptRepository, OntologyRepository ontologyRepository,
                               EntityService entityService, TableColumnConceptRepository tableColumnConceptRepository,
                               TableColumnUnitRepository tableColumnUnitRepository, OntologyMapper ontologyMapper,
                               UnitIdxRepository unitIdxRepository, ConceptIdxRepository conceptIdxRepository) {
        this.tableMapper = tableMapper;
        this.ontologyMapper = ontologyMapper;
        this.unitIdxRepository = unitIdxRepository;
        this.conceptIdxRepository = conceptIdxRepository;
        this.unitRepository = unitRepository;
        this.ontologyRepository = ontologyRepository;
        this.conceptRepository = conceptRepository;
        this.tableColumnUnitRepository = tableColumnUnitRepository;
        this.tableColumnConceptRepository = tableColumnConceptRepository;
        this.entityService = entityService;
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
        /* save in metadata database */
        final TableColumnConcept concept = tableColumnConceptRepository.save(entity);
        log.info("Saved concept with id {} in metadata database", concept.getId());
        /* save in open search database */
        conceptIdxRepository.save(ontologyMapper.tableColumnConceptToConceptDto(concept));
        log.info("Saved concept with id {} in open search database", concept.getId());
        return concept;
    }

    @Override
    @Transactional
    public TableColumnUnit saveUnit(UnitSaveDto data) {
        final TableColumnUnit entity = ontologyMapper.unitSaveDtoToTableColumnUnit(data);
        /* save in metadata database */
        final TableColumnUnit unit = tableColumnUnitRepository.save(entity);
        log.info("Saved unit with id {} in metadata database", unit.getId());
        /* save in open search database */
        unitIdxRepository.save(ontologyMapper.tableColumnUnitToUnitDto(unit));
        log.info("Saved unit with id {} in open search database", unit.getId());
        return unit;
    }

    @Override
    public TableColumnConcept findConcept(String uri) throws ConceptNotFoundException {
        final Optional<TableColumnConcept> optional = conceptRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find column concept with uri {}", uri);
            throw new ConceptNotFoundException("Failed to find concept with uri " + uri);
        }
        return optional.get();
    }

    @Override
    public TableColumnUnit findUnit(String uri) throws UnitNotFoundException {
        final Optional<TableColumnUnit> optional = unitRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find unit with uri {}", uri);
            throw new UnitNotFoundException("Failed to find unit with uri " + uri);
        }
        return optional.get();
    }

    @Override
    public TableColumnConcept saveConcept(String uri) throws QueryMalformedException, SemanticEntityNotFoundException {
        /* check compatible ontologies */
        final Ontology ontology = getCompatibleOntology(uri);
        if (ontology == null) {
            return TableColumnConcept.builder()
                    .uri(uri)
                    .build();
        }
        /* save in metadata database */
        final TableColumnConcept concept = tableMapper.entityDtoToTableColumnConcept(
                entityService.findOneByUri(ontology, uri));
        log.info("Saved concept with uri {} in metadata database", concept.getUri());
        /* save in open search database */
        conceptIdxRepository.save(tableMapper.tableColumnConceptToConceptDto(concept));
        log.info("Saved concept with uri {} in open search database", concept.getUri());
        return concept;
    }

    @Override
    public TableColumnUnit saveUnit(String uri) throws SemanticEntityNotFoundException, QueryMalformedException {
        final Ontology ontology = getCompatibleOntology(uri);
        if (ontology == null) {
            return TableColumnUnit.builder()
                    .uri(uri)
                    .build();
        }
        /* save in metadata database */
        final TableColumnUnit unit = tableMapper.entityDtoToTableColumnUnit(entityService.findOneByUri(ontology, uri));
        log.info("Saved unit with uri {} in metadata database", unit.getUri());
        /* save in open search database */
        unitIdxRepository.save(tableMapper.tableColumnUnitToUnitDto(unit));
        log.info("Saved unit with uri {} in open search database", unit.getUri());
        return unit;
    }

    private Ontology getCompatibleOntology(String uri) {
        final List<Ontology> ontologies = ontologyRepository.findAll()
                .stream()
                .filter(o -> uri.startsWith(o.getUri()))
                .toList();
        if (ontologies.size() != 1) {
            log.warn("Failed to find registered ontology for entity with uri {}", uri);
            return null;
        }
        final Ontology ontology = ontologies.get(0);
        log.debug("found available compatible ontology with id {}", ontology.getId());
        return ontology;
    }

}
