package at.tuwien.service;

import at.tuwien.api.database.table.columns.concepts.ConceptSaveDto;
import at.tuwien.api.database.table.columns.concepts.UnitSaveDto;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.SemanticEntityNotFoundException;
import at.tuwien.exception.UnitNotFoundException;

import java.util.List;

public interface SemanticService {

    List<TableColumnConcept> findAllConcepts();

    List<TableColumnUnit> findAllUnits();

    TableColumnConcept saveConcept(ConceptSaveDto data);

    TableColumnUnit saveUnit(UnitSaveDto data);

    /**
     * Finds a ColumnConcept with given uri.
     *
     * @param uri The uri.
     * @return The concept, if successful.
     * @throws ConceptNotFoundException The ColumnConcept was not found in the metadata database.
     */
    TableColumnConcept findConcept(String uri) throws ConceptNotFoundException;

    /**
     * Finds a unit with given uri.
     *
     * @param uri The uri.
     * @return The unit, if successful.
     * @throws UnitNotFoundException The unit was not found in the metadata database.
     */
    TableColumnUnit findUnit(String uri) throws UnitNotFoundException;

    /**
     * Saves a concept with uri and authorization information for retrieving information from the semantics service.
     *
     * @param uri           The uri.
     * @param authorization The authorization information.
     * @return The saved column concept.
     * @throws SemanticEntityNotFoundException The semantic information could not be found.
     */
    TableColumnConcept saveConcept(String uri, String authorization) throws SemanticEntityNotFoundException;

    /**
     * Saves a unit with uri and authorization information for retrieving information from the semantics service.
     *
     * @param uri           The uri.
     * @param authorization The authorization information.
     * @return The saved column unit.
     * @throws SemanticEntityNotFoundException The semantic information could not be found.
     */
    TableColumnUnit saveUnit(String uri, String authorization) throws SemanticEntityNotFoundException;
}
