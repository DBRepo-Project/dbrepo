package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.ConceptNotFoundException;
import at.tuwien.exception.UnitNotFoundException;

import java.util.List;

public interface SemanticService {

    List<TableColumnConcept> findAllConcepts();

    List<TableColumnUnit> findAllUnits();

    TableColumnUnit findUnit(String uri) throws UnitNotFoundException;

    TableColumnConcept findConcept(String uri) throws ConceptNotFoundException;
}
