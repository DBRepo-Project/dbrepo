package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnConcept;
import at.ac.tuwien.ifs.dbrepo.core.exception.ConceptNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.ConceptRepository;
import at.ac.tuwien.ifs.dbrepo.service.ConceptService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ConceptServiceImpl implements ConceptService {

    private final ConceptRepository conceptRepository;

    @Autowired
    public ConceptServiceImpl(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional
    public TableColumnConcept create(TableColumnConcept concept) {
        return conceptRepository.save(concept);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConcept> findAll() {
        return conceptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public TableColumnConcept find(String uri) throws ConceptNotFoundException {
        final Optional<TableColumnConcept> optional = conceptRepository.findByUri(uri);
        if (optional.isEmpty()) {
            log.error("Failed to find concept with uri {} in metadata database", uri);
            throw new ConceptNotFoundException("Failed to find concept in metadata database");
        }
        return optional.get();
    }

}
