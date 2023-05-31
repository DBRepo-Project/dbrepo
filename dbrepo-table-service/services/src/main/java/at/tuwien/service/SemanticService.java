package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.SemanticEntityNotFoundException;
import at.tuwien.exception.UnitNotFoundException;

public interface SemanticService {

    /**
     * Finds a ColumnConcept with given uri
     *
     * @param uri The uri.
     * @return The concept, if successful.
     * @throws ConceptNotFoundException The ColumnConcept was not found in the metadata database.
     */
    TableColumnConcept findConcept(String uri) throws ConceptNotFoundException;

    /**
     * Finds a unit with given uri
     *
     * @param uri The uri.
     * @return The unit, if successful.
     * @throws UnitNotFoundException The unit was not found in the metadata database.
     */
    TableColumnUnit findUnit(String uri) throws UnitNotFoundException;

    TableColumnConcept saveConcept(String uri, String authorization) throws SemanticEntityNotFoundException;

    TableColumnUnit saveUnit(String uri, String authorization) throws SemanticEntityNotFoundException;
}
