package at.tuwien.service.impl;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.UnitNotFoundException;
import at.tuwien.repository.mdb.*;
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

    private final UnitRepository unitRepository;
    private final ConceptRepository conceptRepository;

    @Autowired
    public SemanticServiceImpl(UnitRepository unitRepository, ConceptRepository conceptRepository) {
        this.unitRepository = unitRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConcept> findAllConcepts() {
        return conceptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnUnit> findAllUnits() {
        return unitRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnUnit findUnit(String uri) throws UnitNotFoundException {
        final Optional<TableColumnUnit> optional = unitRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find unit with uri {} in metadata database", uri);
            throw new UnitNotFoundException("Failed to find unit with uri " + uri);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnConcept findConcept(String uri) throws ConceptNotFoundException {
        final Optional<TableColumnConcept> optional = conceptRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find concept with uri {} in metadata database", uri);
            throw new ConceptNotFoundException("Failed to find concept with uri " + uri);
        }
        return optional.get();
    }

}
