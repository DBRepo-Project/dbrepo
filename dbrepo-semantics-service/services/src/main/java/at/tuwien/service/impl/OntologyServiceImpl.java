package at.tuwien.service.impl;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.jpa.OntologyRepository;
import at.tuwien.service.OntologyService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class OntologyServiceImpl implements OntologyService {

    private final OntologyMapper ontologyMapper;
    private final OntologyRepository ontologyRepository;

    @Autowired
    public OntologyServiceImpl(OntologyMapper ontologyMapper, OntologyRepository ontologyRepository) {
        this.ontologyMapper = ontologyMapper;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public List<Ontology> findAll() {
        return ontologyRepository.findAll();
    }

    @Override
    public Ontology create(OntologyCreateDto data) {
        final Ontology ontology = ontologyRepository.save(ontologyMapper.ontologyCreateDtoToOntology(data));
        log.info("Created ontology with id {}", ontology.getId());
        return ontology;
    }
}
