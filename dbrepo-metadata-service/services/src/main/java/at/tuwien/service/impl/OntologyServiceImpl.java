package at.tuwien.service.impl;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.api.semantics.OntologyModifyDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.mapper.SparqlMapper;
import at.tuwien.repository.OntologyRepository;
import at.tuwien.service.OntologyService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class OntologyServiceImpl implements OntologyService {

    private final SparqlMapper sparqlMapper;
    private final MetadataMapper metadataMapper;
    private final OntologyRepository ontologyRepository;

    @Autowired
    public OntologyServiceImpl(SparqlMapper ontologyMapper, MetadataMapper metadataMapper,
                               OntologyRepository ontologyRepository) {
        this.sparqlMapper = ontologyMapper;
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
