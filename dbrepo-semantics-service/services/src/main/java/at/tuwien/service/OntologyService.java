package at.tuwien.service;

import at.tuwien.api.semantics.OntologyCreateDto;
import at.tuwien.entities.semantics.Ontology;

import java.util.List;

public interface OntologyService {
    List<Ontology> findAll();

    Ontology create(OntologyCreateDto data);
}
