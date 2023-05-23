package at.tuwien.service;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.EntitySearchDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.QueryMalformedException;

import java.util.List;

public interface QueryService {

    List<EntityDto> find(Ontology ontology, EntitySearchDto query) throws QueryMalformedException;

    List<EntityDto> find(Ontology ontology, EntitySearchDto query, Integer limit) throws QueryMalformedException;
}
