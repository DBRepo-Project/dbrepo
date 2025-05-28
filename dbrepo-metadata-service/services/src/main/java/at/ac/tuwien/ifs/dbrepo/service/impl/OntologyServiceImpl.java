package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.semantics.OntologyCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.OntologyModifyDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.semantics.Ontology;
import at.ac.tuwien.ifs.dbrepo.core.exception.OntologyNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.repository.OntologyRepository;
import at.ac.tuwien.ifs.dbrepo.service.OntologyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class OntologyServiceImpl implements OntologyService {

    private final MetadataMapper metadataMapper;
    private final OntologyRepository ontologyRepository;

    @Autowired
    public OntologyServiceImpl(MetadataMapper metadataMapper,
                               OntologyRepository ontologyRepository) {
        this.metadataMapper = metadataMapper;
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
    public Ontology find(UUID id) throws OntologyNotFoundException {
        final Optional<Ontology> optional = ontologyRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find ontology with id {}", id);
            throw new OntologyNotFoundException("Failed to find ontology with id " + id);
        }
        return optional.get();
    }

    @Override
    public Ontology find(String entityUri) throws OntologyNotFoundException {
        final String pattern;
        try {
            final URI uri = new URI(entityUri);
            pattern = uri.getScheme() + "://" + uri.getHost() + "%";
        } catch (URISyntaxException e) {
            log.error("Failed to find ontology: URI pattern invalid: {}", e.getMessage());
            throw new OntologyNotFoundException("Failed to find ontology: URI pattern invalid", e);
        }
        final Optional<Ontology> optional = ontologyRepository.findByUriPattern(pattern);
        if (optional.isEmpty()) {
            log.error("Failed to find ontology with URI pattern: {}", pattern);
            throw new OntologyNotFoundException("Failed to find ontology");
        }
        return optional.get();
    }

    @Override
    public Ontology create(OntologyCreateDto data, Principal principal) {
        /* delete in metadata database */
        final Ontology entity = metadataMapper.ontologyCreateDtoToOntology(data);
        final Ontology ontology = ontologyRepository.save(entity);
        log.info("Created ontology with id {} ", ontology.getId());
        return ontology;
    }

    @Override
    public Ontology update(Ontology ontology, OntologyModifyDto data) {
        ontology.setPrefix(data.getPrefix());
        ontology.setUri(data.getUri());
        ontology.setSparqlEndpoint(data.getSparqlEndpoint());
        ontology.setRdfPath(data.getRdfPath());
        /* delete in metadata database */
        ontology = ontologyRepository.save(ontology);
        log.info("Update ontology with id {}", ontology.getId());
        return ontology;
    }

    @Override
    public void delete(Ontology ontology) {
        /* delete in metadata database */
        ontologyRepository.deleteById(ontology.getId());
        log.info("Deleted ontology with id {}", ontology.getId());
    }
}
