package at.tuwien.service;

import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
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
     */
    List<EntityDto> findByLabel(Ontology ontology, String label) throws MalformedException;

    /**
     * Finds entities in the ontology whose label match the given label with maximum number of entities.
     *
     * @param ontology The ontology.
     * @param label    The label.
     * @param limit    The maximum number of entities to return.
     * @return The list of entities that match.
     */
    List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws MalformedException;

    /**
     * Finds entities in the ontology whose uri match the given uri.
     *
     * @param uri      The uri.
     * @return The list of entities that match.
     */
    List<EntityDto> findByUri(String uri) throws MalformedException, OntologyNotFoundException;

    /**
     * Finds an entity in the ontology whose uri match the given uri.
     *
     * @param uri      The uri.
     * @return The entity, if successful.
     */
    EntityDto findOneByUri(String uri) throws MalformedException, SemanticEntityNotFoundException, OntologyNotFoundException;

    /**
     * Attempts to suggest table semantics for a table with given id in database with given id.
     *
     * @param table    The table.
     * @return The list of entities that were suggested.
     */
    List<EntityDto> suggestByTable(Table table) throws MalformedException;

    /**
     * Attempts to suggest table column semantics for a table column in table with given id in database with given id.
     *
     * @param column   The table column.
     * @return The list of entities that were suggested.
     */
    List<TableColumnEntityDto> suggestByColumn(TableColumn column) throws MalformedException;
}
