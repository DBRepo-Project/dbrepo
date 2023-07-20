package at.tuwien.service.impl;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.repository.mdb.ConceptRepository;
import at.tuwien.service.ConceptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConceptServiceImpl implements ConceptService {

    private final ConceptRepository conceptRepository;

    @Autowired
    public ConceptServiceImpl(ConceptRepository conceptRepository) {
        this.conceptRepository = conceptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableColumnConcept> findAll() {
        return conceptRepository.findAll();
    }
}
