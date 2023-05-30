package at.tuwien.service.impl;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.entities.user.User;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.OntologyMapper;
import at.tuwien.repository.jpa.OntologyRepository;
import at.tuwien.service.OntologyService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class OntologyServiceImpl implements OntologyService {

    private final UserService userService;
    private final OntologyMapper ontologyMapper;
    private final OntologyRepository ontologyRepository;

    @Autowired
    public OntologyServiceImpl(UserService userService, OntologyMapper ontologyMapper,
                               OntologyRepository ontologyRepository) {
        this.userService = userService;
        this.ontologyMapper = ontologyMapper;
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public List<Ontology> findAll() {
        return ontologyRepository.findAll();
    }

    @Override
    public Ontology find(Long id) throws OntologyNotFoundException {
        final Optional<Ontology> optional = ontologyRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find ontology with id {}", id);
            throw new OntologyNotFoundException("Failed to find ontology with id " + id);
        }
        return optional.get();
    }

    @Override
    public Ontology create(OntologyCreateDto data, Principal principal) throws UserNotFoundException {
        final User user = userService.findByUsername(principal.getName());
        final Ontology entity = ontologyMapper.ontologyCreateDtoToOntology(data);
        entity.setCreatedBy(user.getId());
        final Ontology ontology = ontologyRepository.save(entity);
        log.info("Created ontology with id {}", ontology.getId());
        return ontology;
    }

    @Override
    public Ontology update(Long id, OntologyModifyDto data) throws OntologyNotFoundException {
        final Ontology entity = find(id);
        entity.setPrefix(data.getPrefix());
        entity.setUri(data.getUri());
        entity.setSparqlEndpoint(data.getSparqlEndpoint());
        final Ontology ontology = ontologyRepository.save(entity);
        log.info("Update ontology with id {}", ontology.getId());
        return ontology;
    }

    @Override
    public void delete(Long id) throws OntologyNotFoundException {
        if (!ontologyRepository.existsById(id)) {
            log.error("Failed to delete ontology: ontology with id {} does not exist", id);
            throw new OntologyNotFoundException("Failed to delete ontology: ontology with id " + id + " does not exist");
        }
        ontologyRepository.deleteById(id);
        log.info("Deleted ontology with id {}", id);
    }
}
