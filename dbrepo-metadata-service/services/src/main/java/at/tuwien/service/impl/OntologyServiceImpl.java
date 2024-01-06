package at.tuwien.service.impl;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.KeycloakRemoteException;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.service.OntologyService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

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
    public List<Ontology> findAllProcessable() {
        return ontologyRepository.findAllProcessable();
    }

    @Override
    public Ontology find(Long id) throws OntologyNotFoundException {
        final Optional<Ontology> optional = ontologyRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find ontology with id {} in metadata database", id);
            throw new OntologyNotFoundException("Failed to find ontology with id " + id + " in metadata database");
        }
        return optional.get();
    }

    @Override
    public Ontology create(OntologyCreateDto data, Principal principal) throws UserNotFoundException,
            KeycloakRemoteException, AccessDeniedException {
        final Ontology entity = ontologyMapper.ontologyCreateDtoToOntology(data);
        final Ontology ontology = ontologyRepository.save(entity);
        log.info("Created ontology with id {}  in metadata database", ontology.getId());
        return ontology;
    }

    @Override
    public Ontology update(Long id, OntologyModifyDto data) throws OntologyNotFoundException {
        final Ontology entity = find(id);
        entity.setPrefix(data.getPrefix());
        entity.setUri(data.getUri());
        entity.setSparqlEndpoint(data.getSparqlEndpoint());
        entity.setRdfPath(data.getRdfPath());
        final Ontology ontology = ontologyRepository.save(entity);
        log.info("Update ontology with id {} in metadata database", ontology.getId());
        return ontology;
    }

    @Override
    public void delete(Long id) throws OntologyNotFoundException {
        if (!ontologyRepository.existsById(id)) {
            log.error("Failed to delete ontology with id {} in metadata database: does not exist", id);
            throw new OntologyNotFoundException("Failed to delete ontology with id " + id + " in metadata database: does not exist");
        }
        ontologyRepository.deleteById(id);
        log.info("Deleted ontology with id {}", id);
    }
}
