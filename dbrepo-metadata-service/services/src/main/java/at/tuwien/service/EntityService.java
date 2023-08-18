package at.tuwien.service;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.*;

import java.util.List;

public interface EntityService {

    List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException;

    List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException;

    List<EntityDto> findByUri(Ontology ontology, String uri) throws QueryMalformedException;

    EntityDto findOneByUri(Ontology ontology, String uri) throws QueryMalformedException,
            SemanticEntityNotFoundException;

    List<EntityDto> suggestTableSemantics(Long databaseId, Long tableId) throws TableNotFoundException,
            QueryMalformedException, DatabaseNotFoundException;

    List<TableColumnEntityDto> suggestTableColumnSemantics(Long databaseId, Long tableId, Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException;
}
