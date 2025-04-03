package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnConcept;
import at.ac.tuwien.ifs.dbrepo.core.exception.ConceptNotFoundException;

import java.util.List;

public interface ConceptService {

    TableColumnConcept create(TableColumnConcept concept);

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
