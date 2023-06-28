package at.tuwien.service.impl;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.SemanticEntityNotFoundException;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.gateway.SemanticServiceGateway;
import at.tuwien.mapper.TableMapper;
import at.tuwien.repository.mdb.ConceptRepository;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.repository.mdb.UnitRepository;
import at.tuwien.repository.sdb.ConceptIdxRepository;
import at.tuwien.repository.sdb.UnitIdxRepository;
import at.tuwien.service.SemanticService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class SemanticServiceImpl implements SemanticService {

    private final TableMapper tableMapper;
    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;
    private final UnitIdxRepository unitIdxRepository;
    private final OntologyRepository ontologyRepository;
    private final ConceptIdxRepository conceptIdxRepository;
    private final SemanticServiceGateway semanticServiceGateway;

    @Autowired
    public SemanticServiceImpl(TableMapper tableMapper, UnitRepository unitRepository,
                               ConceptRepository conceptRepository, UnitIdxRepository unitIdxRepository,
                               OntologyRepository ontologyRepository, ConceptIdxRepository conceptIdxRepository,
                               SemanticServiceGateway semanticServiceGateway) {
        this.tableMapper = tableMapper;
        this.unitRepository = unitRepository;
        this.conceptRepository = conceptRepository;
        this.unitIdxRepository = unitIdxRepository;
        this.ontologyRepository = ontologyRepository;
        this.conceptIdxRepository = conceptIdxRepository;
        this.semanticServiceGateway = semanticServiceGateway;
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
            throw new UnitNotFoundException("Failed to find unit");
        }
        return optional.get();
    }

    @Override
    public TableColumnConcept saveConcept(String uri, String authorization) throws SemanticEntityNotFoundException {
        /* check compatible ontologies */
        final Ontology ontology = getCompatibleOntology(uri);
        if (ontology == null) {
            return TableColumnConcept.builder()
                    .uri(uri)
                    .build();
        }
        final TableColumnConcept concept = tableMapper.entityDtoToTableColumnConcept(semanticServiceGateway.getEntity(ontology.getId(), uri, authorization));
        log.info("Saved concept with uri {} in metadata database", concept.getUri());
        conceptIdxRepository.save(concept);
        log.info("Saved concept with uri {} in search database", concept.getUri());
        return concept;
    }

    @Override
    public TableColumnUnit saveUnit(String uri, String authorization) throws SemanticEntityNotFoundException {
        final Ontology ontology = getCompatibleOntology(uri);
        if (ontology == null) {
            return TableColumnUnit.builder()
                    .uri(uri)
                    .build();
        }
        final TableColumnUnit unit = tableMapper.entityDtoToTableColumnUnit(semanticServiceGateway.getEntity(ontology.getId(), uri, authorization));
        log.info("Saved unit with uri {} in metadata database", unit.getUri());
        unitIdxRepository.save(unit);
        log.info("Saved unit with uri {} in search database", unit.getUri());
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
