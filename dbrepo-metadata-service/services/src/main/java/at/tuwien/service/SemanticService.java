package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.UnitNotFoundException;

import java.util.List;

public interface SemanticService {

    /**
     * Finds all table column concepts in the metadata database.
     *
     * @return The list of table column concepts.
     */
    List<TableColumnConcept> findAllConcepts();

    /**
     * Finds all table column units in the metadata database.
     *
     * @return The list of table column units.
     */
    List<TableColumnUnit> findAllUnits();

    /**
     * Finds a table column unit by given uri in the metadata database.
     *
     * @param uri The uri.
     * @return The table column unit, if successful.
     * @throws UnitNotFoundException The unit was not found.
     */
    TableColumnUnit findUnit(String uri) throws UnitNotFoundException;

    /**
     * Finds a table column concept by given uri in the metadata database.
     *
     * @param uri The uri.
     * @return The table column concept, if successful.
     * @throws ConceptNotFoundException The concept was not found.
     */
    TableColumnConcept findConcept(String uri) throws ConceptNotFoundException;
}
