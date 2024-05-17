package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.exception.ConceptNotFoundException;

import java.util.List;

public interface ConceptService {

    /**
     * Finds all table column concepts in the metadata database.
     *
     * @return The list of table column concepts.
     */
    List<TableColumnConcept> findAll();

    /**
     * Finds a table column concept by given uri in the metadata database.
     *
     * @param uri The uri.
     * @return The table column concept, if successful.
     * @throws ConceptNotFoundException The concept was not found.
     */
    TableColumnConcept find(String uri) throws ConceptNotFoundException;
}
