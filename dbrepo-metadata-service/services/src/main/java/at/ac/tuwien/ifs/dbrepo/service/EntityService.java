package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.semantics.EntityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.semantics.TableColumnEntityDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.semantics.Ontology;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.OntologyNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SemanticEntityNotFoundException;

import java.util.List;

public interface EntityService {

    /**
     * Finds entities in the ontology whose label match the given label.
     *
     * @param ontology The ontology.
     * @param label    The label.
     * @return List of entities.
     * @throws MalformedException  The query is malformed.
     */
    List<EntityDto> findByLabel(Ontology ontology, String label) throws MalformedException;

    /**
     * Finds entities in the ontology whose label match the given label with maximum number of entities.
     *
     * @param ontology The ontology.
     * @param label    The label.
     * @param limit    The maximum number of entities to return.
     * @return List of entities.
     * @throws MalformedException  The query is malformed.
     */
    List<EntityDto> findByLabel(Ontology ontology, String label, Integer limit) throws MalformedException;

    /**
     * Finds entities in the ontology whose uri match the given uri.
     *
     * @param uri      The uri.
     * @return List of entities.
     * @throws MalformedException         The query is malformed.
     * @throws OntologyNotFoundException  The ontology was not found in the metadata database.
     */
    List<EntityDto> findByUri(String uri) throws MalformedException, OntologyNotFoundException;

    /**
     * Finds an entity in the ontology whose uri match the given uri.
     *
     * @param uri      The uri.
     * @return The entity, if successful.
     * @throws MalformedException               The query is malformed.
     * @throws SemanticEntityNotFoundException  The semantic entity was not found in the metadata database.
     * @throws OntologyNotFoundException        The ontology was not found in the metadata database.
     */
    EntityDto findOneByUri(String uri) throws MalformedException, SemanticEntityNotFoundException,
            OntologyNotFoundException;

    /**
     * Attempts to suggest table semantics for a table with given id in database with given id.
     *
     * @param table    The table.
     * @return The list of entities that were suggested.
     * @throws MalformedException  The query is malformed.
     */
    List<EntityDto> suggestByTable(Table table) throws MalformedException;

    /**
     * Attempts to suggest table column semantics for a table column in table with given id in database with given id.
     *
     * @param column   The table column.
     * @return The list of entities that were suggested.
     * @throws MalformedException  The query is malformed.
     */
    List<TableColumnEntityDto> suggestByColumn(TableColumn column) throws MalformedException;
}
