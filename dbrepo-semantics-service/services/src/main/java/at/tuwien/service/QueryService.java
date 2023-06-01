package at.tuwien.service;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;

import java.util.List;

public interface QueryService {

    List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException;

    List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException;

    List<EntityDto> findByUri(Ontology ontology, String uri) throws QueryMalformedException;
}
