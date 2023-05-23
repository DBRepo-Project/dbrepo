package at.tuwien.service;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;

import java.util.List;

public interface OntologyService {
    List<Ontology> findAll();

    Ontology find(Long id) throws OntologyNotFoundException;

    Ontology create(OntologyCreateDto data);

    void delete(Long id);
}
