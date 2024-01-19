package at.tuwien.service;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.*;

import java.util.List;

public interface EntityService {

    /**
     * Finds entities in the ontology whose label match the given label.
     *
     * @param ontology The ontology.
     * @param label    The label.
     * @return The list of entities that match.
     * @throws QueryMalformedException  The SPARQL query is malformed.
     * @throws OntologyInvalidException The given ontology is invalid.
     */
    List<EntityDto> findByLabel(Ontology ontology, String label) throws QueryMalformedException, OntologyInvalidException;

    /**
     * Finds entities in the ontology whose label match the given label with maximum number of entities.
     *
     * @param ontology The ontology.
     * @param label    The label.
     * @param limit    The maximum number of entities to return.
     * @return The list of entities that match.
     * @throws QueryMalformedException  The SPARQL query is malformed.
     * @throws OntologyInvalidException The given ontology is invalid.
     */
    List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws QueryMalformedException, OntologyInvalidException;

    /**
     * Finds entities in the ontology whose uri match the given uri.
     *
     * @param ontology The ontology.
     * @param uri      The uri.
     * @return The list of entities that match.
     * @throws QueryMalformedException  The SPARQL query is malformed.
     * @throws OntologyInvalidException The given ontology is invalid.
     */
    List<EntityDto> findByUri(Ontology ontology, String uri) throws QueryMalformedException, OntologyInvalidException;

    /**
     * Finds an entity in the ontology whose uri match the given uri.
     *
     * @param ontology The ontology.
     * @param uri      The uri.
     * @return The entity, if successful.
     * @throws QueryMalformedException         The SPARQL query is malformed.
     * @throws OntologyInvalidException        The given ontology is invalid.
     * @throws SemanticEntityNotFoundException The entity was not found.
     */
    EntityDto findOneByUri(Ontology ontology, String uri) throws QueryMalformedException,
            SemanticEntityNotFoundException, OntologyInvalidException;

    /**
     * Attempts to suggest table semantics for a table with given id in database with given id.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @return The list of entities that were suggested.
     * @throws TableNotFoundException    The table with id was not found in the metadata database.
     * @throws QueryMalformedException   The SPARQL query is malformed.
     * @throws DatabaseNotFoundException The database with id was not found in the metadata database.
     * @throws OntologyInvalidException  The given ontology is invalid.
     */
    List<EntityDto> suggestTableSemantics(Long databaseId, Long tableId) throws TableNotFoundException,
            QueryMalformedException, DatabaseNotFoundException, OntologyInvalidException;

    /**
     * Attempts to suggest table column semantics for a table column in table with given id in database with given id.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param columnId   The table column id.
     * @return The list of entities that were suggested.
     * @throws TableNotFoundException       The table with id was not found in the metadata database.
     * @throws QueryMalformedException      The SPARQL query is malformed.
     * @throws DatabaseNotFoundException    The database with id was not found in the metadata database.
     * @throws OntologyInvalidException     The given ontology is invalid.
     * @throws TableColumnNotFoundException The table column was not found.
     */
    List<TableColumnEntityDto> suggestTableColumnSemantics(Long databaseId, Long tableId, Long columnId)
            throws QueryMalformedException, TableColumnNotFoundException, TableNotFoundException,
            DatabaseNotFoundException, OntologyInvalidException;
}
