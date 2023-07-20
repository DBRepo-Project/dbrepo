package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnConcept;

import java.util.List;

public interface ConceptService {

    /**
     * Finds all column concepts in the metadata database.
     *
     * @return List of column concepts.
     */
    List<TableColumnConcept> findAll();
}
